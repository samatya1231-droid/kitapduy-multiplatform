package com.semtinazizi.kitapduy.data.repository

import com.russhwolf.settings.Settings
import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.domain.repository.SocialRepository
import com.semtinazizi.kitapduy.model.FeedItem
import com.semtinazizi.kitapduy.model.FeedType
import com.semtinazizi.kitapduy.model.SupabaseBookshelf
import com.semtinazizi.kitapduy.model.SupabaseFollowRecord
import com.semtinazizi.kitapduy.model.SupabaseInsertNotification
import com.semtinazizi.kitapduy.model.SupabasePost
import com.semtinazizi.kitapduy.model.UserProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import com.semtinazizi.kitapduy.platform.AppConfig
import com.semtinazizi.kitapduy.util.UrlSanitizer
import kotlinx.coroutines.CancellationException

class SupabaseSocialRepository(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository,
    settings: Settings? = runCatching { Settings() }.getOrNull(),
    private val httpClient: io.ktor.client.HttpClient? = null
) : SocialRepository {
    private val localStore = LocalUserStore(settings)

    private fun fixUrl(url: String?): String {
        val publicBase = AppConfig.cfPublicUrl.trim().trimEnd('/')
        val defaultBase = if (publicBase.startsWith("http://localhost") || publicBase.startsWith("http://127.0.0.1")) {
            publicBase
        } else {
            UrlSanitizer.CUSTOM_R2_BASE_URL
        }
        return UrlSanitizer.normalizeMediaUrl(url, defaultBase)
    }

    override suspend fun getUserProfile(userId: String): UserProfile? {
        authRepository.getUserProfile(userId)?.let { return it }
        CrossPlatformBridgeHelper.fetchBridgeUserProfile(httpClient, userId)?.let { return it }
        return try {
            val post = supabaseClient.postgrest["posts"]
                .select {
                    filter { eq("user_id", userId) }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(1)
                }
                .decodeList<SupabasePost>()
                .firstOrNull() ?: return null
            UserProfile(
                uid = userId,
                name = post.userName?.takeIf { it.isNotBlank() } ?: "KitapDuy Okuru",
                username = "okur_${userId.take(5)}",
                bio = "Merhaba, ben bir KitapDuy okuruyum!",
                photoUrl = fixUrl(post.userPhoto),
                isPremium = post.isPremium ?: false
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            null
        }
    }

    override suspend fun searchUsers(query: String): List<UserProfile> {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return emptyList()
        val blocked = authRepository.currentUserUid?.let { authRepository.getBlockedUsers(it).toSet() }.orEmpty()

        val remoteUsers = try {
            supabaseClient.postgrest["users"]
                .select {
                    filter {
                        or {
                            ilike("name", "%$cleanQuery%")
                            ilike("username", "%$cleanQuery%")
                        }
                    }
                }
                .decodeList<UserProfile>()
                .filter { it.uid !in blocked }
                .map {
                    it.copy(
                        photoUrl = fixUrl(it.photoUrl),
                        coverPhotoUrl = fixUrl(it.coverPhotoUrl)
                    )
                }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Throwable) {
            e.printStackTrace()
            emptyList()
        }

        if (remoteUsers.isNotEmpty()) return remoteUsers

        return try {
            supabaseClient.postgrest["posts"]
                .select {
                    filter {
                        ilike("user_name", "%$cleanQuery%")
                    }
                }
                .decodeList<SupabasePost>()
                .filter { !it.userId.isNullOrBlank() && it.userId !in blocked }
                .distinctBy { it.userId }
                .map { post ->
                    val uid = post.userId!!.trim()
                    UserProfile(
                        uid = uid,
                        name = post.userName?.takeIf { it.isNotBlank() } ?: "Okur",
                        username = "okur_${uid.take(5)}",
                        bio = "Merhaba, ben bir KitapDuy okuruyum!",
                        photoUrl = fixUrl(post.userPhoto),
                        isPremium = post.isPremium ?: false
                    )
                }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Throwable) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun fetchUserPosts(userId: String): List<FeedItem> {
        return try {
            val posts = supabaseClient.postgrest["posts"]
                .select {
                    filter { eq("user_id", userId) }
                    // Order cannot easily be parsed without raw string sometimes, but let's assume descending id
                }
                .decodeList<SupabasePost>()

            val currentUid = authRepository.currentUserUid
            val blocked = currentUid?.let { authRepository.getBlockedUsers(it).toSet() }.orEmpty()
            val reported = currentUid?.let { localStore.getReportedPosts(it) }.orEmpty()
            posts
                .filter { it.userId !in blocked && (it.id?.toString() ?: "") !in reported }
                .map(::mapSupabasePostToFeedItem)
                .sortedByDescending { it.id.toLongOrNull() ?: 0L }
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun fetchUserShelves(userId: String): List<SupabaseBookshelf> {
        return try {
            supabaseClient.postgrest["user_bookshelves"]
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<SupabaseBookshelf>()
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun getFollowers(userId: String): List<SupabaseFollowRecord> {
        return try {
            supabaseClient.postgrest["followers"]
                .select { filter { eq("following_id", userId) } }
                .decodeList<SupabaseFollowRecord>()
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun getFollowing(userId: String): List<SupabaseFollowRecord> {
        return try {
            supabaseClient.postgrest["followers"]
                .select { filter { eq("follower_id", userId) } }
                .decodeList<SupabaseFollowRecord>()
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun isFollowing(followerId: String, followingId: String): Boolean {
        return try {
            val result = supabaseClient.postgrest["followers"]
                .select {
                    filter {
                        eq("follower_id", followerId)
                        eq("following_id", followingId)
                    }
                }
                .decodeList<SupabaseFollowRecord>()
            result.isNotEmpty()
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun follow(followerId: String, followingId: String) {
        try {
            supabaseClient.postgrest["followers"].insert(SupabaseFollowRecord(followerId, followingId))
            val senderName = runCatching { authRepository.getUserProfile(followerId)?.name }
                .getOrNull()?.takeIf { it.isNotBlank() } ?: "Bir okur"
            CrossPlatformBridgeHelper.sendSocialNotification(
                supabaseClient = supabaseClient,
                httpClient = httpClient,
                receiverId = followingId,
                senderId = followerId,
                senderName = senderName,
                type = "follow",
                message = "$senderName seni takip etmeye başladı! 👋",
                targetId = followerId
            )
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun unfollow(followerId: String, followingId: String) {
        try {
            supabaseClient.postgrest["followers"].delete {
                filter {
                    eq("follower_id", followerId)
                    eq("following_id", followingId)
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun sendNotification(receiverId: String, message: String, type: String) {
        try {
            val notification = SupabaseInsertNotification(receiverId = receiverId, message = message, type = type)
            supabaseClient.postgrest["notifications"].insert(notification)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Throwable) {
            println("Social Notification Error: ${e.message}")
        }
    }

    /** Apple Guideline 1.2 - Icerik raporlama */
    override suspend fun reportPost(postId: String, reporterId: String, reason: String): Boolean {
        val cleanPostId = postId.trim()
        val cleanReporterId = reporterId.trim()
        val cleanReason = reason.trim()
        if (cleanPostId.isEmpty() || cleanReporterId.isEmpty() || cleanReason.isEmpty()) return false
        localStore.addReportedPost(cleanReporterId, cleanPostId)
        return try {
            try {
                supabaseClient.postgrest["reports"].insert(ReportRecord(cleanPostId, cleanReporterId, cleanReason))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Throwable) {
                println("Report Error (saved locally): ${e.message}")
            }
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            false
        }
    }

    /** Apple Guideline 1.2 - Kullanici engelleme */
    override suspend fun blockUser(blockerId: String, blockedUid: String): Boolean {
        val cleanBlocker = blockerId.trim()
        val cleanBlocked = blockedUid.trim()
        if (cleanBlocker.isEmpty() || cleanBlocked.isEmpty() || cleanBlocker == cleanBlocked) return false
        localStore.addBlockedUser(cleanBlocker, cleanBlocked)
        return try {
            try {
                supabaseClient.postgrest["blocked_users"].upsert(SocialBlockRecord(cleanBlocker, cleanBlocked))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Throwable) {
                println("Block Error (saved locally): ${e.message}")
            }
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            false
        }
    }
}

@kotlinx.serialization.Serializable
data class ReportRecord(
    @kotlinx.serialization.SerialName("post_id") val postId: String,
    @kotlinx.serialization.SerialName("reporter_id") val reporterId: String,
    @kotlinx.serialization.SerialName("reason") val reason: String
)

@kotlinx.serialization.Serializable
data class SocialBlockRecord(
    @kotlinx.serialization.SerialName("blocker_uid") val blockerUid: String,
    @kotlinx.serialization.SerialName("blocked_uid") val blockedUid: String
)

