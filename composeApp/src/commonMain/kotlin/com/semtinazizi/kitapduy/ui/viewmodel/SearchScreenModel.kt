package com.semtinazizi.kitapduy.ui.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.semtinazizi.kitapduy.domain.repository.BookRepository
import com.semtinazizi.kitapduy.domain.repository.SocialRepository
import com.semtinazizi.kitapduy.model.Book
import com.semtinazizi.kitapduy.model.Podcast
import com.semtinazizi.kitapduy.model.UserProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SearchCategory {
    BOOKS, USERS
}

class SearchScreenModel(
    private val bookRepository: BookRepository,
    private val socialRepository: SocialRepository
) : ScreenModel {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(SearchCategory.BOOKS)
    val selectedCategory: StateFlow<SearchCategory> = _selectedCategory.asStateFlow()

    private val _bookResults = MutableStateFlow<List<Book>>(emptyList())
    val bookResults: StateFlow<List<Book>> = _bookResults.asStateFlow()
    
    private val _podcastResults = MutableStateFlow<List<Podcast>>(emptyList())
    val podcastResults: StateFlow<List<Podcast>> = _podcastResults.asStateFlow()

    private val _userResults = MutableStateFlow<List<UserProfile>>(emptyList())
    val userResults: StateFlow<List<UserProfile>> = _userResults.asStateFlow()
    
    // For Mixed Discovery Section
    private val _initialBooks = MutableStateFlow<List<Book>>(emptyList())
    val initialBooks: StateFlow<List<Book>> = _initialBooks.asStateFlow()
    
    private val _initialPodcasts = MutableStateFlow<List<Podcast>>(emptyList())
    val initialPodcasts: StateFlow<List<Podcast>> = _initialPodcasts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var isDiscoveryLoading = false
    private var isSearchLoading = false
    private var searchJob: Job? = null

    init {
        loadDiscoveryItems()
    }

    private fun updateCombinedLoading() {
        _isLoading.value = if (_searchQuery.value.length >= 2) {
            isSearchLoading
        } else {
            isDiscoveryLoading
        }
    }

    private fun loadDiscoveryItems() {
        screenModelScope.launch {
            isDiscoveryLoading = true
            updateCombinedLoading()
            try {
                _initialBooks.value = bookRepository.getBooks()
                _initialPodcasts.value = bookRepository.getPodcasts()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
            } finally {
                isDiscoveryLoading = false
                updateCombinedLoading()
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchDebounced(query)
    }

    fun setCategory(category: SearchCategory) {
        _selectedCategory.value = category
        searchDebounced(_searchQuery.value)
    }

    private fun searchDebounced(query: String) {
        searchJob?.cancel()
        if (query.length < 2) {
            isSearchLoading = false
            _bookResults.value = emptyList()
            _podcastResults.value = emptyList()
            _userResults.value = emptyList()
            updateCombinedLoading()
            return
        }

        searchJob = screenModelScope.launch {
            delay(500) // Debounce
            isSearchLoading = true
            updateCombinedLoading()
            try {
                if (_selectedCategory.value == SearchCategory.BOOKS) {
                    _bookResults.value = bookRepository.searchBooks(query)
                    _podcastResults.value = bookRepository.searchPodcasts(query)
                    _userResults.value = emptyList()
                } else {
                    _userResults.value = socialRepository.searchUsers(query)
                    _bookResults.value = emptyList()
                    _podcastResults.value = emptyList()
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
            } finally {
                isSearchLoading = false
                updateCombinedLoading()
            }
        }
    }
}
