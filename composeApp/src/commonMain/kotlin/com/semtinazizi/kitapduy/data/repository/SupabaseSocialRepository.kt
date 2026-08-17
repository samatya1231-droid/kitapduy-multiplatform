package com.semtinazizi.kitapduy.data.repository

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

class SupabaseSocialRepository(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository
) : SocialRepository {

    private fun fixUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""
        if (url.startsWith("http")) return url
        return "${AppConfig.cfPublicUrl.trimEnd('/')}/${url.trimStart('/')}"
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
        } catch (e: Exception) {
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

            posts.map { post ->
                val isBookPost = !post.bookId.isNullOrEmpty()
                val isAutoStatus = post.comment?.contains("dinlemeye başlad") == true
                val isQuotePost = post.startPos != null
                val hasImage = !post.imageUrl.isNullOrEmpty()

                val feedType = when {
                    post.bookName == "Anket" -> FeedType.POLL
                    isQuotePost -> FeedType.QUOTE
                    hasImage && !isBookPost -> FeedType.IMAGE
                    isBookPost && isAutoStatus -> FeedType.LISTEN
                    isBookPost -> FeedType.COMMENT
                    else -> FeedType.QUOTE
                }

                val safeLikedByList = post.likedBy?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

                FeedItem(
                    id = post.id?.toString() ?: "",
                    userId = post.userId ?: "",
                    userName = post.userName ?: "Okur",
                    userPhotoUrl = fixUrl(post.userPhoto),
                    bookId = post.bookId ?: "",
                    bookTitle = post.bookName ?: "",
                    bookCoverUrl = if (isBookPost) fixUrl(post.imageUrl) else "",
                    postImageUrl = if (!isBookPost) fixUrl(post.imageUrl) else null,
                    content = post.comment ?: "",
                    likes = post.likes ?: 0,
                    likedBy = safeLikedByList,
                    commentCount = post.commentCount ?: 0,
                    timestamp = 0L,
                    type = feedType,
                    isPremium = post.isPremium ?: false,
                    audioUrl = fixUrl(post.audioUrl),
                    startPos = post.startPos,
                    pollOptions = post.pollOptions ?: emptyList(),
                    pollVotes = post.pollVotes ?: emptyMap(),
                    pollVoters = post.pollVoters ?: emptyList()
                )
            }.sortedByDescending { it.id.toLongOrNull() ?: 0L }
        } catch (e: Exception) {
            println("SupabaseSocialRepository Error: ${e.message}")
            emptyList()
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
            emptyList()
        }
    }

    override suspend fun getFollowers(userId: String): List<SupabaseFollowRecord> {
        return try {
            supabaseClient.postgrest["followers"]
                .select { filter { eq("following_id", userId) } }
                .decodeList<SupabaseFollowRecord>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getFollowing(userId: String): List<SupabaseFollowRecord> {
        return try {
            supabaseClient.postgrest["followers"]
                .select { filter { eq("follower_id", userId) } }
                .decodeList<SupabaseFollowRecord>()
        } catch (e: Exception) {
            emptyList()
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
            false
        }
    }

    override suspend fun follow(followerId: String, followingId: String) {
        try {
            supabaseClient.postgrest["followers"].insert(SupabaseFollowRecord(followerId, followingId))
        } catch (e: Exception) {
            println("Social Follow Error: ${e.message}")
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
            println("Social Unfollow Error: ${e.message}")
        }
    }

    override suspend fun sendNotification(receiverId: String, message: String, type: String) {
        try {
            val notification = SupabaseInsertNotification(
                receiverId = receiverId,
                message = message,
                type = type
            )
            supabaseClient.postgrest["notifications"].insert(notification)
        } catch (e: Exception) {
            println("Social Notification Error: ${e.message}")
        }
    }
}
