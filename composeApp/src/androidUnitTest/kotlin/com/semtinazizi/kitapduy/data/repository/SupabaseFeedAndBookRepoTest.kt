package com.semtinazizi.kitapduy.data.repository

import com.russhwolf.settings.Settings
import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.model.UserProfile
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

class SupabaseFeedAndBookRepoTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun fakeSettings(): Settings {
        val values = mutableMapOf<String, Any>()
        return Proxy.newProxyInstance(Settings::class.java.classLoader, arrayOf(Settings::class.java)) { _, method, args ->
            val key = args?.firstOrNull() as? String
            when {
                method.name.startsWith("put") && key != null && args.size >= 2 -> { values[key] = args[1]; Unit }
                method.name.startsWith("get") && key != null -> values[key] ?: args.getOrNull(1)
                method.name == "hasKey" && key != null -> values.containsKey(key)
                method.name == "remove" && key != null -> { values.remove(key); Unit }
                method.name == "clear" -> { values.clear(); Unit }
                method.name == "getSize" -> values.size
                method.name == "getKeys" -> values.keys
                else -> null
            }
        } as Settings
    }

    private val auth = object : AuthRepository by DummyAuthRepository() {
        override val currentUserUid = "viewer-1"
        override suspend fun getUserProfile(uid: String) = UserProfile(uid = uid, name = "Okur")
    }

    @Test
    fun getFeedItemsIncludesNullCommentsAndDoesNotFabricateSamplePostsOnError() = runTest {
        var capturedOrFilter: String? = null
        val okEngine = MockEngine { request ->
            capturedOrFilter = request.url.parameters["or"]
            respond(
                """[{"id":1,"user_id":"u1","user_name":"Ayşe","book_id":"","book_name":"Anket","comment":null,"poll_options":["A","B"],"likes":0}]""",
                HttpStatusCode.OK,
                jsonHeaders
            )
        }
        val client = createSupabaseClient("https://example.invalid", "key") { install(Postgrest); httpEngine = okEngine }
        try {
            val items = SupabaseFeedRepository(client, auth, fakeSettings()).getFeedItems(excludeListen = true)
            assertNotNull(capturedOrFilter)
            assertTrue(capturedOrFilter!!.contains("comment.is.null"), "NULL comments must not be excluded by neq filter: $capturedOrFilter")
            assertEquals(1, items.size)
        } finally {
            client.close()
        }

        val errClient = createSupabaseClient("https://example.invalid", "key") {
            install(Postgrest)
            httpEngine = MockEngine { respond("""{"message":"offline"}""", HttpStatusCode.ServiceUnavailable, jsonHeaders) }
        }
        try {
            val repo = SupabaseFeedRepository(errClient, auth, fakeSettings())
            // Must return empty list rather than fake "Ahmet Yılmaz" post in non-CI mode
            assertEquals(emptyList(), repo.getFeedItems(excludeListen = true))
            // Failed insert must return false and never pretend success
            assertFalse(repo.createPost("u1", "Ayşe", null, "", "", "Test", false, null, null))
        } finally {
            errClient.close()
        }
    }

    @Test
    fun createQuotePostSendsStartPosAndNormalizesMediaUrls() = runTest {
        var capturedBody = ""
        val engine = MockEngine { request ->
            if (request.method == HttpMethod.Post) {
                capturedBody = request.body.toByteArray().decodeToString()
                respond("[]", HttpStatusCode.Created, jsonHeaders)
            } else respond("[]", HttpStatusCode.OK, jsonHeaders)
        }
        val client = createSupabaseClient("https://example.invalid", "key") { install(Postgrest); httpEngine = engine }
        try {
            val ok = SupabaseFeedRepository(client, auth, fakeSettings()).createQuotePost(
                userId = "viewer-1",
                userName = "Okur",
                userPhoto = "https://pub-7327d7da4dcf4c1bbf6d53bacbbe276c.r2.dev/avatars/u.jpg",
                bookId = "book-1",
                bookName = "Nutuk",
                comment = "\"Büyük söz\"",
                isPremium = false,
                imageUrl = "https://ancient-shape-df97.samatya231.workers.dev/covers/nutuk.jpg",
                audioUrl = "https://pub-7327d7da4dcf4c1bbf6d53bacbbe276c.r2.dev/audio/nutuk.mp3",
                startPos = 12_345L
            )
            assertTrue(ok)
            val parsed = Json.parseToJsonElement(capturedBody)
            val obj = if (parsed is kotlinx.serialization.json.JsonArray) parsed.single().jsonObject else parsed.jsonObject
            assertEquals(12_345L, obj.getValue("start_pos").jsonPrimitive.long)
            assertEquals("https://ses.kitapduy.com.tr/audio/nutuk.mp3", obj.getValue("audio_url").jsonPrimitive.content)
            assertEquals("https://ses.kitapduy.com.tr/covers/nutuk.jpg", obj.getValue("image_url").jsonPrimitive.content)
        } finally {
            client.close()
        }
    }

    @Test
    fun bookRepositoryPaginatesPast1000RowsFiltersGhostsPreservesLanguageTagsAndResolvesPodcastEpisode() = runTest {
        val page1 = (1..1000).joinToString(prefix = "[", postfix = "]") { i ->
            """{"id":"b_$i","title":"Kitap $i","author":"Yazar","category":"Roman","cover_url":"https://ancient-shape-df97.samatya231.workers.dev/c/$i.jpg","audio_url":"https://pub-7327d7da4dcf4c1bbf6d53bacbbe276c.r2.dev/a/$i.mp3","language":"en","tags":["Roman","Klasik"]}"""
        }
        val page2 = """[
          {"id":"b_1001","title":"Valid English Book","author":"Author","category":"Roman","cover_url":"covers/1001.jpg","audio_url":"audio/1001.mp3","language":"EN","tags":["Classics"]},
          {"id":"ghost_1","title":"","author":"Anon"},
          {"id":"ghost_2","title":"İsimsiz Kitap","author":"Anon"}
        ]"""
        val singleRow = """[{"id":"b_1001","title":"Valid English Book","author":"Author","category":"Roman","cover_url":"covers/1001.jpg","audio_url":"audio/1001.mp3","language":"en","tags":["Classics"]}]"""
        val podcastsRow = """[{"id":"111hz","name":"111 Hz","author":"Sunucu","rss_url":"https://rss.example/feed.xml","image_url":"covers/pod.jpg","category":"Podcast"}]"""
        val episodeAudioUrl = "https://ancient-shape-df97.samatya231.workers.dev/podcasts/ep1.mp3"
        val rssXml = """<rss><channel><item><title>Birinci Bölüm</title><enclosure url="$episodeAudioUrl" type="audio/mpeg"/></item></channel></rss>"""

        var booksListCalls = 0
        val supabaseEngine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("/rest/v1/books") && request.url.parameters["id"] == "eq.b_1001" ->
                    respond(singleRow, HttpStatusCode.OK, jsonHeaders)
                path.endsWith("/rest/v1/books") -> {
                    val rangeHeader = request.headers[HttpHeaders.Range].orEmpty()
                    val offsetParam = request.url.parameters["offset"]
                    val isFirstPage = offsetParam == "0" || rangeHeader.contains("0-999") || booksListCalls == 0
                    booksListCalls++
                    val body = if (isFirstPage) page1 else page2
                    respond(body, HttpStatusCode.OK, jsonHeaders)
                }
                path.endsWith("/rest/v1/podcasts") ->
                    respond(podcastsRow, HttpStatusCode.OK, jsonHeaders)
                else -> respond("[]", HttpStatusCode.OK, jsonHeaders)
            }
        }
        val rssHttp = HttpClient(MockEngine { request ->
            if (request.url.toString() == "https://rss.example/feed.xml") {
                respond(rssXml, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/xml"))
            } else respond("", HttpStatusCode.NotFound)
        })
        val client = createSupabaseClient("https://example.invalid", "key") { install(Postgrest); httpEngine = supabaseEngine }
        try {
            val repo = SupabaseBookRepository(client, rssHttp)
            val books = repo.getBooks()
            assertEquals(2, booksListCalls)
            assertEquals(1001, books.size)
            assertEquals("en", books.last().language)
            assertEquals(listOf("Classics"), books.last().tags)
            assertEquals("https://ses.kitapduy.com.tr/c/1.jpg", books.first().coverUrl)
            assertEquals("https://ses.kitapduy.com.tr/a/1.mp3", books.first().audioUrl)

            // Subsequent call serves from in-memory cache; invalidateCache forces fresh fetch
            assertEquals(1001, repo.getBooks().size)
            assertEquals(2, booksListCalls)
            repo.invalidateCache()
            assertEquals(1001, repo.getBooks().size)
            assertEquals(4, booksListCalls)

            val byId = repo.getBookById("b_1001")
            assertNotNull(byId)
            assertEquals("en", byId.language)

            val podcastId = "podcast_111hz_EP_${episodeAudioUrl.hashCode()}"
            val resolved = repo.resolvePodcastEpisode(podcastId)
            assertNotNull(resolved)
            assertEquals("111 Hz", resolved.second)
            assertEquals("Birinci Bölüm", resolved.first.title)
            assertEquals("https://ses.kitapduy.com.tr/podcasts/ep1.mp3", resolved.first.audioUrl)
        } finally {
            client.close()
            rssHttp.close()
        }
    }

    @Test
    fun feedActionsBookCommentsNormalizesReactionLikesAndReplyCountAndEditsComment() = runTest {
        var patchFilterId: String? = null
        var patchFilterUid: String? = null
        val engine = MockEngine { request ->
            when (request.method) {
                HttpMethod.Get -> respond(
                    """[{"id":55,"user_id":"viewer-1","user_name":"Okur","user_photo":"avatars/okur.jpg","book_id":"b1","book_name":"Kitap","comment":"Çok güzel","liked_by":"viewer-1:1,u2:3","comment_count":4,"is_premium":true,"created_at":"2026-01-01T00:00:00Z"}]""",
                    HttpStatusCode.OK,
                    jsonHeaders
                )
                HttpMethod.Patch -> {
                    patchFilterId = request.url.parameters["id"]
                    patchFilterUid = request.url.parameters["user_id"]
                    respond(
                        """[{"id":55,"user_id":"viewer-1","user_name":"Okur","book_id":"b1","book_name":"Kitap","comment":"Güncel yorum"}]""",
                        HttpStatusCode.OK,
                        jsonHeaders
                    )
                }
                else -> respond("[]", HttpStatusCode.OK, jsonHeaders)
            }
        }
        val client = createSupabaseClient("https://example.invalid", "key") { install(Postgrest); httpEngine = engine }
        try {
            val actions = FeedActionsRepository(client, auth)
            val comments = actions.bookComments("b1")
            assertEquals(1, comments.size)
            assertEquals(listOf("viewer-1", "u2"), comments.single().likedBy)
            assertEquals(4, comments.single().replyCount)
            assertEquals("https://ses.kitapduy.com.tr/avatars/okur.jpg", comments.single().userPhotoUrl)

            actions.editPostComment("55", "  Güncel yorum  ")
            assertEquals("eq.55", patchFilterId)
            assertEquals("eq.viewer-1", patchFilterUid)
        } finally {
            client.close()
        }
    }
}
