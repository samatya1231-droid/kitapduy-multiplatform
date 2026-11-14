package com.semtinazizi.kitapduy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semtinazizi.kitapduy.domain.repository.BookRepository
import com.semtinazizi.kitapduy.model.Book
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.CancellationException

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import com.semtinazizi.kitapduy.model.ScienceArticle

class HomeViewModel(
    private val bookRepository: BookRepository,
    private val httpClient: HttpClient
) : ViewModel() {

    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = _books.asStateFlow()

    private val _groupedBooks = MutableStateFlow<Map<String, List<Book>>>(emptyMap())
    val groupedBooks: StateFlow<Map<String, List<Book>>> = _groupedBooks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Tümü")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _favoriteBookIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteBookIds: StateFlow<Set<String>> = _favoriteBookIds.asStateFlow()

    private val _dailySummary = MutableStateFlow<Pair<String, String>?>(null)
    val dailySummary: StateFlow<Pair<String, String>?> = _dailySummary.asStateFlow()

    private val _dailyQuote = MutableStateFlow<Triple<String, String, String>?>(null)
    val dailyQuote: StateFlow<Triple<String, String, String>?> = _dailyQuote.asStateFlow()

    private val _recommendedBooks = MutableStateFlow<List<Book>>(emptyList())
    val recommendedBooks: StateFlow<List<Book>> = _recommendedBooks.asStateFlow()

    private val _podcasts = MutableStateFlow<List<com.semtinazizi.kitapduy.model.Podcast>>(emptyList())
    val podcasts: StateFlow<List<com.semtinazizi.kitapduy.model.Podcast>> = _podcasts.asStateFlow()

    private val _isTrailerPlaying = MutableStateFlow(false)
    val isTrailerPlaying: StateFlow<Boolean> = _isTrailerPlaying.asStateFlow()

    private val _trailerBook = MutableStateFlow<Book?>(null)
    val trailerBook: StateFlow<Book?> = _trailerBook.asStateFlow()

    private val _showTrailerCTA = MutableStateFlow(false)
    val showTrailerCTA: StateFlow<Boolean> = _showTrailerCTA.asStateFlow()

    val articlesLoading = MutableStateFlow(false)
    val articlesError = MutableStateFlow<String?>(null)
    fun reloadArticles() { if (!articlesLoading.value) viewModelScope.launch { fetchArticles() } }

    private val _articles = MutableStateFlow<List<ScienceArticle>>(emptyList())
    val articles: StateFlow<List<ScienceArticle>> = _articles.asStateFlow()

    private val _weeklyStars = MutableStateFlow<List<Book>>(emptyList())
    val weeklyStars: StateFlow<List<Book>> = _weeklyStars.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Kitapları ve Podcastleri paralel yükleyelim
                launch {
                    try {
                        val fetchedBooks = bookRepository.getBooks()
                        _books.value = fetchedBooks
                        val prioritizedBooks = fetchedBooks.sortedWith(
                            compareBy<Book> { if (it.id == "local_ci_book") 1 else 0 }
                                .thenByDescending { it.viewCount }
                        )
                        _weeklyStars.value = prioritizedBooks.take(5)
                        val grouped = prioritizedBooks.groupBy { if (it.category.isBlank()) "Genel" else it.category }
                        _groupedBooks.value = grouped
                        _recommendedBooks.value = prioritizedBooks.take(10)
                    } catch (e: Throwable) { println("HomeVM Books Error: ${e.message}") }
                }
                
                launch {
                    try {
                        println("HomeViewModel: Starting podcast fetch...")
                        val fetchedPodcasts = bookRepository.getPodcasts()
                        println("HomeViewModel: Received ${fetchedPodcasts.size} podcasts from repository")
                        _podcasts.value = fetchedPodcasts
                    } catch (e: Throwable) { println("HomeVM Podcasts Error: ${e.message}") }
                }

                // Makaleler ve diğerleri
                fetchArticles()
                _dailySummary.value = bookRepository.getDailySummary()
                _dailyQuote.value = bookRepository.getDailyQuote() ?: Triple("Bir kitap okuyan her şeyi okuyabilir.", "Sabahattin Ali", "Kürk Mantolu Madonna")

            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                // Not: Paralel launch'lar bitene kadar beklemek gerekirse join() kullanılır. 
                // Şimdilik ana akış isLoading'i kapatsın.
                _isLoading.value = false
            }
        }
    }

    fun dismissTrailerCTA() {
        _showTrailerCTA.value = false
    }

    fun loadFavorites(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            _favoriteBookIds.value = bookRepository.getFavoriteBookIds(userId)
        }
    }

    fun filterByCategory(category: String) {
        _selectedCategory.value = category
    }

    private suspend fun fetchArticles() = kotlinx.coroutines.coroutineScope {
        articlesLoading.value = true
        articlesError.value = null
        try {
            val sources = listOf(
                "https://kayiprihtim.com/feed" to "Kayıp Rıhtım",
                "https://edebiyathaber.net/feed/" to "Edebiyat Haber",
                "https://www.kitaphaber.com.tr/feed" to "Kitap Haber",
                "https://kalemkahveklavye.com/feed" to "Kalem Kahve",
                "https://sanatkritik.com/feed" to "Sanat Kritik",
                "https://dusunbil.com/feed/" to "Düşünbil",
                "https://arkeofili.com/feed/" to "Arkeofili",
                "https://sarkac.org/feed/" to "Sarkaç",
                "https://khosann.com/feed/" to "Khosann")
            val articles = sources.map { (url, source) -> async { parseRssFeed(url, source) } }
                .awaitAll().flatten().distinctBy { it.link }
            if (articles.isNotEmpty()) _articles.value = articles
            else articlesError.value = "Makaleler yüklenemedi. Bağlantını kontrol edip tekrar dene."
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { articlesError.value = "Makaleler yüklenemedi. Tekrar dene." }
        finally { articlesLoading.value = false }
    }

    private suspend fun parseRssFeed(urlString: String, sourceName: String): List<ScienceArticle> {
        val resultList = mutableListOf<ScienceArticle>()
        try {
            val responseText: String = httpClient.get(urlString).bodyAsText()
            
            val itemRegex = "(?s)<item>(.*?)</item>".toRegex(RegexOption.IGNORE_CASE)
            val titleRegex = "(?s)<title>(.*?)</title>".toRegex(RegexOption.IGNORE_CASE)
            val linkRegex = "(?s)<link>(.*?)</link>".toRegex(RegexOption.IGNORE_CASE)
            val descriptionRegex = "(?s)<description><!\\[CDATA\\[(.*?)]]></description>".toRegex(RegexOption.IGNORE_CASE)
            val altDescRegex = "(?s)<description>(.*?)</description>".toRegex(RegexOption.IGNORE_CASE)
            val imgRegex = "<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>".toRegex(RegexOption.IGNORE_CASE)
            
            val items = itemRegex.findAll(responseText)
            for (itemMatch in items) {
                val itemContent = itemMatch.groupValues[1]
                val title = titleRegex.find(itemContent)?.groupValues?.get(1)?.replace("<![CDATA[", "")?.replace("]]>", "")?.trim() ?: continue
                val link = linkRegex.find(itemContent)?.groupValues?.get(1)?.trim() ?: continue
                
                var desc = descriptionRegex.find(itemContent)?.groupValues?.get(1) ?: altDescRegex.find(itemContent)?.groupValues?.get(1) ?: ""
                desc = desc.replace(Regex("<[^>]*>"), "")
                desc = desc.replace("<![CDATA[", "").replace("]]>", "")
                desc = desc.replace("&nbsp;", " ")
                
                var imageUrl = imgRegex.find(itemContent)?.groupValues?.get(1) ?: ""
                if (imageUrl.isBlank()) {
                    imageUrl = when (sourceName) {
                        "Arkeofili" -> "https://images.unsplash.com/photo-1599733594230-6b823276abcc?q=80&w=2070"
                        "Düşünbil" -> "https://images.unsplash.com/photo-1507413245164-6160d8298b31?q=80&w=2070"
                        "Sarkaç" -> "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=2072"
                        "Khosann" -> "https://images.unsplash.com/photo-1485827404703-89b55fcc595e?q=80&w=2070"
                        else -> "https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?q=80&w=1973"
                    }
                }
                
                resultList.add(
                    ScienceArticle(
                        id = link,
                        title = title,
                        description = desc.take(150) + "...",
                        imageUrl = imageUrl,
                        source = sourceName,
                        link = link
                    )
                )
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (e: Exception) {
            println("Parser error for $sourceName: ${e.message}")
        }
        return resultList
    }
}
