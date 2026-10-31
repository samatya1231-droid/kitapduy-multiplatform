package com.semtinazizi.kitapduy.data.repository

import com.russhwolf.settings.Settings
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.lang.reflect.Proxy
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
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalCoroutinesApi::class)
class SupabaseAuthSocialFallbackTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val pgrst205Body = """{"code":"PGRST205","details":null,"hint":null,"message":"Could not find the table 'public.users' in the schema cache"}"""

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        LocalUserStore.clearMemoryFallbackForTests()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        LocalUserStore.clearMemoryFallbackForTests()
    }

    private fun fakeSettings(): Settings {
        val values = mutableMapOf<String, Any>()
        return Proxy.newProxyInstance(
            Settings::class.java.classLoader,
            arrayOf(Settings::class.java)
        ) { _, method, args ->
            val key = args?.firstOrNull() as? String
            when {
                method.name.startsWith("put") && key != null && args.size >= 2 -> {
                    values[key] = args[1]
                    Unit
                }
                method.name.startsWith("get") && key != null -> values[key] ?: args.getOrNull(1)
                method.name == "hasKey" && key != null -> values.containsKey(key)
                method.name == "remove" && key != null -> {
                    values.remove(key)
                    Unit
                }
                method.name == "clear" -> {
                    values.clear()
                    Unit
                }
                method.name == "getSize" -> values.size
                method.name == "getKeys" -> values.keys
                else -> null
            }
        } as Settings
    }

    @Test
    fun getUserProfileFallsBackToAuthenticatedDemoUserWhenUsersTableIsMissing() = runTest {
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("/rest/v1/users") ->
                    respond(pgrst205Body, HttpStatusCode.NotFound, jsonHeaders)
                else ->
                    respond("[]", HttpStatusCode.OK, jsonHeaders)
            }
        }
        val settings = fakeSettings()
        val client = createSupabaseClient("https://example.invalid", "test-key") {
            install(Postgrest)
            install(Auth) {
                alwaysAutoRefresh = false
                autoLoadFromStorage = false
                enableLifecycleCallbacks = false
                sessionManager = com.semtinazizi.kitapduy.data.AppSessionManager(settings)
                codeVerifierCache = io.github.jan.supabase.auth.MemoryCodeVerifierCache()
            }
            httpEngine = engine
        }
        try {
            client.auth.importSession(
                UserSession(
                    accessToken = "token",
                    refreshToken = "refresh",
                    expiresIn = 3600,
                    tokenType = "bearer",
                    user = UserInfo(
                        id = "demo-uid-123",
                        aud = "authenticated",
                        email = "test@kitapduy.com"
                    )
                )
            )
            val repo = SupabaseAuthRepository(client, settings)
            val profile = repo.getUserProfile("demo-uid-123")
            assertNotNull(profile)
            assertEquals("demo-uid-123", profile.uid)
            assertEquals("Test", profile.name)
            assertEquals("test", profile.username)
        } finally {
            client.close()
        }
    }

    @Test
    fun getUserProfileFallsBackToAppleUserMetadataAndPersistsLocalEditsWhenUsersTableIsMissing() = runTest {
        val engine = MockEngine {
            respond(pgrst205Body, HttpStatusCode.NotFound, jsonHeaders)
        }
        val settings = fakeSettings()
        val client = createSupabaseClient("https://example.invalid", "test-key") {
            install(Postgrest)
            install(Auth) {
                alwaysAutoRefresh = false
                autoLoadFromStorage = false
                enableLifecycleCallbacks = false
                sessionManager = com.semtinazizi.kitapduy.data.AppSessionManager(settings)
                codeVerifierCache = io.github.jan.supabase.auth.MemoryCodeVerifierCache()
            }
            httpEngine = engine
        }
        try {
            client.auth.importSession(
                UserSession(
                    accessToken = "token",
                    refreshToken = "refresh",
                    expiresIn = 3600,
                    tokenType = "bearer",
                    user = UserInfo(
                        id = "apple-uid-999",
                        aud = "authenticated",
                        email = "privaterelay@appleid.com",
                        userMetadata = buildJsonObject {
                            put("full_name", "Apple Okuru")
                        }
                    )
                )
            )
            val repo = SupabaseAuthRepository(client, settings)
            val initial = repo.getUserProfile("apple-uid-999")
            assertNotNull(initial)
            assertEquals("Apple Okuru", initial.name)

            assertTrue(repo.updateProfile("apple-uid-999", "Yeni İsim", "Kitap kurdu", "yeni_okur"))
            assertTrue(repo.updateProfilePhoto("apple-uid-999", "https://example.com/profil fotoğrafı.jpg"))
            assertTrue(repo.updateCoverPhoto("apple-uid-999", "https://example.com/kapak resmi.jpg"))
            assertFalse(repo.updateCoverPhoto("different-uid", "https://example.com/other.jpg"))

            val updated = repo.getUserProfile("apple-uid-999")
            assertNotNull(updated)
            assertEquals("Yeni İsim", updated.name)
            assertEquals("Kitap kurdu", updated.bio)
            assertEquals("yeni_okur", updated.username)
            assertEquals("https://example.com/profil%20foto%C4%9Fraf%C4%B1.jpg", updated.photoUrl)
            assertEquals("https://example.com/kapak%20resmi.jpg", updated.coverPhotoUrl)
        } finally {
            client.close()
        }
    }

    @Test
    fun getUserProfileForOtherUserFallsBackToPostsAuthorWhenUsersTableIsMissing() = runTest {
        val postsJson = """[{"id":1,"user_id":"author-42","user_name":"Sabahattin Okur","user_photo":"https://example.com/avatar/ışık.jpg","is_premium":true}]"""
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("/rest/v1/users") ->
                    respond(pgrst205Body, HttpStatusCode.NotFound, jsonHeaders)
                request.url.encodedPath.endsWith("/rest/v1/posts") ->
                    respond(postsJson, HttpStatusCode.OK, jsonHeaders)
                else ->
                    respond("[]", HttpStatusCode.OK, jsonHeaders)
            }
        }
        val settings = fakeSettings()
        val client = createSupabaseClient("https://example.invalid", "test-key") {
            install(Postgrest)
            install(Auth) {
                alwaysAutoRefresh = false
                autoLoadFromStorage = false
                enableLifecycleCallbacks = false
                sessionManager = com.semtinazizi.kitapduy.data.AppSessionManager(settings)
                codeVerifierCache = io.github.jan.supabase.auth.MemoryCodeVerifierCache()
            }
            httpEngine = engine
        }
        try {
            val repo = SupabaseAuthRepository(client, settings)
            val profile = repo.getUserProfile("author-42")
            assertNotNull(profile)
            assertEquals("author-42", profile.uid)
            assertEquals("Sabahattin Okur", profile.name)
            assertEquals("https://example.com/avatar/%C4%B1%C5%9F%C4%B1k.jpg", profile.photoUrl)
            assertTrue(profile.isPremium)
            assertNull(repo.getUserProfile("   "))
        } finally {
            client.close()
        }
    }

    @Test
    fun reportPostAndBlockUserSucceedLocallyWhenRemoteTablesReturn404() = runTest {
        val engine = MockEngine {
            respond(
                """{"code":"PGRST205","message":"Could not find the table in the schema cache"}""",
                HttpStatusCode.NotFound,
                jsonHeaders
            )
        }
        val settings = fakeSettings()
        val client = createSupabaseClient("https://example.invalid", "test-key") {
            install(Postgrest)
            install(Auth) {
                alwaysAutoRefresh = false
                autoLoadFromStorage = false
                enableLifecycleCallbacks = false
                sessionManager = com.semtinazizi.kitapduy.data.AppSessionManager(settings)
                codeVerifierCache = io.github.jan.supabase.auth.MemoryCodeVerifierCache()
            }
            httpEngine = engine
        }
        try {
            val authRepo = SupabaseAuthRepository(client, settings)
            val socialRepo = SupabaseSocialRepository(client, authRepo, settings)

            assertFalse(socialRepo.reportPost("", "me", "Spam"))
            assertFalse(socialRepo.reportPost("10", "", "Spam"))
            assertFalse(socialRepo.reportPost("10", "me", "   "))
            assertTrue(socialRepo.reportPost("10", "me", "Spam içerik"))

            assertFalse(socialRepo.blockUser("", "spammer-1"))
            assertFalse(socialRepo.blockUser("me", ""))
            assertFalse(socialRepo.blockUser("me", "me"))
            assertTrue(socialRepo.blockUser("me", "spammer-1"))
            assertTrue(socialRepo.blockUser("me", "spammer-2"))

            val blocked = authRepo.getBlockedUsers("me")
            assertEquals(listOf("spammer-1", "spammer-2"), blocked)
        } finally {
            client.close()
        }
    }
}
