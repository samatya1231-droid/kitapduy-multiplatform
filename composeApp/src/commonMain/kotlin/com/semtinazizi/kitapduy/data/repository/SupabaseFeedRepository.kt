package com.semtinazizi.kitapduy.data.repository

import com.semtinazizi.kitapduy.domain.repository.FeedRepository
import com.semtinazizi.kitapduy.model.FeedItem
import com.semtinazizi.kitapduy.model.FeedType
import com.semtinazizi.kitapduy.model.SupabaseInsertPost
import com.semtinazizi.kitapduy.model.SupabasePost
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.decodeRecord
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

import com.semtinazizi.kitapduy.platform.AppConfig

class SupabaseFeedRepository(
    private val supabaseClient: SupabaseClient
) : FeedRepository {

    private var realtimeChannel: io.github.jan.supabase.realtime.RealtimeChannel? = null
    private var realtimeJob: kotlinx.coroutines.Job? = null

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
        } catch (e: Exception) {
            println("SupabaseFeedRepository Error: ${e.message}")
            emptyList()
        }
    }
    
    override fun startRealtimeListening(onNewPost: (FeedItem) -> Unit) {
        realtimeJob?.cancel()
        
        realtimeJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            realtimeChannel?.unsubscribe()
            try {
                realtimeChannel = supabaseClient.channel("public:posts_feed")
                
                realtimeChannel?.postgresChangeFlow<io.github.jan.supabase.realtime.PostgresAction.Insert>(schema = "public") {
                    table = "posts"
                }?.onEach { action ->
                    val newPost = action.decodeRecord<SupabasePost>()
                    val feedItem = mapSupabasePostToFeedItem(newPost)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onNewPost(feedItem)
                    }
                }?.launchIn(this)

                realtimeChannel?.subscribe()
            } catch (e: Exception) {
                println("Realtime listening failed: ${e.message}")
            }
        }
    }

    override fun stopRealtimeListening() {
        val oldChannel = realtimeChannel
        realtimeChannel = null
        realtimeJob?.cancel()
        realtimeJob = null
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            oldChannel?.unsubscribe()
        }
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
        } catch (e: Exception) {
            println("SupabaseFeedRepository Insert Error: ${e.message}")
            false
        }
    }
}
