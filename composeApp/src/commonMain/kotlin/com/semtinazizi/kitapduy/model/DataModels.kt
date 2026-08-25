@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.semtinazizi.kitapduy.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Android @Keep ve Firebase @PropertyName kaldırıldı — KMP uyumlu hale getirildi

enum class FeedType {
    READING, FINISHED, ON_HOLD, LIKE, COMMENT, LISTEN, POLL, QUOTE, IMAGE, VOICE
}

data class FeedItem(
    val id: String,
    val userId: String,
    val userName: String,
    val userPhotoUrl: String,
    val bookId: String,
    val bookTitle: String,
    val bookCoverUrl: String,
    val content: String,
    val likes: Int = 0,
    val timestamp: Long,
    val type: FeedType,
    val comment: String = content,
    val likedBy: List<String> = emptyList(),
    val badges: List<String> = emptyList(),
    val isPremium: Boolean = false,
    val pollOptions: List<String> = emptyList(),
    val pollVotes: Map<String, Int> = emptyMap(),
    val pollVoters: List<String> = emptyList(),
    val postImageUrl: String? = null,
    val commentCount: Int = 0,
    val audioUrl: String? = null,
    val startPos: Long? = null
)

data class Comment(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String? = null,
    val text: String = "",
    val isPremium: Boolean = false,
    val timestamp: Long = 0,
    val likedBy: List<String> = emptyList(),
    val replyCount: Int = 0
)

@Serializable
data class NotificationItem(
    val id: Long? = null,
    val message: String = "",
    @SerialName("book_id") val bookId: String? = null,
    @SerialName("receiver_id") val receiverId: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    val type: String = ""
) {
    val displayId: String get() = id?.toString() ?: ""
    val safeBookId: String get() = bookId ?: ""
    // Timestamp parsing platform-specific; şimdilik 0L döner
    val timestamp: Long get() = 0L
}

@Serializable
data class SupabasePost(
    val id: Long? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("user_name") val userName: String? = null,
    @SerialName("user_photo") val userPhoto: String? = null,
    @SerialName("is_premium") val isPremium: Boolean? = false,
    @SerialName("book_id") val bookId: String? = null,
    @SerialName("book_name") val bookName: String? = null,
    val comment: String? = null,
    val likes: Int? = null,
    @SerialName("liked_by") val likedBy: String? = null,
    @SerialName("comment_count") val commentCount: Int? = 0,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("audio_url") val audioUrl: String? = null,
    @SerialName("start_pos") val startPos: Long? = null,
    @SerialName("poll_options") val pollOptions: List<String>? = null,
    @SerialName("poll_votes") val pollVotes: Map<String, Int>? = null,
    @SerialName("poll_voters") val pollVoters: List<String>? = null
)

@Serializable
data class SupabaseInsertPost(
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String,
    @SerialName("user_photo") val userPhoto: String?,
    @SerialName("book_id") val bookId: String,
    @SerialName("book_name") val bookName: String,
    val comment: String,
    val likes: Int,
    @SerialName("is_premium") val isPremium: Boolean = false,
    @SerialName("image_url") val imageUrl: String?,
    @SerialName("audio_url") val audioUrl: String? = null,
    @SerialName("start_pos") val startPos: Long? = null,
    @SerialName("poll_options") val pollOptions: List<String>? = null,
    @SerialName("poll_votes") val pollVotes: Map<String, Int>? = null,
    @SerialName("poll_voters") val pollVoters: List<String>? = null
)

@Serializable
data class SupabaseComment(
    val id: Long? = null,
    @SerialName("post_id") val postId: Long,
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String,
    @SerialName("user_photo") val userPhoto: String?,
    @SerialName("is_premium") val isPremium: Boolean = false,
    @SerialName("comment_text") val text: String,
    @SerialName("created_at") val createdAt: String? = null,
    val likes: Int? = 0,
    @SerialName("liked_by") val likedBy: String? = null
)

@Serializable
data class SupabaseInsertFeedComment(
    @SerialName("post_id") val postId: Long,
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String,
    @SerialName("user_photo") val userPhoto: String?,
    @SerialName("is_premium") val isPremium: Boolean = false,
    @SerialName("comment_text") val commentText: String,
    val likes: Int = 0,
    @SerialName("liked_by") val likedBy: String = ""
)

