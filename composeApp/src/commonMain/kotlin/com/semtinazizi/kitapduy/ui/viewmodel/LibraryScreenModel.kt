package com.semtinazizi.kitapduy.ui.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.domain.repository.BookRepository
import com.semtinazizi.kitapduy.model.SupabaseBookshelf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LibraryScreenModel(
    private val bookRepository: BookRepository,
    private val authRepository: AuthRepository
) : ScreenModel {

    private val _bookshelfItems = MutableStateFlow<List<SupabaseBookshelf>>(emptyList())
    val bookshelfItems: StateFlow<List<SupabaseBookshelf>> = _bookshelfItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadBookshelf(shelfType: String) {
        val uid = authRepository.currentUserUid
        if (uid == null) {
            _errorMessage.value = "Kullanıcı girişi yapılmamış."
            return
        }

        screenModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                // Shelf type API beklediği formata çevrilebilir veya doğrudan yollanabilir
                // Örn: "FAVORITE", "LISTENING", "FINISHED", "DOWNLOADED"
                val items = bookRepository.getBookshelf(userId = uid, shelfType = shelfType)
                _bookshelfItems.value = items
            } catch (e: Throwable) {
                _errorMessage.value = e.message ?: "Bir hata oluştu."
                _bookshelfItems.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
