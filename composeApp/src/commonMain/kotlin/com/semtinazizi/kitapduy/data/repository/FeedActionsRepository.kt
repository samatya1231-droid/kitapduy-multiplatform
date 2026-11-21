package com.semtinazizi.kitapduy.data.repository

import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Uses the same posts and shelf records as the Android application. */
class FeedActionsRepository(
    private val client: SupabaseClient,
    private val auth: AuthRepository,
    private val httpClient: io.ktor.client.HttpClient? = null
) {
    private val mutationLock = Mutex()
    private fun uid(): String = auth.currentUserUid?.takeIf { it.isNotBlank() }
        ?: error("Lütfen giriş yapın.")

    suspend fun unreadNotificationCount(): Int {
        val userId = auth.currentUserUid?.takeIf { it.isNotBlank() } ?: return 0
        return CrossPlatformBridgeHelper.unreadNotificationCount(client, userId)
    }

    suspend fun toggleLike(postId: String, reaction: Int) {
        var notifyReceiver: String? = null
        var notifySenderName = "Bir okur"
        var notifySenderId = ""
        mutationLock.withLock {
            val userId = uid()
            notifySenderId = userId
            val post = client.postgrest["posts"].select { filter { eq("id", postId.toLong()) } }.decodeSingle<SupabasePost>()
            val likes = post.likedBy.orEmpty().split(',').filter { it.isNotBlank() }.toMutableList()
            val previous = likes.firstOrNull { it.substringBefore(':') == userId }
            likes.removeAll { it.substringBefore(':') == userId }
            val newlyLiked = previous == null
            if (previous == null || (previous != userId && previous != "$userId:$reaction")) likes.add("$userId:$reaction")
            client.postgrest["posts"].update({
                set("liked_by", likes.joinToString(",")); set("likes", likes.size)
            }) { filter { eq("id", postId.toLong()) } }
            if (newlyLiked && !post.userId.isNullOrBlank() && post.userId != userId) {
                notifyReceiver = post.userId
                notifySenderName = auth.getUserProfile(userId)?.name?.takeIf { it.isNotBlank() } ?: "Bir okur"
            }
        }
        notifyReceiver?.let { receiverId ->
            CrossPlatformBridgeHelper.sendSocialNotification(
                supabaseClient = client,
                httpClient = httpClient,
                receiverId = receiverId,
                senderId = notifySenderId,
                senderName = notifySenderName,
                type = "like",
                message = "$notifySenderName gönderini beğendi! ❤️",
                targetId = postId
            )
        }
    }

    suspend fun vote(postId: String, option: Int) {
        var notifyReceiver: String? = null
        var notifySenderName = "Bir okur"
        var notifySenderId = ""
        mutationLock.withLock {
            val userId = uid()
            notifySenderId = userId
            val post = client.postgrest["posts"].select { filter { eq("id", postId.toLong()) } }.decodeSingle<SupabasePost>()
            require(option in post.pollOptions.orEmpty().indices) { "Geçersiz seçenek." }
            if (userId in post.pollVoters.orEmpty()) return@withLock
            val votes = post.pollVotes.orEmpty().toMutableMap()
            votes[option.toString()] = (votes[option.toString()] ?: 0) + 1
            client.postgrest["posts"].update({
                set("poll_votes", votes); set("poll_voters", post.pollVoters.orEmpty() + userId)
            }) { filter { eq("id", postId.toLong()) } }
            if (!post.userId.isNullOrBlank() && post.userId != userId) {
                notifyReceiver = post.userId
                notifySenderName = auth.getUserProfile(userId)?.name?.takeIf { it.isNotBlank() } ?: "Bir okur"
            }
        }
        notifyReceiver?.let { receiverId ->
            CrossPlatformBridgeHelper.sendSocialNotification(
                supabaseClient = client,
                httpClient = httpClient,
                receiverId = receiverId,
                senderId = notifySenderId,
                senderName = notifySenderName,
                type = "poll_vote",
                message = "$notifySenderName anketine oy verdi! 📊",
                targetId = postId
            )
        }
    }

    suspend fun deletePost(postId: String) {
        val userId = uid()
        // Server RLS remains authoritative; ordinary users can only delete their own rows.
        val admin = auth.getUserProfile(userId)?.badges?.contains("admin") == true
        val deleted = client.postgrest["posts"].delete { filter {
            eq("id", postId.toLong())
            if (!admin) eq("user_id", userId)
        }; select() }.decodeList<SupabasePost>()
        check(deleted.isNotEmpty()) { "Gönderi silinemedi; silinmiş veya başka bir kullanıcıya ait olabilir." }
    }

    suspend fun bookComments(bookId: String): List<Comment> = client.postgrest["posts"].select {
        filter { eq("book_id", bookId) }
        order("created_at", Order.DESCENDING)
    }.decodeList<SupabasePost>()
        .filter {
            it.audioUrl.isNullOrBlank() &&
                !it.comment.isNullOrBlank() &&
                it.comment?.contains("dinlemeye başladı", ignoreCase = true) != true
        }
        .map {
            val normalizedLikes = it.likedBy.orEmpty()
                .split(',')
                .map { token -> token.substringBefore(':').trim() }
                .filter { uid -> uid.isNotBlank() }
                .distinct()
            Comment(
                id = it.id.toString(),
                userId = it.userId.orEmpty(),
                userName = it.userName.orEmpty(),
                userPhotoUrl = resolveFeedUrl(it.userPhoto).ifBlank { null },
                text = it.comment.orEmpty(),
                isPremium = it.isPremium == true,
                timestamp = parseSupabaseTimestamp(it.createdAt),
                likedBy = normalizedLikes,
                replyCount = it.commentCount ?: 0
            )
        }

    suspend fun editPostComment(postId: String, text: String) {
        val userId = uid()
        val body = text.trim()
        require(body.isNotEmpty() && body.length <= 2000) { "Yorum 1–2000 karakter olmalı." }
        val admin = auth.getUserProfile(userId)?.badges?.contains("admin") == true
        val updated = client.postgrest["posts"].update({ set("comment", body) }) {
            filter {
                eq("id", postId.toLong())
                if (!admin) eq("user_id", userId)
            }
            select()
        }.decodeSingleOrNull<SupabasePost>()
        checkNotNull(updated) { "Yorum güncellenemedi; silinmiş veya başka bir kullanıcıya ait olabilir." }
    }

    suspend fun addToShelf(book: Book, shelfType: String) {
        val userId = uid()
        val existing = client.postgrest["user_bookshelves"].select { filter {
            eq("user_id", userId); eq("book_id", book.id); eq("shelf_type", shelfType)
        } }.decodeList<SupabaseBookshelf>()
        if (existing.isEmpty()) client.postgrest["user_bookshelves"].insert(
            SupabaseBookshelf(userId = userId, bookId = book.id, shelfType = shelfType, bookTitle = book.title, bookCoverUrl = book.coverUrl))
    }
}
