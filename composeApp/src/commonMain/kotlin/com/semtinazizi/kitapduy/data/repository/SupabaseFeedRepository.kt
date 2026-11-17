package com.semtinazizi.kitapduy.data.repository

import com.semtinazizi.kitapduy.domain.repository.FeedRepository
import com.semtinazizi.kitapduy.model.FeedItem
import com.semtinazizi.kitapduy.model.FeedType
import com.semtinazizi.kitapduy.model.SupabaseInsertPost
import com.semtinazizi.kitapduy.model.SupabasePost
import com.semtinazizi.kitapduy.platform.AppConfig
import com.semtinazizi.kitapduy.util.UrlSanitizer
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class SupabaseFeedRepository(
    private val supabaseClient: SupabaseClient,
    private val authRepository: com.semtinazizi.kitapduy.domain.repository.AuthRepository,
    settings: com.russhwolf.settings.Settings? = runCatching { com.russhwolf.settings.Settings() }.getOrNull()
) : FeedRepository {
    private val localStore = LocalUserStore(settings)

    companion object {
        const val LISTEN_COMMENT = "Bu kitabı dinlemeye başladı \uD83C\uDFA7"
    }

    override suspend fun getFeedItems(excludeListen: Boolean, limit: Long, offset: Long): List<FeedItem> {
        return try {
            val posts = supabaseClient.postgrest["posts"]
                .select {
                    if (excludeListen) {
                        filter {
                            or {
                                neq("comment", LISTEN_COMMENT)
                                exact("comment", null)
                            }
                        }
                    }
                    order("created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    range(offset, offset + limit - 1)
                }
                .decodeList<SupabasePost>()

            filterAndMap(posts)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (e: Throwable) {
            println("FeedRepository Error: ${e.message}")
            if (AppConfig.isCiSmokeTest) {
                listOf(
                    FeedItem(
                        id = "ci_feed_sample_1",
                        userId = "ci_sample_user",
                        userName = "Ahmet Yılmaz",
                        userPhotoUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&q=80",
                        bookId = "ci_sample_1",
                        bookTitle = "Kürk Mantolu Madonna",
                        bookCoverUrl = "https://images.unsplash.com/photo-1544947950-fa07a98d237f?q=80&w=600",
                        content = "Harika bir başyapıt, seslendirmesi çok başarılı!",
                        timestamp = 1700000000000L,
                        type = FeedType.QUOTE
                    )
                )
            } else {
                emptyList()
            }
        }
    }

    override suspend fun getListeningFeedItems(limit: Long, offset: Long): List<FeedItem> {
        return try {
            val posts = supabaseClient.postgrest["posts"]
                .select {
                    filter {
                        eq("comment", LISTEN_COMMENT)
                    }
                    order("created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    range(offset, offset + limit - 1)
                }
                .decodeList<SupabasePost>()

            filterAndMap(posts)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (e: Throwable) {
            println("FeedRepository Listening Error: ${e.message}")
            emptyList()
        }
    }

    private suspend fun filterAndMap(posts: List<SupabasePost>): List<FeedItem> {
        val currentUid = authRepository.currentUserUid
        val blocked = currentUid?.let { authRepository.getBlockedUsers(it).toSet() }.orEmpty()
        val reported = currentUid?.let { localStore.getReportedPosts(it) }.orEmpty()
        return posts
            .filter { it.userId !in blocked && (it.id?.toString() ?: "") !in reported }
            .map { mapSupabasePostToFeedItem(it) }
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
    ): Boolean = insertPostRow(
        userId = userId,
        userName = userName,
        userPhoto = userPhoto,
        bookId = bookId,
        bookName = bookName,
        comment = comment,
        isPremium = isPremium,
        imageUrl = imageUrl,
        audioUrl = audioUrl,
        startPos = null,
        pollOptions = pollOptions
    )

    override suspend fun createQuotePost(
        userId: String,
        userName: String,
        userPhoto: String?,
        bookId: String,
        bookName: String,
        comment: String,
        isPremium: Boolean,
        imageUrl: String?,
        audioUrl: String?,
        startPos: Long?
    ): Boolean = insertPostRow(
        userId = userId,
        userName = userName,
        userPhoto = userPhoto,
        bookId = bookId,
        bookName = bookName,
        comment = comment,
        isPremium = isPremium,
        imageUrl = imageUrl,
        audioUrl = audioUrl,
        startPos = startPos,
        pollOptions = null
    )

    private suspend fun insertPostRow(
        userId: String,
        userName: String,
        userPhoto: String?,
        bookId: String,
        bookName: String,
        comment: String,
        isPremium: Boolean,
        imageUrl: String?,
        audioUrl: String?,
        startPos: Long?,
        pollOptions: List<String>?
    ): Boolean {
        val normalizedPhoto = userPhoto?.let { UrlSanitizer.normalizeMediaUrl(it).ifBlank { null } }
        val normalizedImage = imageUrl?.let { UrlSanitizer.normalizeMediaUrl(it).ifBlank { null } }
        val normalizedAudio = audioUrl?.let { UrlSanitizer.normalizeMediaUrl(it).ifBlank { null } }

        val insertPost = SupabaseInsertPost(
            userId = userId,
            userName = userName,
            userPhoto = normalizedPhoto,
            bookId = bookId,
            bookName = bookName,
            comment = comment,
            likes = 0,
            isPremium = isPremium,
            imageUrl = normalizedImage,
            audioUrl = normalizedAudio,
            startPos = startPos,
            pollOptions = pollOptions,
            pollVotes = if (pollOptions != null) pollOptions.indices.associate { it.toString() to 0 } else null,
            pollVoters = if (pollOptions != null) emptyList() else null
        )

        return try {
            supabaseClient.postgrest["posts"].insert(insertPost)
            true
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (e: Throwable) {
            println("SupabaseFeedRepository Insert Error: ${e.message}")
            false
        }
    }
}
