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
    private val supabaseClient: SupabaseClient,
    private val authRepository: com.semtinazizi.kitapduy.domain.repository.AuthRepository,
    settings: com.russhwolf.settings.Settings? = runCatching { com.russhwolf.settings.Settings() }.getOrNull()
) : FeedRepository {
    private val localStore = LocalUserStore(settings)

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

            val currentUid = authRepository.currentUserUid
            val blocked = currentUid?.let { authRepository.getBlockedUsers(it).toSet() }.orEmpty()
            val reported = currentUid?.let { localStore.getReportedPosts(it) }.orEmpty()
            posts
                .filter { it.userId !in blocked && (it.id?.toString() ?: "") !in reported }
                .map { mapSupabasePostToFeedItem(it) }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
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
                pollVotes = if (pollOptions != null) pollOptions.indices.associate { it.toString() to 0 } else null,
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
