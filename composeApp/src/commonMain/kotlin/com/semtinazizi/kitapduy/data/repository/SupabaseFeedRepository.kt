package com.semtinazizi.kitapduy.data.repository

import com.semtinazizi.kitapduy.domain.repository.FeedRepository
import com.semtinazizi.kitapduy.model.FeedItem
import com.semtinazizi.kitapduy.model.FeedType
import com.semtinazizi.kitapduy.model.SupabaseInsertPost
import com.semtinazizi.kitapduy.model.SupabasePost
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

import com.semtinazizi.kitapduy.platform.AppConfig

class SupabaseFeedRepository(
    private val supabaseClient: SupabaseClient
) : FeedRepository {

    private fun fixUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""
        if (url.startsWith("http")) return url
        return "${AppConfig.cfPublicUrl.trimEnd('/')}/${url.trimStart('/')}"
    }
    
    private fun mapSupabasePostToFeedItem(post: SupabasePost): FeedItem {
        val isBookPost = !post.bookId.isNullOrEmpty()
        val isAutoStatus = post.comment?.contains("dinlemeye başladı \uD83C\uDFA7") == true
        val isQuotePost = post.startPos != null
        val hasImage = !post.imageUrl.isNullOrEmpty()

        val feedType = when {
            post.pollOptions != null || post.bookName == "Anket" -> FeedType.POLL
            isQuotePost -> FeedType.QUOTE
            hasImage && !isBookPost -> FeedType.IMAGE
            isBookPost && isAutoStatus -> FeedType.LISTEN
            post.audioUrl != null -> FeedType.VOICE
            isBookPost -> FeedType.COMMENT
            else -> FeedType.QUOTE
        }

        return FeedItem(
            id = post.id?.toString() ?: "",
            userId = post.userId ?: "",
            userName = post.userName ?: "Bilinmeyen Kullanıcı",
            userPhotoUrl = fixUrl(post.userPhoto),
            bookId = post.bookId ?: "",
            bookTitle = post.bookName ?: "",
            bookCoverUrl = if (isBookPost) fixUrl(post.imageUrl) else "",
            content = post.comment ?: "",
            likes = post.likes ?: 0,
            timestamp = 0L,
            type = feedType,
            isPremium = post.isPremium ?: false,
            postImageUrl = if (!isBookPost) fixUrl(post.imageUrl) else null,
            audioUrl = fixUrl(post.audioUrl),
            commentCount = post.commentCount ?: 0,
            pollOptions = post.pollOptions ?: emptyList(),
            pollVotes = post.pollVotes ?: emptyMap(),
            pollVoters = post.pollVoters ?: emptyList(),
            startPos = post.startPos
        )
    }

    override suspend fun getFeedItems(excludeListen: Boolean): List<FeedItem> {
        return try {
            val posts = supabaseClient.postgrest["posts"]
                .select {
                    if (excludeListen) {
                        filter {
                            neq("comment", "Bu kitabı dinlemeye başladı \uD83C\uDFA7")
                        }
                    }
                    order("created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    range(0, 50)
                }
                .decodeList<SupabasePost>()

            posts.map { mapSupabasePostToFeedItem(it) }
        } catch (e: Throwable) {
            println("FeedRepository Error: ${e.message}")
            listOf(
                FeedItem(
                    id = "error",
                    userId = "",
                    userName = "Hata",
                    userPhotoUrl = "",
                    bookId = "",
                    bookTitle = "",
                    bookCoverUrl = "",
                    content = e.message ?: "Bilinmeyen Hata",
                    timestamp = 0L,
                    type = FeedType.QUOTE
                )
            )
        }
    }
    
    override fun startRealtimeListening(onNewPost: (FeedItem) -> Unit) {
        // Realtime plugin iOS'ta crash'e neden olduğu için devre dışı
        println("FeedRepository: Realtime listening devre dışı (iOS stabilitesi için)")
    }

    override fun stopRealtimeListening() {
        // Realtime plugin devre dışı - no-op
    }

    override suspend fun createPost(
        userId: String,
        userName: String,
        userPhoto: String?,
        bookId: String,
        bookName: String,
        comment: String,
        isPremium: Boolean,
        imageUrl: String?,
        audioUrl: String?,
        pollOptions: List<String>?
    ): Boolean {
        return try {
            val insertPost = SupabaseInsertPost(
                userId = userId,
                userName = userName,
                userPhoto = userPhoto,
                bookId = bookId,
                bookName = bookName,
                comment = comment,
                likes = 0,
                isPremium = isPremium,
                imageUrl = imageUrl,
                audioUrl = audioUrl,
                pollOptions = pollOptions,
                pollVotes = if (pollOptions != null) pollOptions.associateWith { 0 } else null,
                pollVoters = if (pollOptions != null) emptyList() else null
            )
            
            supabaseClient.postgrest["posts"].insert(insertPost)
            true
        } catch (e: Throwable) {
            println("SupabaseFeedRepository Insert Error: ${e.message}")
            false
        }
    }
}
