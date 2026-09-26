package com.semtinazizi.kitapduy.ui.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.semtinazizi.kitapduy.data.repository.FeedActionsRepository
import com.semtinazizi.kitapduy.domain.repository.FeedRepository
import com.semtinazizi.kitapduy.model.Book
import com.semtinazizi.kitapduy.model.Comment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class BookDetailScreenModel(private val feed: FeedRepository, private val actions: FeedActionsRepository) : ScreenModel {
    val viewCount = MutableStateFlow(0)
    val likeCount = MutableStateFlow(0)
    val isLiked = MutableStateFlow(false)
    val comments = MutableStateFlow<List<Comment>>(emptyList())
    val activeListeners = MutableStateFlow<Int?>(null)
    val error = MutableStateFlow<String?>(null)
    val isSending = MutableStateFlow(false)

    fun loadBookData(book: Book, userId: String?) {
        viewCount.value = book.viewCount
        likeCount.value = book.likeCount
        screenModelScope.launch {
            try { comments.value = actions.bookComments(book.id) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error.value = "Yorumlar yüklenemedi. Lütfen tekrar deneyin." }
        }
    }

    fun toggleLike(bookId: String, userId: String?) {
        // Android stores book likes in Firestore. Do not fabricate Supabase records or counters.
        error.value = if (userId.isNullOrBlank()) "Lütfen giriş yapın."
            else "Kitap beğenileri şu anda kullanılamıyor."
    }

    fun addComment(bookId: String, bookTitle: String, bookCoverUrl: String, text: String,
                   userPhoto: String?, isPremium: Boolean, userId: String, userName: String,
                   onSuccess: () -> Unit = {}) {
        if (isSending.value || text.isBlank()) return
        if (userId.isBlank()) { error.value = "Lütfen giriş yapın."; return }
        isSending.value = true
        screenModelScope.launch {
            try {
                check(feed.createPost(userId, userName, userPhoto, bookId, bookTitle, text.trim(),
                    isPremium, bookCoverUrl, null)) { "Yorum gönderilemedi. Lütfen tekrar deneyin." }
                onSuccess()
                comments.value = actions.bookComments(bookId)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error.value = e.message ?: "Yorum gönderilemedi." }
            finally { isSending.value = false }
        }
    }

    fun addToShelf(book: Book, shelfType: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        screenModelScope.launch {
            try { actions.addToShelf(book, shelfType); onSuccess() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { onError(e.message ?: "Kitaplığa eklenemedi.") }
        }
    }
}
