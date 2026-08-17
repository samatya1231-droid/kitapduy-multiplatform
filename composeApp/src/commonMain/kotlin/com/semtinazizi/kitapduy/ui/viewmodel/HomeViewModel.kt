package com.semtinazizi.kitapduy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semtinazizi.kitapduy.domain.repository.BookRepository
import com.semtinazizi.kitapduy.model.Book
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val bookRepository: BookRepository
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

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Kitapları yükle
            val fetchedBooks = bookRepository.getBooks()
            _books.value = fetchedBooks
            
            // Podcastleri yükle
            val fetchedPodcasts = bookRepository.getPodcasts()
            _podcasts.value = fetchedPodcasts
            
            // Kategorilere göre grupla
            val grouped = fetchedBooks.groupBy { it.category }.filterKeys { it.isNotBlank() }
            _groupedBooks.value = grouped
            
            _recommendedBooks.value = fetchedBooks.shuffled().take(10)
            
            // Günlük içerikler
            _dailySummary.value = bookRepository.getDailySummary()
            
            val quote = bookRepository.getDailyQuote()
            if (quote != null) {
                _dailyQuote.value = quote
            } else {
                // Yedek alıntı
                _dailyQuote.value = Triple("Bir kitap okudum ve hayatım değişti.", "Orhan Pamuk", "Yeni Hayat")
            }

            _isLoading.value = false
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
}
