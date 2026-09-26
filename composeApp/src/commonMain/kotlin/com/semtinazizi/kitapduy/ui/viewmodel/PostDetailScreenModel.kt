package com.semtinazizi.kitapduy.ui.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.semtinazizi.kitapduy.domain.repository.PostDetailRepository
import com.semtinazizi.kitapduy.model.Comment
import com.semtinazizi.kitapduy.model.FeedItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class PostDetailScreenModel(private val repository: PostDetailRepository, private val suppliedScope: CoroutineScope? = null) : ScreenModel {
    private val scope get() = suppliedScope ?: screenModelScope
    val post = MutableStateFlow<FeedItem?>(null)
    val comments = MutableStateFlow<List<Comment>>(emptyList())
    val loading = MutableStateFlow(false)
    val sending = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val draft = MutableStateFlow("")
    private var postId: String? = null
    private var loadJob: Job? = null
    private val busy = mutableSetOf<String>()

    fun load(id: String) {
        if (id != postId) { post.value = null; comments.value = emptyList(); draft.value = "" }
        postId = id
        loadJob?.cancel()
        loading.value = true
        error.value = null
        loadJob = scope.launch {
            try {
                val fetched = repository.getPost(id)
                if (fetched == null) {
                    post.value = null; comments.value = emptyList()
                    error("Gönderi bulunamadı veya silinmiş.")
                }
                val rows = repository.getComments(id)
                post.value = fetched.copy(commentCount = rows.size)
                comments.value = rows
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error.value = e.message ?: "Gönderi yüklenemedi." }
            finally { loading.value = false }
        }
    }
    fun send() {
        val id = postId ?: return
        val text = draft.value.trim()
        if (sending.value || text.isEmpty() || post.value == null) return
        if (text.length > 2000) { error.value = "Yorum en fazla 2000 karakter olabilir."; return }
        error.value = null
        sending.value = true
        scope.launch {
            try {
                val saved = repository.addComment(id, text)
                if (postId == id) {
                    comments.value = listOf(saved) + comments.value.filterNot { it.id == saved.id }
                    if (draft.value.trim() == text) draft.value = ""
                    updateCount()
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error.value = e.message ?: "Yorum gönderilemedi." }
            finally { sending.value = false }
        }
    }
    fun edit(commentId: String, text: String, onSuccess: () -> Unit) = mutate(commentId) { id ->
        val saved = repository.editComment(id, commentId, text)
        comments.value = comments.value.map { if (it.id == saved.id) saved else it }
        onSuccess()
    }
    fun delete(commentId: String) = mutate(commentId) { id ->
        repository.deleteComment(id, commentId)
        comments.value = comments.value.filterNot { it.id == commentId }
        updateCount()
    }
    fun like(commentId: String) = mutate(commentId) { id ->
        val saved = repository.likeComment(id, commentId)
        comments.value = comments.value.map { if (it.id == saved.id) saved else it }
    }
    private fun updateCount() { post.value = post.value?.copy(commentCount = comments.value.size) }
    private fun mutate(key: String, action: suspend (String) -> Unit) {
        val id = postId ?: return
        if (!busy.add(key)) return
        error.value = null
        scope.launch {
            try { action(id) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error.value = e.message ?: "İşlem tamamlanamadı." }
            finally { busy.remove(key) }
        }
    }
}
