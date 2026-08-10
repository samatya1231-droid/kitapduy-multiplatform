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

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Kitapları yükle
            val fetchedBooks = bookRepository.getBooks()
            _books.value = fetchedBooks
            
            // Kategorilere göre grupla
            val grouped = fetchedBooks.groupBy { it.category }.filterKeys { it.isNotBlank() }
            _groupedBooks.value = grouped
            
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
