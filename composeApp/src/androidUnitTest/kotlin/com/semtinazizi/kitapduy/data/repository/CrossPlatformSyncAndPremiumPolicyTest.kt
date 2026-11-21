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
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
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
    fun submitBookRequestPostsToBookRequestsTable() = runTest {
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
                supabaseClient = client,
                userId = "u1",
                userName = "Rahmi",
                bookTitle = "Satranç",
                author = "Stefan Zweig"
            )
            assertTrue(ok)
            assertTrue(postedToBookRequests)
        } finally {
            client.close()
        }
    }
}
