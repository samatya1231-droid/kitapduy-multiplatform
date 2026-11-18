package com.semtinazizi.kitapduy.data.repository

import com.semtinazizi.kitapduy.domain.repository.*
import com.semtinazizi.kitapduy.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.Count
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SupabasePostDetailRepository(
    private val client: SupabaseClient,
    private val auth: AuthRepository,
    private val httpClient: io.ktor.client.HttpClient? = null
) : PostDetailRepository {
    private val lock = Mutex()
    private fun uid() = auth.currentUserUid?.takeIf { it.isNotBlank() } ?: error("Lütfen giriş yapın.")
    private fun clean(text: String): String = text.trim().also { require(it.isNotEmpty() && it.length <= 2000) { "Yorum 1–2000 karakter olmalı." } }
    private fun map(row: SupabaseComment) = Comment(id = requireNotNull(row.id).toString(),
        userId = row.userId, userName = row.userName, userPhotoUrl = resolveFeedUrl(row.userPhoto),
        text = row.text, isPremium = row.isPremium,
        likedBy = row.likedBy.orEmpty().split(',').filter { it.isNotBlank() },
        timestamp = row.createdAt?.let { runCatching { kotlinx.datetime.Instant.parse(it).toEpochMilliseconds() }.getOrNull() } ?: 0L)

    override suspend fun getPost(id: String): FeedItem? = client.postgrest["posts"].select {
        filter { eq("id", id.toLong()) }
    }.decodeSingleOrNull<SupabasePost>()?.let(::mapSupabasePostToFeedItem)

    override suspend fun getComments(postId: String): List<Comment> = client.postgrest["feed_comments"].select {
        filter { eq("post_id", postId.toLong()) }; order("created_at", Order.DESCENDING)
    }.decodeList<SupabaseComment>().map(::map)

    override suspend fun addComment(postId: String, text: String): Comment {
        val userId = uid()
        val body = clean(text)
        val profile = auth.getUserProfile(userId) ?: error("Profil yüklenemedi. Lütfen tekrar deneyin.")
        // The author always comes from the authenticated session, never the viewed profile.
        val row = SupabaseInsertFeedComment(postId.toLong(), userId, profile.name, profile.photoUrl, profile.hasPremium(), body)
        val saved = map(client.postgrest["feed_comments"].insert(row) { select() }.decodeSingle<SupabaseComment>())
        refreshCommentCount(postId)
        val targetPostOwner = runCatching {
            client.postgrest["posts"].select { filter { eq("id", postId.toLong()) } }.decodeSingleOrNull<SupabasePost>()?.userId
        }.getOrNull()
        if (!targetPostOwner.isNullOrBlank() && targetPostOwner != userId) {
            val senderName = profile.name.takeIf { it.isNotBlank() } ?: "Bir okur"
            CrossPlatformBridgeHelper.sendSocialNotification(
                supabaseClient = client,
                httpClient = httpClient,
                receiverId = targetPostOwner,
                senderId = userId,
                senderName = senderName,
                type = "comment",
                message = "$senderName gönderine yorum yaptı! 💬",
                targetId = postId
            )
        }
        return saved
    }

    override suspend fun editComment(postId: String, commentId: String, text: String): Comment {
        val userId = uid(); val body = clean(text)
        val updated = client.postgrest["feed_comments"].update({ set("comment_text", body) }) {
            filter { eq("id", commentId.toLong()); eq("post_id", postId.toLong()); eq("user_id", userId) }; select()
        }.decodeSingleOrNull<SupabaseComment>() ?: error("Yorum değiştirilemedi; silinmiş veya başka bir kullanıcıya ait olabilir.")
        return map(updated)
    }

    override suspend fun deleteComment(postId: String, commentId: String) {
        val userId = uid()
        val deleted = client.postgrest["feed_comments"].delete {
            filter { eq("id", commentId.toLong()); eq("post_id", postId.toLong()); eq("user_id", userId) }; select()
        }.decodeList<SupabaseComment>()
        check(deleted.isNotEmpty()) { "Yorum silinemedi; silinmiş veya başka bir kullanıcıya ait olabilir." }
        refreshCommentCount(postId)
    }

    override suspend fun likeComment(postId: String, commentId: String): Comment {
        var commentOwnerId: String? = null
        var currentUserId = ""
        var newlyLiked = false
        val updated = lock.withLock {
            val userId = uid()
            currentUserId = userId
            val row = client.postgrest["feed_comments"].select {
                filter { eq("id", commentId.toLong()); eq("post_id", postId.toLong()) }
            }.decodeSingle<SupabaseComment>()
            commentOwnerId = row.userId
            val likes = row.likedBy.orEmpty().split(',').filter { it.isNotBlank() }.toMutableSet()
            if (!likes.remove(userId)) {
                likes.add(userId)
                newlyLiked = true
            }
            val savedRow = client.postgrest["feed_comments"].update({ set("likes", likes.size); set("liked_by", likes.joinToString(",")) }) {
                filter { eq("id", commentId.toLong()); eq("post_id", postId.toLong()) }; select()
            }.decodeSingleOrNull<SupabaseComment>() ?: error("Beğeni kaydedilemedi.")
            map(savedRow)
        }
        if (newlyLiked && !commentOwnerId.isNullOrBlank() && commentOwnerId != currentUserId) {
            val senderName = runCatching { auth.getUserProfile(currentUserId)?.name }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Bir okur"
            CrossPlatformBridgeHelper.sendSocialNotification(
                supabaseClient = client,
                httpClient = httpClient,
                receiverId = commentOwnerId!!,
                senderId = currentUserId,
                senderName = senderName,
                type = "like",
                message = "$senderName yorumunu beğendi! ❤️",
                targetId = postId
            )
        }
        return updated
    }

    private suspend fun refreshCommentCount(postId: String) {
        // The comment is already committed. A counter failure must not invite a duplicate submission.
        try {
            val total = client.postgrest["feed_comments"].select {
                filter { eq("post_id", postId.toLong()) }; count(Count.EXACT); head = true
            }.countOrNull() ?: return
            client.postgrest["posts"].update({ set("comment_count", total) }) {
                filter { eq("id", postId.toLong()) }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Detail uses the actual returned comments; the feed counter can be reconciled later.
        }
    }
}
