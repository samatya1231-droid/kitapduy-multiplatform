package com.semtinazizi.kitapduy.ui.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.semtinazizi.kitapduy.data.repository.FeedActionsRepository
import com.semtinazizi.kitapduy.domain.repository.FeedRepository
import com.semtinazizi.kitapduy.domain.repository.PostDetailRepository
import com.semtinazizi.kitapduy.model.Book
import com.semtinazizi.kitapduy.model.Comment
import com.semtinazizi.kitapduy.util.UrlSanitizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class BookDetailScreenModel(
    private val feed: FeedRepository,
    private val actions: FeedActionsRepository,
    private val postDetailRepository: PostDetailRepository? = null
) : ScreenModel {
    val viewCount = MutableStateFlow(0)
    val likeCount = MutableStateFlow(0)
    val isLiked = MutableStateFlow(false)
    val comments = MutableStateFlow<List<Comment>>(emptyList())
    val repliesByComment = MutableStateFlow<Map<String, List<Comment>>>(emptyMap())
    val loadingRepliesFor = MutableStateFlow<Set<String>>(emptySet())
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

    fun editBookComment(bookId: String, commentId: String, newText: String) {
        if (newText.isBlank()) return
        screenModelScope.launch {
            try {
                actions.editPostComment(commentId, newText)
                comments.value = actions.bookComments(bookId)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error.value = e.message ?: "Yorum düzenlenemedi." }
        }
    }

    fun deleteBookComment(bookId: String, commentId: String) {
        screenModelScope.launch {
            try {
                actions.deletePost(commentId)
                comments.value = actions.bookComments(bookId)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error.value = e.message ?: "Yorum silinemedi." }
        }
    }

    fun toggleBookCommentLike(bookId: String, commentId: String) {
        screenModelScope.launch {
            try {
                actions.toggleLike(commentId, 1)
                comments.value = actions.bookComments(bookId)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error.value = e.message ?: "Beğeni kaydedilemedi." }
        }
    }

    fun loadReplies(commentId: String) {
        val repo = postDetailRepository ?: return
        if (commentId in loadingRepliesFor.value) return
        screenModelScope.launch {
            loadingRepliesFor.value = loadingRepliesFor.value + commentId
            try {
                val list = repo.getComments(commentId)
                repliesByComment.value = repliesByComment.value + (commentId to list)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error.value = e.message ?: "Yanıtlar yüklenemedi." }
            finally { loadingRepliesFor.value = loadingRepliesFor.value - commentId }
        }
    }

    fun addReply(bookId: String, commentId: String, replyText: String) {
        val repo = postDetailRepository ?: return
        if (replyText.isBlank()) return
        screenModelScope.launch {
            try {
                repo.addComment(commentId, replyText)
                val list = repo.getComments(commentId)
                repliesByComment.value = repliesByComment.value + (commentId to list)
                comments.value = actions.bookComments(bookId)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error.value = e.message ?: "Yanıt gönderilemedi." }
        }
    }

    fun shareQuoteToFeed(
        book: Book,
        quoteText: String,
        positionMs: Long,
        userId: String,
        userName: String,
        userPhoto: String?,
        isPremium: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        val clean = quoteText.trim()
        if (clean.isBlank()) return
        if (userId.isBlank()) { error.value = "Alıntı paylaşmak için giriş yapın."; return }
        screenModelScope.launch {
            try {
                val formatted = if (clean.startsWith("\"") && clean.endsWith("\"")) clean else "\"$clean\""
                val ok = feed.createQuotePost(
                    userId = userId,
                    userName = userName.ifBlank { "Kullanıcı" },
                    userPhoto = userPhoto,
                    bookId = book.id,
                    bookName = book.title,
                    comment = formatted,
                    isPremium = isPremium,
                    imageUrl = UrlSanitizer.normalizeMediaUrl(book.coverUrl).ifBlank { null },
                    audioUrl = UrlSanitizer.normalizeMediaUrl(book.audioUrl).ifBlank { null },
                    startPos = positionMs.coerceAtLeast(0L)
                )
                check(ok) { "Alıntı akışta paylaşılamadı. Lütfen tekrar deneyin." }
                onSuccess()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error.value = e.message ?: "Alıntı paylaşılamadı." }
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