@Serializable
data class SupabaseInsertNotification(
    @SerialName("receiver_id") val receiverId: String,
    val message: String,
    @SerialName("book_id") val bookId: String? = null,
    val type: String,
    @SerialName("is_read") val isRead: Boolean = false
)

@Serializable
data class SupabaseFollowRecord(
    @SerialName("follower_id") val followerId: String,
    @SerialName("following_id") val followingId: String
)

@Serializable
data class SupabaseBookshelf(
    val id: Long? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("book_id") val bookId: String,
    @SerialName("shelf_type") val shelfType: String,
    @SerialName("book_title") val bookTitle: String? = null,
    @SerialName("book_cover_url") val bookCoverUrl: String? = null
)

@Serializable
data class ChatMessage(
    val id: Long? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String,
    @SerialName("user_photo") val userPhoto: String? = null,
    val message: String,
    @SerialName("is_premium") val isPremium: Boolean = false,
    @SerialName("image_url") val imageUrl: String? = null,
    val likes: Int? = 0,
    @SerialName("liked_by") val likedBy: List<String>? = emptyList(),
    @SerialName("reply_to_name") val replyToName: String? = null,
    @SerialName("reply_to_message") val replyToMessage: String? = null,
    @SerialName("room") val room: String = "☕ Genel"
)

@Serializable
data class PoetryItem(
    val id: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val title: String = "",
    val author: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("user_photo_url") val userPhotoUrl: String? = null,
    val content: String = "",
    val likes: Int = 0,
    @SerialName("liked_by") val likedBy: List<String> = emptyList()
)

@Serializable
data class ArticleItem(
    val id: String = "",
    val title: String = "",
    val author: String = "KitapDuy Editör",
    val content: String = "",
    val headerImageUrl: String = "",
    val readingTime: Int = 5,
    val category: String = "Genel",
    val isPremium: Boolean = false,
    val timestamp: Long = 0L,
    val views: Int = 0
)

// ReadingClub: Firebase @PropertyName kaldırıldı, sadece @SerialName (Supabase) kaldı
@Serializable
data class ReadingClub(
    val id: String = "",
    @SerialName("created_at") var createdAt: String? = null,
    val name: String = "",
    val description: String = "",
    @SerialName("cover_url") var coverUrl: String = "",
    @SerialName("member_count") var memberCount: Int = 0,
    @SerialName("is_active") var isActive: Boolean = true,
    @SerialName("book_id") var bookId: String? = null,
    @SerialName("owner_id") var ownerId: String? = null
)

@Serializable
data class UserClub(
    @SerialName("user_id") val userId: String,
    @SerialName("club_id") val clubId: String,
    @SerialName("joined_at") val joinedAt: String? = null
)

@Serializable
data class FocusPresence(
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String,
    @SerialName("user_photo") val userPhoto: String? = null
)

@Serializable
data class QuizQuestion(
    val id: String = "",
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val hint: String = ""
)

@Serializable
data class LeaderboardEntry(
    @SerialName("user_id") val userId: String = "",
    @SerialName("user_name") val userName: String = "Anonim",
    @SerialName("total_score") val totalScore: Int = 0
)

@Serializable
data class AskidaKitap(
    val id: String = "",
    @SerialName("book_name") var bookName: String = "",
    @SerialName("donor_id") var donorId: String = "",
    @SerialName("donor_name") var donorName: String = "",
    @SerialName("donor_note") var donorNote: String = "",
    @SerialName("image_url") var imageUrl: String = "",
    var status: String = "AVAILABLE", // AVAILABLE, CLAIMED, COMPLETED
    @SerialName("claimer_id") var claimerId: String? = null,
    var city: String = "",
    var category: String = "Diğer",
    var condition: String = "İyi",
    var timestamp: Long = 0L,
    @SerialName("completed_at") var completedAt: Long? = null
)

@Serializable
data class AskidaMesaj(
    val id: String = "",
    @SerialName("book_id") var bookId: String = "",
    @SerialName("sender_id") var senderId: String = "",
    var text: String = "",
    var timestamp: Long = 0L
)
