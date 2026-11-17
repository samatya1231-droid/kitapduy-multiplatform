package com.semtinazizi.kitapduy.ui.viewmodel

import com.semtinazizi.kitapduy.data.repository.FeedActionsRepository
import com.semtinazizi.kitapduy.data.repository.StorageRepository
import com.semtinazizi.kitapduy.data.repository.mapSupabasePostToFeedItem
import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.domain.repository.BookRepository
import com.semtinazizi.kitapduy.domain.repository.FeedRepository
import com.semtinazizi.kitapduy.model.Book
import com.semtinazizi.kitapduy.model.FeedItem
import com.semtinazizi.kitapduy.model.FeedType
import com.semtinazizi.kitapduy.model.Podcast
import com.semtinazizi.kitapduy.model.PodcastEpisode
import com.semtinazizi.kitapduy.model.SupabasePost
import com.semtinazizi.kitapduy.model.UserProfile
import com.semtinazizi.kitapduy.util.UrlSanitizer
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class FeedMappingAndScreenModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun normalizeMediaUrlRewritesLegacyR2WorkersAndPreservesLoopbackAndLocalUris() {
        assertEquals(
            "https://ses.kitapduy.com.tr/audiobooks/Nutuk/01.mp3",
            UrlSanitizer.normalizeMediaUrl("https://ancient-shape-df97.samatya231.workers.dev/audiobooks/Nutuk/01.mp3?t=123")
        )
        assertEquals(
            "https://ses.kitapduy.com.tr/feed_images/u1/pic.jpg",
            UrlSanitizer.normalizeMediaUrl("https://pub-7327d7da4dcf4c1bbf6d53bacbbe276c.r2.dev/feed_images/u1/pic.jpg")
        )
        assertEquals(
            "https://ses.kitapduy.com.tr/u1/avatar.jpg",
            UrlSanitizer.normalizeMediaUrl("https://abc.r2.cloudflarestorage.com/kitapduyprofiller/u1/avatar.jpg?X-Amz-Signature=xyz")
        )
        assertEquals(
            "https://ses.kitapduy.com.tr/covers/S%C4%B1r%C3%A7a%20K%C3%B6%C5%9Fk.jpg",
            UrlSanitizer.normalizeMediaUrl("covers/Sırça Köşk.jpg")
        )
        assertEquals(
            "http://127.0.0.1:8080/test.mp3",
            UrlSanitizer.normalizeMediaUrl("http://127.0.0.1:8080/test.mp3")
        )
        assertEquals(
            "file:///var/mobile/book%201.mp3",
            UrlSanitizer.normalizeMediaUrl("file:///var/mobile/book 1.mp3")
        )
        assertEquals(
            "https://images.unsplash.com/photo-123",
            UrlSanitizer.normalizeMediaUrl("http://images.unsplash.com/photo-123")
        )
    }

    @Test
    fun mapSupabasePostToFeedItemHandlesQuoteVsImageAndNormalizesUrls() {
        val imageWithZeroStartPos = mapSupabasePostToFeedItem(
            SupabasePost(
                id = 10L,
                userId = "u1",
                userName = "Ali",
                userPhoto = "https://pub-7327d7da4dcf4c1bbf6d53bacbbe276c.r2.dev/avatars/u1.jpg",
                bookId = "",
                bookName = "",
                comment = "Kitap kahve keyfi",
                imageUrl = "https://ancient-shape-df97.samatya231.workers.dev/feed_images/u1/photo.png",
                audioUrl = null,
                startPos = 0L
            )
        )
        assertEquals(FeedType.IMAGE, imageWithZeroStartPos.type)
        assertEquals("https://ses.kitapduy.com.tr/feed_images/u1/photo.png", imageWithZeroStartPos.postImageUrl)
        assertEquals("https://ses.kitapduy.com.tr/avatars/u1.jpg", imageWithZeroStartPos.userPhotoUrl)

        val audioQuote = mapSupabasePostToFeedItem(
            SupabasePost(
                id = 11L,
                userId = "u1",
                userName = "Ali",
                bookId = "book-1",
                bookName = "Tutunamayanlar",
                comment = "\"Kötü resim asarım\"",
                imageUrl = "covers/tutunamayanlar.jpg",
                audioUrl = "https://pub-7327d7da4dcf4c1bbf6d53bacbbe276c.r2.dev/audio/tutunamayanlar.mp3",
                startPos = 45_000L
            )
        )
        assertEquals(FeedType.QUOTE, audioQuote.type)
        assertEquals("https://ses.kitapduy.com.tr/audio/tutunamayanlar.mp3", audioQuote.audioUrl)
        assertNull(audioQuote.postImageUrl)

        val blankImagePost = mapSupabasePostToFeedItem(
            SupabasePost(id = 12L, userId = "u1", userName = "Ali", comment = "Sadece yazı", imageUrl = "   ")
        )
        assertEquals(FeedType.QUOTE, blankImagePost.type)
        assertNull(blankImagePost.postImageUrl)
    }

    @Test
    fun computeListeningRibbonApplies24HourCutoffAndDeduplicatesByUserAndDisplayName() {
        val now = 1_700_000_000_000L
        val items = listOf(
            FeedItem("2", " u1 ", "Asya Kaya", "", "b2", "Book 2", "", "", timestamp = now - 120_000L, type = FeedType.LISTEN),
            FeedItem("1", "u1", "Asya Kaya", "", "b1", "Book 1", "", "", timestamp = now - 60_000L, type = FeedType.LISTEN),
            FeedItem("3", "u2", "B", "", "b3", "Book 3", "", "", timestamp = now - (25L * 3600L * 1000L), type = FeedType.LISTEN),
            FeedItem("4", "u3", "C", "", "b4", "Book 4", "", "", timestamp = now - 3600_000L, type = FeedType.COMMENT),
            FeedItem("5", "u4", "Asya Deniz", "", "b5", "Book 5", "", "", timestamp = now - 180_000L, type = FeedType.LISTEN),
            // Same reader appearing under multiple account/guest UIDs must render only once
            FeedItem("6", "uid-alt-1", "Asya", "", "b6", "Book 6", "", "", timestamp = now - 30_000L, type = FeedType.LISTEN),
            FeedItem("7", "uid-alt-2", " asya ", "", "b7", "Book 7", "", "", timestamp = now - 90_000L, type = FeedType.LISTEN),
            FeedItem("8", "uid-alt-3", "ASYA", "", "b8", "Book 8", "", "", timestamp = now - 150_000L, type = FeedType.LISTEN)
        )
        val ribbon = FeedScreenModel.computeListeningRibbon(items, nowMs = now)
        assertEquals(listOf("6", "1", "5"), ribbon.map { it.id })
    }

    @Test
    fun feedScreenModelSeparatesGeneralAndListeningPaginationAndHandlesFailures() = runTest {
        val createdPolls = mutableListOf<Triple<String, String, List<String>?>>()
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()

        val fakeFeedRepo = object : FeedRepository {
            override suspend fun getFeedItems(excludeListen: Boolean, limit: Long, offset: Long): List<FeedItem> {
                return if (offset == 0L) {
                    (1..50).map { idx ->
                        FeedItem(
                            id = "g_$idx", userId = "ug_$idx", userName = "User $idx", userPhotoUrl = "",
                            bookId = "", bookTitle = "", bookCoverUrl = "", content = "Post $idx",
                            timestamp = now - idx * 1000L, type = FeedType.QUOTE
                        )
                    }
                } else {
                    listOf(
                        FeedItem("g_51", "ug_51", "User 51", "", "", "", "", "Page 2", timestamp = now - 90_000L, type = FeedType.QUOTE)
                    )
                }
            }

            override suspend fun getListeningFeedItems(limit: Long, offset: Long): List<FeedItem> {
                return if (offset == 0L) {
                    (1..50).map { idx ->
                        FeedItem(
                            id = "l_$idx", userId = "ul_${idx % 3}", userName = "Listener ${idx % 3}", userPhotoUrl = "",
                            bookId = "b_$idx", bookTitle = "Book $idx", bookCoverUrl = "",
                            content = "Bu kitabı dinlemeye başladı \uD83C\uDFA7",
                            timestamp = now - idx * 1000L, type = FeedType.LISTEN
                        )
                    }
                } else {
                    listOf(
                        FeedItem("l_51", "ul_99", "Listener 99", "", "b_51", "Book 51", "", "Bu kitabı dinlemeye başladı \uD83C\uDFA7", timestamp = now - 90_000L, type = FeedType.LISTEN)
                    )
                }
            }

            override suspend fun createPost(
                userId: String, userName: String, userPhoto: String?, bookId: String, bookName: String,
                comment: String, isPremium: Boolean, imageUrl: String?, audioUrl: String?, pollOptions: List<String>?
            ): Boolean {
                createdPolls.add(Triple(bookName, comment, pollOptions))
                return true
            }

            override fun startRealtimeListening(onNewPost: (FeedItem) -> Unit) {}
            override fun stopRealtimeListening() {}
        }

        val failingStorage = object : StorageRepository {
            override suspend fun uploadProfilePicture(userId: String, bytes: ByteArray, extension: String) =
                Result.failure<String>(IllegalStateException("Auth expired"))
            override suspend fun uploadImage(folder: String, fileNamePrefix: String, bytes: ByteArray, extension: String) =
                Result.failure<String>(IllegalStateException("Upload failed cleanly"))
        }

        val client = createSupabaseClient("https://example.invalid", "key") {
            install(Postgrest)
            httpEngine = MockEngine { respond("[]", HttpStatusCode.OK) }
        }
        val bookRepo = object : BookRepository {
            override suspend fun getBooks() = listOf(Book(id = "b_51", title = "Book 51", author = "Author"))
            override suspend fun searchBooks(query: String) = emptyList<Book>()
            override suspend fun getPodcasts() = emptyList<Podcast>()
            override suspend fun searchPodcasts(query: String) = emptyList<Podcast>()
            override suspend fun getDailySummary(): Pair<String, String>? = null
            override suspend fun getDailyQuote(): Triple<String, String, String>? = null
            override suspend fun getFavoriteBookIds(userId: String) = emptySet<String>()
            override suspend fun getBookshelf(userId: String, shelfType: String) = emptyList<com.semtinazizi.kitapduy.model.SupabaseBookshelf>()
            override suspend fun resolvePodcastEpisode(
                bookId: String, fallbackTitle: String, fallbackCover: String, fallbackAudioUrl: String, fallbackAuthor: String
            ): Pair<PodcastEpisode, String>? = if (bookId.startsWith("podcast_")) {
                PodcastEpisode(bookId, fallbackTitle.ifBlank { "Ep 1" }, "https://ses.kitapduy.com.tr/ep.mp3") to "111 Hz"
            } else null
        }

        try {
            val auth = object : AuthRepository by com.semtinazizi.kitapduy.data.repository.DummyAuthRepository() {}
            val model = FeedScreenModel(fakeFeedRepo, failingStorage, FeedActionsRepository(client, auth), bookRepo)
            model.loadFeed(true)
            runCurrent()

            assertEquals(50, model.generalFeedItems.value.size)
            assertEquals(50, model.listeningFeedItems.value.size)
            assertEquals(3, model.listeningRibbonItems.value.size) // distinct by userId within 24h

            model.loadMore(isListeningTab = true)
            runCurrent()
            assertEquals(51, model.listeningFeedItems.value.size)
            assertTrue(model.listeningFeedItems.value.any { it.id == "l_51" })

            model.sharePoll("Favori türün?", listOf("Roman", "Tarih"), UserProfile(uid = "u1", name = "Okur"))
            runCurrent()
            assertEquals(Triple("Anket", "Favori türün?", listOf("Roman", "Tarih")), createdPolls.single())

            // Image failure must NOT create broken imageless post
            val ok = model.uploadAndCreateImagePost("Deneme", byteArrayOf(1, 2, 3), "jpg", UserProfile(uid = "u1", name = "Okur"))
            assertFalse(ok)
            assertEquals("Upload failed cleanly", model.actionError.value)
            assertEquals(1, createdPolls.size)

            // Silent refresh leaves loading state untouched
            model.refreshListeningStreamSilently()
            runCurrent()
            assertFalse(model.isLoading.value)

            // Target resolution for both books and podcast_ items
            val podcastTarget = model.resolveContentTarget(
                FeedItem("p1", "u1", "A", "", "podcast_111hz_EP_12", "Bölüm 1", "", "", timestamp = now, type = FeedType.LISTEN)
            )
            assertTrue(podcastTarget is FeedContentTarget.PodcastTarget)
            assertNotNull((podcastTarget as FeedContentTarget.PodcastTarget).episode)

            val bookTarget = model.resolveContentTarget(
                FeedItem("b1", "u1", "A", "", "b_51", "Book 51", "", "", timestamp = now, type = FeedType.LISTEN)
            )
            assertTrue(bookTarget is FeedContentTarget.BookTarget)
        } finally {
            client.close()
        }
    }
}
