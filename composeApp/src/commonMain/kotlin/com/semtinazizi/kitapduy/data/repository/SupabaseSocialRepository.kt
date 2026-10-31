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
    settings: Settings? = runCatching { Settings() }.getOrNull()
) : SocialRepository {
    private val localStore = LocalUserStore(settings)

    private fun fixUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""
        val resolved = if (url.startsWith("http")) url else "${AppConfig.cfPublicUrl.trimEnd('/')}/${url.trimStart('/')}"
        return UrlSanitizer.sanitizeUrl(resolved)
    }

    override suspend fun getUserProfile(userId: String): UserProfile? {
        return authRepository.getUserProfile(userId)
    }

    override suspend fun searchUsers(query: String): List<UserProfile> {
        if (query.isBlank()) return emptyList()
        return try {
            val users = supabaseClient.postgrest["users"]
                .select {
                    filter {
                        or {
                            ilike("name", "%$query%")
                            ilike("username", "%$query%")
                        }
                    }
                }
                .decodeList<UserProfile>()
                
            users.map {
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
    }

    override suspend fun fetchUserPosts(userId: String): List<FeedItem> {
        return try {
            val posts = supabaseClient.postgrest["posts"]
                .select {
                    filter { eq("user_id", userId) }
                    // Order cannot easily be parsed without raw string sometimes, but let's assume descending id
                }
                .decodeList<SupabasePost>()

            val blocked = authRepository.currentUserUid?.let { authRepository.getBlockedUsers(it).toSet() }.orEmpty()
            posts.filter { it.userId !in blocked }.map(::mapSupabasePostToFeedItem).sortedByDescending { it.id.toLongOrNull() ?: 0L }
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

