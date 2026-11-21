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
import io.ktor.client.request.header
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

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _englishBooks = MutableStateFlow<List<Book>>(emptyList())
    val englishBooks: StateFlow<List<Book>> = _englishBooks.asStateFlow()

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

    private val _articles = MutableStateFlow<List<ScienceArticle>>(FALLBACK_ARTICLES)
    val articles: StateFlow<List<ScienceArticle>> = _articles.asStateFlow()

    private val _weeklyStars = MutableStateFlow<List<Book>>(emptyList())
    val weeklyStars: StateFlow<List<Book>> = _weeklyStars.asStateFlow()

    init {
        loadInitialData(isRefresh = false)
    }

    fun refresh() {
        if (_isRefreshing.value || _isLoading.value) return
        bookRepository.invalidateCache()
        loadInitialData(isRefresh = true)
    }

    private fun loadInitialData(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) _isRefreshing.value = true else _isLoading.value = true
            try {
                kotlinx.coroutines.coroutineScope {
                    val booksDeferred = async {
                        try {
                            val fetchedBooks = bookRepository.getBooks()
                            _books.value = fetchedBooks
                            val audiobookCatalog = fetchedBooks.filterNot {
                                it.category.trim().equals("Podcast", ignoreCase = true)
                            }
                            val prioritizedBooks = audiobookCatalog.sortedWith(
                                compareBy<Book> { if (it.id == "local_ci_book") 1 else 0 }
                                    .thenByDescending { it.viewCount }
                            )
                            _weeklyStars.value = prioritizedBooks.take(5)
                            val grouped = prioritizedBooks.groupBy { if (it.category.isBlank()) "Genel" else it.category }
                            _groupedBooks.value = grouped
                            _recommendedBooks.value = prioritizedBooks.take(10)
                            _englishBooks.value = audiobookCatalog.filter {
                                it.language.equals("en", ignoreCase = true) ||
                                    it.category.trim().contains("İngilizce", ignoreCase = true) ||
                                    it.category.trim().equals("English", ignoreCase = true)
                            }
                        } catch (e: Throwable) {
                            println("HomeVM Books Error: ${e.message}")
                        }
                    }

                    val podcastsDeferred = async {
                        try {
                            val fetchedPodcasts = bookRepository.getPodcasts()
                            _podcasts.value = fetchedPodcasts
                        } catch (e: Throwable) {
                            println("HomeVM Podcasts Error: ${e.message}")
                        }
                    }

                    val articlesDeferred = async {
                        try {
                            fetchArticles()
                        } catch (_: Throwable) { }
                    }

                    _dailySummary.value = bookRepository.getDailySummary()
                    _dailyQuote.value = bookRepository.getDailyQuote()
                        ?: Triple("Bir kitap okuyan her şeyi okuyabilir.", "Sabahattin Ali", "Kürk Mantolu Madonna")

                    awaitAll(booksDeferred, podcastsDeferred, articlesDeferred)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
                _isRefreshing.value = false
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
            if (articles.isNotEmpty()) {
                _articles.value = articles
            } else if (_articles.value.isEmpty()) {
                _articles.value = FALLBACK_ARTICLES
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) {
            if (_articles.value.isEmpty()) {
                _articles.value = FALLBACK_ARTICLES
            }
        }
        finally { articlesLoading.value = false }
    }

    private suspend fun parseRssFeed(urlString: String, sourceName: String): List<ScienceArticle> {
        val resultList = mutableListOf<ScienceArticle>()
        try {
            val responseText: String = httpClient.get(urlString) {
                header("User-Agent", RSS_BROWSER_USER_AGENT)
                header("Accept", "application/rss+xml, application/xml, text/xml;q=0.9, */*;q=0.8")
            }.bodyAsText()
            
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

    companion object {
        internal const val RSS_BROWSER_USER_AGENT =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"

        internal val FALLBACK_ARTICLES: List<ScienceArticle> = listOf(
            ScienceArticle(
                id = "https://sarkac.org/edebiyat-ve-hafiza/",
                title = "Sesli Okuma ve Zihinsel Anlatı: Beynimiz Hikâyeleri Nasıl Canlandırır?",
                description = "Nörobilim araştırmaları, sesli kitap dinlerken beynin görsel ve duygusal merkezlerinin okuma ile aynı yoğunlukta çalıştığını gösteriyor...",
                imageUrl = "https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?q=80&w=1973",
                source = "Sarkaç",
                link = "https://sarkac.org/"
            ),
            ScienceArticle(
                id = "https://dusunbil.com/felsefe-ve-edebiyat/",
                title = "Dostoyevski'den Camus'ye: Modern İnsanın İç Sesi ve Varoluş",
                description = "Klasik romanlarda vicdan, özgürlük ve yabancılaşma kavramlarının felsefi kökenlerine derin bir yolculuk...",
                imageUrl = "https://images.unsplash.com/photo-1507413245164-6160d8298b31?q=80&w=2070",
                source = "Düşünbil",
                link = "https://dusunbil.com/"
            ),
            ScienceArticle(
                id = "https://arkeofili.com/antik-kutuphaneler/",
                title = "İskenderiye'den Bergama'ya: Antik Dünyanın Bilgi Tapınakları",
                description = "Papirüs rulolarından parşömen kodekslere uzanan süreçte insanlığın yazılı ve sözlü mirası nasıl korundu?...",
                imageUrl = "https://images.unsplash.com/photo-1599733594230-6b823276abcc?q=80&w=2070",
                source = "Arkeofili",
                link = "https://arkeofili.com/"
            ),
            ScienceArticle(
                id = "https://kayiprihtim.com/dosya/klasik-edebiyat/",
                title = "Zamanıneskitemediği Klasikler Neden Her Çağda Yeniden Okunur?",
                description = "Sabahattin Ali, Stefan Zweig ve Arthur Conan Doyle eserlerinin kuşaklar boyunca okurlarda uyandırdığı ortak duygular...",
                imageUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=2072",
                source = "Kayıp Rıhtım",
                link = "https://kayiprihtim.com/"
            )
        )
    }
}
