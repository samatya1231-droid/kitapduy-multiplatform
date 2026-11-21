package com.semtinazizi.kitapduy.data.repository

import com.russhwolf.settings.Settings
import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.model.UserProfile
import com.semtinazizi.kitapduy.ui.components.dialogs.atmosphereSounds
import com.semtinazizi.kitapduy.ui.components.dialogs.clampQuoteStartPosition
import com.semtinazizi.kitapduy.ui.components.dialogs.sleepTimerOptions
import com.semtinazizi.kitapduy.util.KmpBookProgressManager
import com.semtinazizi.kitapduy.util.UrlSanitizer
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import java.lang.reflect.Proxy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrossPlatformSyncAndPremiumPolicyTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun fakeSettings(): Settings {
        val values = mutableMapOf<String, Any>()
        return Proxy.newProxyInstance(
            Settings::class.java.classLoader,
            arrayOf(Settings::class.java)
        ) { _, method, args ->
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

    private fun authAs(uid: String, name: String = "Test Okur"): AuthRepository =
        object : AuthRepository by DummyAuthRepository() {
            override val currentUserUid: String = uid
            override suspend fun getUserProfile(uid: String) = UserProfile(uid = uid, name = name)
        }

    @Test
    fun atmosphereLastTwoArePremiumAndSleepTimerHasPremiumOptions() {
        assertEquals(4, atmosphereSounds.size)
        assertFalse(atmosphereSounds[0].isPremium)
        assertFalse(atmosphereSounds[1].isPremium)
        assertTrue(atmosphereSounds[2].isPremium)
        assertTrue(atmosphereSounds[3].isPremium)

        val freeTimers = sleepTimerOptions.filter { !it.isPremium }.map { it.id }
        val premiumTimers = sleepTimerOptions.filter { it.isPremium }.map { it.id }
        assertEquals(listOf("15", "30", "45"), freeTimers)
        assertTrue(premiumTimers.containsAll(listOf("60", "90", "120", "book_end")))
    }

    @Test
    fun quoteStartPositionClampingHandlesZeroBoundariesAndOverflow() {
        assertEquals(0L, clampQuoteStartPosition(-5_000L, 100_000L))
        assertEquals(25_000L, clampQuoteStartPosition(25_000L, 100_000L))
        assertEquals(85_000L, clampQuoteStartPosition(98_000L, 100_000L))
        assertEquals(0L, clampQuoteStartPosition(20_000L, 10_000L))
        assertEquals(42_000L, clampQuoteStartPosition(42_000L, 0L))
    }

    @Test
    fun urlSanitizerPreservesAndroidResourceAndFileUris() {
        val rawUri = "android.resource://com.semtinazizi.kitapduy/raw/kitapduy_tanitim"
        assertEquals(rawUri, UrlSanitizer.normalizeMediaUrl(rawUri))
        val bundleUri = "file:///var/containers/KitapDuy.app/rain_sound.mp3"
        assertEquals(bundleUri, UrlSanitizer.normalizeMediaUrl(bundleUri))
    }

    @Test
    fun progressManagerIncrementsRevisionOnProgressAndLastPlayedSave() {
        val manager = KmpBookProgressManager(fakeSettings())
        val initial = manager.progressRevision.value
        manager.saveLastPlayedBook("book_1")
        assertTrue(manager.progressRevision.value > initial)
        val afterLast = manager.progressRevision.value
        manager.saveProgress("book_1", 19_000L)
        assertTrue(manager.progressRevision.value > afterLast)
        assertEquals(19_000L, manager.getProgress("book_1"))
    }

    @Test
    fun bookCommentsIncludeAndroidTimestampedCommentsAndExcludeAudioQuotesAndListenStatus() = runTest {
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/rest/v1/posts")) {
                respond(
                    """
                    [
                      {"id":1,"created_at":"2026-11-20 12:00:00+00","user_id":"android_u1","user_name":"Selnur Kara","comment":"Harika bir bölüm!","start_pos":45000,"audio_url":null,"is_premium":true},
                      {"id":2,"created_at":"2026-11-20T12:05:00Z","user_id":"ios_u2","user_name":"Ahmet","comment":"Sesli alıntı","start_pos":30000,"audio_url":"https://ses.kitapduy.com.tr/book.mp3"},
                      {"id":3,"created_at":"2026-11-20T12:10:00Z","user_id":"android_u3","user_name":"Mehmet","comment":"Bu kitabı dinlemeye başladı 🎧","start_pos":null,"audio_url":null}
                    ]
                    """.trimIndent(),
                    HttpStatusCode.OK,
                    jsonHeaders
                )
            } else {
                respond("[]", HttpStatusCode.OK, jsonHeaders)
            }
        }
        val client = createSupabaseClient("https://example.invalid", "test-key") {
            install(Postgrest)
            httpEngine = engine
        }
        try {
            val repo = FeedActionsRepository(client, authAs("ios_u2"))
            val comments = repo.bookComments("book_123")
            assertEquals(1, comments.size)
            assertEquals("Selnur Kara", comments.single().userName)
            assertEquals("Harika bir bölüm!", comments.single().text)
            assertTrue(comments.single().isPremium)
            assertTrue(comments.single().timestamp > 0L)
        } finally {
            client.close()
        }
    }

    @Test
    fun pollVoteSendsNotificationToPollOwner() = runTest {
        val notificationInserts = mutableListOf<String>()
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("/rest/v1/posts") && request.method == HttpMethod.Get -> {
                    respond(
                        """[{"id":55,"user_id":"android_owner","user_name":"Selnur Kara","poll_options":["A","B"],"poll_votes":{},"poll_voters":[]}]""",
                        HttpStatusCode.OK,
                        jsonHeaders
                    )
                }
                path.endsWith("/rest/v1/posts") && request.method == HttpMethod.Patch -> {
                    respond("[]", HttpStatusCode.OK, jsonHeaders)
                }
                path.endsWith("/rest/v1/notifications") && request.method == HttpMethod.Post -> {
                    notificationInserts.add(request.body.toString())
                    respond("[]", HttpStatusCode.Created, jsonHeaders)
                }
                else -> respond("[]", HttpStatusCode.OK, jsonHeaders)
            }
        }
        val client = createSupabaseClient("https://example.invalid", "test-key") {
            install(Postgrest)
            httpEngine = engine
        }
        try {
            val repo = FeedActionsRepository(client, authAs("ios_voter", "Ali Okur"))
            repo.vote("55", 0)
            assertEquals(1, notificationInserts.size)
        } finally {
            client.close()
        }
    }

    @Test
    fun searchUsersMergesSupabaseUsersAndAndroidPostAuthorsWithoutEarlyReturn() = runTest {
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("/rest/v1/users") -> respond(
                    """[{"uid":"ios_1","name":"Selnur iOS","username":"selnur_ios","is_premium":false}]""",
                    HttpStatusCode.OK,
                    jsonHeaders
                )
                path.endsWith("/rest/v1/posts") -> respond(
                    """[
                        {"id":10,"user_id":"android_selnur","user_name":"Selnur Kara","user_photo":"https://ses.kitapduy.com.tr/selnur.jpg","is_premium":true},
                        {"id":11,"user_id":"ios_1","user_name":"Selnur iOS","user_photo":"","is_premium":false}
                    ]""",
                    HttpStatusCode.OK,
                    jsonHeaders
                )
                else -> respond("[]", HttpStatusCode.OK, jsonHeaders)
            }
        }
        val client = createSupabaseClient("https://example.invalid", "test-key") {
            install(Postgrest)
            httpEngine = engine
        }
        try {
            val repo = SupabaseSocialRepository(client, authAs("me"), settings = fakeSettings())
            val results = repo.searchUsers("Selnur")
            assertEquals(2, results.size)
            assertEquals(setOf("ios_1", "android_selnur"), results.map { it.uid }.toSet())
            val androidUser = results.first { it.uid == "android_selnur" }
            assertEquals("Selnur Kara", androidUser.name)
            assertTrue(androidUser.isPremium)
        } finally {
            client.close()
        }
    }

    @Test
    fun submitBookRequestPostsToBookRequestsTableInLocalFixtureMode() = runTest {
        var postedToBookRequests = false
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/rest/v1/book_requests") && request.method == HttpMethod.Post) {
                postedToBookRequests = true
                respond("[]", HttpStatusCode.Created, jsonHeaders)
            } else {
                respond("[]", HttpStatusCode.OK, jsonHeaders)
            }
        }
        val client = createSupabaseClient("https://example.invalid", "test-key") {
            install(Postgrest)
            httpEngine = engine
        }
        try {
            val ok = CrossPlatformBridgeHelper.submitBookRequest(
                httpClient = null,
                supabaseClient = client,
                userId = "u1",
                userName = "Rahmi",
                bookTitle = "Satranç",
                author = "Stefan Zweig",
                useLocalFixture = true
            )
            assertTrue(ok)
            assertTrue(postedToBookRequests)
        } finally {
            client.close()
        }
    }

    @Test
    fun submitBookRequestPostsToCrossPlatformBridgeInProductionAndRespectsStatus() = runTest {
        val capturedBodies = mutableListOf<String>()
        var nextStatus = HttpStatusCode.Created
        val httpClient = HttpClient(MockEngine { request ->
            assertEquals("submit_book_request", request.url.parameters["action"])
            assertEquals(HttpMethod.Post, request.method)
            val bodyText = (request.body as? TextContent)?.text.orEmpty()
            capturedBodies.add(bodyText)
            respond("""{"ok":true}""", nextStatus, jsonHeaders)
        })
        try {
            assertTrue(
                CrossPlatformBridgeHelper.submitBookRequest(
                    httpClient = httpClient,
                    supabaseClient = null,
                    userId = "ios_u1",
                    userName = "Rahmi",
                    bookTitle = "Satranç",
                    author = "",
                    useLocalFixture = false
                )
            )
            assertEquals(1, capturedBodies.size)
            assertTrue(capturedBodies[0].contains(""""title":"Satranç""""))
            assertTrue(capturedBodies[0].contains(""""user_id":"ios_u1""""))

            nextStatus = HttpStatusCode.OK
            assertTrue(
                CrossPlatformBridgeHelper.submitBookRequest(
                    httpClient = httpClient,
                    userId = "ios_u1",
                    userName = "Rahmi",
                    bookTitle = "Satranç",
                    author = "Stefan Zweig",
                    useLocalFixture = false
                )
            )

            nextStatus = HttpStatusCode.TooManyRequests
            assertFalse(
                CrossPlatformBridgeHelper.submitBookRequest(
                    httpClient = httpClient,
                    userId = "ios_u1",
                    userName = "Rahmi",
                    bookTitle = "6. Kitap",
                    author = "",
                    useLocalFixture = false
                )
            )

            assertFalse(
                CrossPlatformBridgeHelper.submitBookRequest(
                    httpClient = httpClient,
                    userId = "ios_u1",
                    userName = "Rahmi",
                    bookTitle = "A",
                    author = "",
                    useLocalFixture = false
                )
            )
        } finally {
            httpClient.close()
        }
    }

    @Test
    fun submitBookRequestDetailedParsesAlreadyRequestedAndRateLimitError() = runTest {
        var nextStatus = HttpStatusCode.OK
        var nextBody = """{"ok":true,"status":"already_requested","id":"abc"}"""
        val httpClient = HttpClient(MockEngine { _ ->
            respond(nextBody, nextStatus, jsonHeaders)
        })
        try {
            val dup = CrossPlatformBridgeHelper.submitBookRequestDetailed(
                httpClient = httpClient,
                userId = "ios_u1",
                userName = "Rahmi",
                bookTitle = "Satranç",
                author = "",
                useLocalFixture = false
            )
            assertTrue(dup.success)
            assertTrue(dup.alreadyRequested)

            nextStatus = HttpStatusCode.TooManyRequests
            nextBody = """{"ok":false,"status":"rate_limited","error":"Bugün en fazla 5 kitap talep edebilirsiniz."}"""
            val limited = CrossPlatformBridgeHelper.submitBookRequestDetailed(
                httpClient = httpClient,
                userId = "ios_u1",
                userName = "Rahmi",
                bookTitle = "Altıncı Kitap",
                author = "",
                useLocalFixture = false
            )
            assertFalse(limited.success)
            assertEquals("Bugün en fazla 5 kitap talep edebilirsiniz.", limited.errorMessage)

            val oversizeTitle = CrossPlatformBridgeHelper.submitBookRequestDetailed(
                httpClient = httpClient,
                userId = "ios_u1",
                userName = "Rahmi",
                bookTitle = "a".repeat(161),
                author = "",
                useLocalFixture = false
            )
            assertFalse(oversizeTitle.success)
            assertTrue(oversizeTitle.errorMessage?.contains("160") == true)

            val oversizeAuthor = CrossPlatformBridgeHelper.submitBookRequestDetailed(
                httpClient = httpClient,
                userId = "ios_u1",
                userName = "Rahmi",
                bookTitle = "Satranç",
                author = "a".repeat(101),
                useLocalFixture = false
            )
            assertFalse(oversizeAuthor.success)
            assertTrue(oversizeAuthor.errorMessage?.contains("100") == true)
        } finally {
            httpClient.close()
        }
    }

    @Test
    fun socialRepositoryGetUserProfileFetchesAndroidFirestoreUserViaBridgeEvenWhenAuthFallbackExists() = runTest {
        var bridgeCalledForUid: String? = null
        val supabaseEngine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("/rest/v1/users") -> respond("[]", HttpStatusCode.OK, jsonHeaders)
                path.endsWith("/rest/v1/posts") -> respond("[]", HttpStatusCode.OK, jsonHeaders)
                else -> respond("[]", HttpStatusCode.OK, jsonHeaders)
            }
        }
        val bridgeHttpClient = HttpClient(MockEngine { request ->
            if (request.url.parameters["action"] == "user") {
                bridgeCalledForUid = request.url.parameters["uid"]
                respond(
                    """{"uid":"OIl22wh2Z9Vgq8HHPgvxXURdfAp2","name":"Selnur Kara","username":"selnur_k","bio":"Klasik okuru","photo_url":"https://ses.kitapduy.com.tr/selnur.jpg","cover_photo_url":"https://ses.kitapduy.com.tr/cover.jpg","badges":["premium","bookworm"],"premium_until":4102444800000,"is_premium":true,"streak":14}""",
                    HttpStatusCode.OK,
                    jsonHeaders
                )
            } else {
                respond("[]", HttpStatusCode.OK, jsonHeaders)
            }
        })
        val client = createSupabaseClient("https://example.invalid", "test-key") {
            install(Postgrest)
            httpEngine = supabaseEngine
        }
        try {
            // Even when authRepository returns a synthetic non-null "Okur" fallback,
            // SupabaseSocialRepository.getUserProfile MUST resolve the real Firestore profile via bridge.
            val socialRepo = SupabaseSocialRepository(
                supabaseClient = client,
                authRepository = authAs("ios_me", "Okur"),
                settings = fakeSettings(),
                httpClient = bridgeHttpClient
            )
            val resolved = socialRepo.getUserProfile("OIl22wh2Z9Vgq8HHPgvxXURdfAp2")
            assertEquals("OIl22wh2Z9Vgq8HHPgvxXURdfAp2", bridgeCalledForUid)
            assertEquals("Selnur Kara", resolved?.name)
            assertEquals("selnur_k", resolved?.username)
            assertEquals("Klasik okuru", resolved?.bio)
            assertTrue(resolved?.isPremium == true)
            assertEquals(14, resolved?.streak)
            assertTrue(resolved?.badges?.contains("bookworm") == true)
        } finally {
            client.close()
            bridgeHttpClient.close()
        }
    }

    @Test
    fun searchUsersStripsLeadingAtAndMergesFullBridgeProfileWithTurkishSorting() = runTest {
        var capturedUsersQuery = ""
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("/rest/v1/users") -> {
                    capturedUsersQuery = request.url.encodedQuery
                    respond(
                        """[{"uid":"dual_1","name":"Barış Arslan","username":"baris_a","bio":"","is_premium":false}]""",
                        HttpStatusCode.OK,
                        jsonHeaders
                    )
                }
                else -> respond("[]", HttpStatusCode.OK, jsonHeaders)
            }
        }
        val bridgeHttpClient = HttpClient(MockEngine { request ->
            if (request.url.parameters["action"] == "search_users") {
                respond(
                    """[
                        {"uid":"dual_1","name":"Barış Arslan","username":"baris_a","bio":"VIP Okur","photo_url":"https://ses.kitapduy.com.tr/b.jpg","cover_photo_url":"https://ses.kitapduy.com.tr/bc.jpg","badges":["premium","admin"],"premium_until":4102444800000,"is_premium":true,"streak":9},
                        {"uid":"android_2","name":"İrem Yılmaz","username":"irem_y","bio":"","is_premium":false}
                    ]""",
                    HttpStatusCode.OK,
                    jsonHeaders
                )
            } else {
                respond("[]", HttpStatusCode.OK, jsonHeaders)
            }
        })
        val client = createSupabaseClient("https://example.invalid", "test-key") {
            install(Postgrest)
            httpEngine = engine
        }
        try {
            val repo = SupabaseSocialRepository(client, authAs("me"), settings = fakeSettings(), httpClient = bridgeHttpClient)
            val results = repo.searchUsers("@baris")
            assertFalse(capturedUsersQuery.contains("%40"), "Leading @ must be stripped before Supabase ilike query: $capturedUsersQuery")
            assertTrue(capturedUsersQuery.contains("baris"))
            val baris = results.first { it.uid == "dual_1" }
            assertEquals("dual_1", results.first().uid)
            assertTrue(baris.isPremium)
            assertEquals("VIP Okur", baris.bio)
            assertEquals(9, baris.streak)
            assertTrue(baris.badges.contains("admin"))
            assertEquals("https://ses.kitapduy.com.tr/bc.jpg", baris.coverPhotoUrl)

            val sortedTurkish = SupabaseSocialRepository.mergeUserSearchResults(
                query = "irem",
                remoteUsers = listOf(UserProfile(uid = "u_other", name = "Ahmet İrem", isPremium = true)),
                bridgeUsers = listOf(UserProfile(uid = "u_irem", name = "İrem Yılmaz", isPremium = false)),
                postAuthors = emptyList()
            )
            assertEquals("u_irem", sortedTurkish.first().uid)
        } finally {
            client.close()
            bridgeHttpClient.close()
        }
    }

    @Test
    fun searchBridgeUsersParsesFirestoreBridgeUsersAndHandlesErrors() = runTest {
        var nextStatus = HttpStatusCode.OK
        val httpClient = HttpClient(MockEngine { request ->
            assertEquals("search_users", request.url.parameters["action"])
            assertEquals("Selnur", request.url.parameters["q"])
            if (nextStatus == HttpStatusCode.OK) {
                respond(
                    """[{"uid":"android_selnur","name":"Selnur Kara","username":"selnur_k","bio":"VIP Okur","photo_url":"https://ses.kitapduy.com.tr/selnur.jpg","badges":["premium"],"is_premium":true,"streak":5}]""",
                    HttpStatusCode.OK,
                    jsonHeaders
                )
            } else {
                respond("""{"error":"unsupported_action"}""", nextStatus, jsonHeaders)
            }
        })
        try {
            val found = CrossPlatformBridgeHelper.searchBridgeUsers(httpClient, "Selnur", useLocalFixture = false)
            assertEquals(1, found.size)
            assertEquals("android_selnur", found.single().uid)
            assertEquals("Selnur Kara", found.single().name)
            assertEquals("selnur_k", found.single().username)
            assertTrue(found.single().isPremium)

            nextStatus = HttpStatusCode.BadRequest
            assertTrue(CrossPlatformBridgeHelper.searchBridgeUsers(httpClient, "Selnur", useLocalFixture = false).isEmpty())
            assertTrue(CrossPlatformBridgeHelper.searchBridgeUsers(httpClient, "S", useLocalFixture = false).isEmpty())
        } finally {
            httpClient.close()
        }
    }

    @Test
    fun followUserSendsNotificationWithFollowerUidTargetToSupabaseAndIosPushBridge() = runTest {
        val notificationBodies = mutableListOf<String>()
        val pushBodies = mutableListOf<String>()
        val supabaseEngine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("/rest/v1/followers") && request.method == HttpMethod.Post -> {
                    respond("[]", HttpStatusCode.Created, jsonHeaders)
                }
                path.endsWith("/rest/v1/notifications") && request.method == HttpMethod.Post -> {
                    notificationBodies.add((request.body as? TextContent)?.text ?: request.body.toString())
                    respond("[]", HttpStatusCode.Created, jsonHeaders)
                }
                else -> respond("[]", HttpStatusCode.OK, jsonHeaders)
            }
        }
        val pushHttpClient = HttpClient(MockEngine { request ->
            if (request.url.encodedPath.endsWith("/pushSocialNotificationToIos")) {
                pushBodies.add((request.body as? TextContent)?.text.orEmpty())
            }
            respond("""{"ok":true}""", HttpStatusCode.OK, jsonHeaders)
        })
        val client = createSupabaseClient("https://example.invalid", "test-key") {
            install(Postgrest)
            httpEngine = supabaseEngine
        }
        try {
            val repo = SupabaseSocialRepository(
                supabaseClient = client,
                authRepository = authAs("ios_follower_9", "Zeynep"),
                settings = fakeSettings(),
                httpClient = pushHttpClient
            )
            repo.follow("ios_follower_9", "android_owner_1")
            assertEquals(1, notificationBodies.size)
            assertTrue(notificationBodies[0].contains(""""receiver_id":"android_owner_1""""))
            assertTrue(notificationBodies[0].contains(""""book_id":"ios_follower_9""""))
            assertTrue(notificationBodies[0].contains(""""type":"follow""""))
            if (!CrossPlatformBridgeHelper.isLocalFixture()) {
                assertEquals(1, pushBodies.size)
                assertTrue(pushBodies[0].contains(""""type":"FOLLOW""""))
                assertTrue(pushBodies[0].contains(""""postId":"ios_follower_9""""))
            }
        } finally {
            client.close()
            pushHttpClient.close()
        }
    }

    @Test
    fun bookCommentsExcludePollsAndFormatAndroidSpoilerPrefix() = runTest {
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/rest/v1/posts")) {
                respond(
                    """
                    [
                      {"id":10,"created_at":"2026-11-20T12:00:00Z","user_id":"android_u1","user_name":"Selnur Kara","comment":"[SPOILER] Son bölümde dedektif sırrı çözüyor","is_premium":true},
                      {"id":11,"created_at":"2026-11-20T12:05:00Z","user_id":"android_u2","user_name":"Ahmet","book_name":"Anket","comment":"Hangi kitap?","poll_options":["A","B"]}
                    ]
                    """.trimIndent(),
                    HttpStatusCode.OK,
                    jsonHeaders
                )
            } else {
                respond("[]", HttpStatusCode.OK, jsonHeaders)
            }
        }
        val client = createSupabaseClient("https://example.invalid", "test-key") {
            install(Postgrest)
            httpEngine = engine
        }
        try {
            val repo = FeedActionsRepository(client, authAs("me"))
            val comments = repo.bookComments("book_123")
            assertEquals(1, comments.size)
            assertEquals("⚠️ Spoiler: Son bölümde dedektif sırrı çözüyor", comments.single().text)
        } finally {
            client.close()
        }
    }

    @Test
    fun notificationTitleForTypeCoversPollVoteAndSocialTypes() {
        assertEquals("📊 Ankete Yeni Oy", CrossPlatformBridgeHelper.notificationTitleForType("poll_vote"))
        assertEquals("👋 Yeni Takipçi", CrossPlatformBridgeHelper.notificationTitleForType("FOLLOW"))
        assertEquals("❤️ Yeni Beğeni", CrossPlatformBridgeHelper.notificationTitleForType("like"))
        assertEquals("💬 Yeni Yorum", CrossPlatformBridgeHelper.notificationTitleForType("comment"))
        assertEquals("💬 Yorumuna Yanıt", CrossPlatformBridgeHelper.notificationTitleForType("reply"))
    }
}
