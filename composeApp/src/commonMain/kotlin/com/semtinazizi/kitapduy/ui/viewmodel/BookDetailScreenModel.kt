package com.semtinazizi.kitapduy.ui.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.semtinazizi.kitapduy.model.Comment
import com.semtinazizi.kitapduy.model.Book
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class BookDetailScreenModel : ScreenModel {

    private val _viewCount = MutableStateFlow(0)
    val viewCount: StateFlow<Int> = _viewCount.asStateFlow()

    private val _likeCount = MutableStateFlow(0)
    val likeCount: StateFlow<Int> = _likeCount.asStateFlow()

    private val _isLiked = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLiked.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _activeListeners = MutableStateFlow(1) // Canlı dinleyenler (Mock)
    val activeListeners: StateFlow<Int> = _activeListeners.asStateFlow()

    fun loadBookData(bookId: String, userId: String?) {
        screenModelScope.launch {
            // TODO: Supabase'den gerçek beğeni/görüntülenme verisini çek
            // Şimdilik Dummy değerler atıyoruz.
            _viewCount.value = (150..5000).random()
            _likeCount.value = (10..500).random()
            _isLiked.value = false
            _activeListeners.value = (1..50).random()
            
            loadComments(bookId)
        }
    }

    private fun loadComments(bookId: String) {
        // TODO: Supabase'den yorumları çek. Mock data:
        _comments.value = listOf(
            Comment(
                id = "1",
                userId = "user1",
                userName = "Kitap Kurdu",
                userPhotoUrl = null,
                text = "Bu kitabı dinlerken adeta büyülendim. Seslendirme harika!",
                isPremium = true,
                timestamp = 1690000000000
            ),
            Comment(
                id = "2",
                userId = "user2",
                userName = "Edebiyat Sever",
                userPhotoUrl = null,
                text = "Yazarın kalemi çok güçlü. Kesinlikle tavsiye ederim.",
                isPremium = false,
                timestamp = 1690001000000
            )
        )
    }

    fun incrementViewCount(bookId: String) {
        screenModelScope.launch {
            _viewCount.value += 1
            // TODO: Supabase view count artırımı
        }
    }

    fun toggleLike(bookId: String, userId: String?) {
        if (userId == null) return
        screenModelScope.launch {
            _isLiked.value = !_isLiked.value
            if (_isLiked.value) {
                _likeCount.value += 1
            } else {
                _likeCount.value -= 1
            }
            // TODO: Supabase like toggle
        }
    }

    fun addComment(bookId: String, bookTitle: String, bookCoverUrl: String, text: String, userPhoto: String?, isPremium: Boolean, userId: String, userName: String) {
        screenModelScope.launch {
            val newComment = Comment(
                id = "temp_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}",
                userId = userId,
                userName = userName,
                userPhotoUrl = userPhoto,
                text = text,
                isPremium = isPremium,
                timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            )
            val currentList = _comments.value.toMutableList()
            currentList.add(0, newComment)
            _comments.value = currentList
            
            // TODO: Supabase'e ekle
        }
    }

    fun addToShelf(book: Book, shelfType: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        screenModelScope.launch {
            // TODO: Supabase Bookshelf ekleme işlemi
            onSuccess()
        }
    }
}
