package com.semtinazizi.kitapduy.ui.viewmodel

import com.semtinazizi.kitapduy.domain.repository.PostDetailRepository
import com.semtinazizi.kitapduy.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class PostDetailScreenModelTest {
    @Test fun failedSendKeepsDraftAndAllowsRetry() = runTest {
        val repo = FakePostRepository()
        val model = PostDetailScreenModel(repo, backgroundScope)
        model.load("1"); runCurrent()
        model.draft.value = "Yorum taslağı"
        repo.fail = true
        model.send(); runCurrent()
        assertEquals("Yorum taslağı", model.draft.value)
        assertTrue(model.comments.value.isEmpty())
        assertFalse(model.sending.value)
        repo.fail = false
        model.send(); runCurrent()
        assertEquals("", model.draft.value)
        assertEquals("Yorum taslağı", model.comments.value.single().text)
        assertNull(model.error.value)
    }
    @Test fun doubleTapCreatesOnlyOneComment() = runTest {
        val repo = FakePostRepository()
        val model = PostDetailScreenModel(repo, backgroundScope)
        model.load("1"); runCurrent()
        repo.gate = CompletableDeferred()
        model.draft.value = "Bir yorum"
        model.send(); model.send(); runCurrent()
        assertEquals(1, repo.addCalls)
        repo.gate!!.complete(Unit); runCurrent()
        assertEquals(1, model.comments.value.size)
        assertEquals(1, model.post.value?.commentCount)
    }
    @Test fun textTypedWhileSendingIsNotDiscarded() = runTest {
        val repo = FakePostRepository()
        val model = PostDetailScreenModel(repo, backgroundScope)
        model.load("1"); runCurrent()
        repo.gate = CompletableDeferred()
        model.draft.value = "İlk yorum"; model.send(); runCurrent()
        model.draft.value = "Yeni taslak"
        repo.gate!!.complete(Unit); runCurrent()
        assertEquals("Yeni taslak", model.draft.value)
    }
    @Test fun failedDeleteKeepsCommentAndSuccessfulDeleteRemovesIt() = runTest {
        val repo = FakePostRepository().apply { rows = listOf(Comment(id = "9", text = "Kalıcı")) }
        val model = PostDetailScreenModel(repo, backgroundScope)
        model.load("1"); runCurrent()
        repo.fail = true
        model.delete("9"); runCurrent()
        assertEquals(1, model.comments.value.size)
        repo.fail = false
        model.delete("9"); runCurrent()
        assertTrue(model.comments.value.isEmpty())
    }
    @Test fun editsAppearOnlyAfterServerSuccess() = runTest {
        val repo = FakePostRepository().apply { rows = listOf(Comment(id = "9", text = "Eski")) }
        val model = PostDetailScreenModel(repo, backgroundScope)
        model.load("1"); runCurrent()
        var success = false
        repo.fail = true
        model.edit("9", "Yeni") { success = true }; runCurrent()
        assertFalse(success); assertEquals("Eski", model.comments.value.single().text)
        repo.fail = false
        model.edit("9", "Yeni") { success = true }; runCurrent()
        assertTrue(success); assertEquals("Yeni", model.comments.value.single().text)
    }
    @Test fun missingPostDoesNotLeaveAnInfiniteSpinnerOrStaleContent() = runTest {
        val repo = FakePostRepository()
        val model = PostDetailScreenModel(repo, backgroundScope)
        model.load("1"); runCurrent()
        repo.exists = false
        model.load("1"); runCurrent()
        assertFalse(model.loading.value); assertNull(model.post.value); assertNotNull(model.error.value)
    }
}

private class FakePostRepository : PostDetailRepository {
    var fail = false
    var exists = true
    var addCalls = 0
    var rows = emptyList<Comment>()
    var gate: CompletableDeferred<Unit>? = null
    override suspend fun getPost(id: String) = if (exists) FeedItem(id, "author", "Yazar", "", "", "", "", "Gönderi", timestamp = 0, type = FeedType.QUOTE) else null
    override suspend fun getComments(postId: String) = rows
    override suspend fun addComment(postId: String, text: String): Comment {
        addCalls++; gate?.await(); check(!fail) { "Sunucu hatası" }
        return Comment(id = "new", text = text)
    }
    override suspend fun editComment(postId: String, commentId: String, text: String): Comment {
        check(!fail); return rows.single().copy(text = text)
    }
    override suspend fun deleteComment(postId: String, commentId: String) { check(!fail) }
    override suspend fun likeComment(postId: String, commentId: String) = rows.single()
}
