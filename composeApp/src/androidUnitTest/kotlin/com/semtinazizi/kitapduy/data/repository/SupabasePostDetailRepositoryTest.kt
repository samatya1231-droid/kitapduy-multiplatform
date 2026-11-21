package com.semtinazizi.kitapduy.data.repository

import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.model.UserProfile
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*

class SupabasePostDetailRepositoryTest {
    private val auth = object : AuthRepository by DummyAuthRepository() {
        override val currentUserUid = "session-owner"
        override suspend fun getUserProfile(uid: String) = UserProfile(uid = uid, name = "Oturumdaki kullanıcı")
    }
    private val row = """[{"id":9,"post_id":1,"user_id":"session-owner","user_name":"Oturumdaki kullanıcı","user_photo":null,"comment_text":"Merhaba"}]"""
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @Test fun insertUsesSessionAuthorAndCounterFailureDoesNotTurnSuccessIntoRetry() = runTest {
        var writes = 0
        val engine = MockEngine { request ->
            if (request.method == HttpMethod.Post) {
                writes++
                val body = Json.parseToJsonElement(request.body.toByteArray().decodeToString())
                val document = if (body is JsonArray) body.single().jsonObject else body.jsonObject
                assertEquals("session-owner", document.getValue("user_id").jsonPrimitive.content)
                assertEquals("Oturumdaki kullanıcı", document.getValue("user_name").jsonPrimitive.content)
                assertEquals("Merhaba", document.getValue("comment_text").jsonPrimitive.content)
                respond(row, HttpStatusCode.Created, jsonHeaders)
            } else respond("""{"message":"Counter unavailable"}""", HttpStatusCode.ServiceUnavailable, jsonHeaders)
        }
        val client = createSupabaseClient("https://example.invalid", "test-key") { install(Postgrest); httpEngine = engine }
        try {
            val saved = SupabasePostDetailRepository(client, auth).addComment("1", "  Merhaba  ")
            assertEquals("9", saved.id)
            assertEquals(1, writes)
        } finally { client.close() }
    }

    @Test fun editingAndDeletionFilterByOwnerAndPostAndRejectNoMatchedRows() = runTest {
        var requests = 0
        val engine = MockEngine { request ->
            requests++
            assertEquals("eq.session-owner", request.url.parameters["user_id"])
            assertEquals("eq.1", request.url.parameters["post_id"])
            assertEquals("eq.9", request.url.parameters["id"])
            respond("[]", HttpStatusCode.OK, jsonHeaders)
        }
        val client = createSupabaseClient("https://example.invalid", "test-key") { install(Postgrest); httpEngine = engine }
        try {
            val repository = SupabasePostDetailRepository(client, auth)
            assertFailsWith<IllegalStateException> { repository.editComment("1", "9", "Yeni") }
            assertFailsWith<IllegalStateException> { repository.deleteComment("1", "9") }
            assertEquals(2, requests)
        } finally { client.close() }
    }

    @Test fun blankOrOversizedCommentsNeverReachTheServer() = runTest {
        val engine = MockEngine { error("Network should not be called") }
        val client = createSupabaseClient("https://example.invalid", "test-key") { install(Postgrest); httpEngine = engine }
        try {
            val repository = SupabasePostDetailRepository(client, auth)
            assertFailsWith<IllegalArgumentException> { repository.addComment("1", "   ") }
            assertFailsWith<IllegalArgumentException> { repository.addComment("1", "a".repeat(2001)) }
        } finally { client.close() }
    }

    @Test fun nonNumericAndroidBookIdReturnsNullOrEmptyWithoutNumberFormatException() = runTest {
        val engine = MockEngine { error("Network should not be called for non-numeric post ID") }
        val client = createSupabaseClient("https://example.invalid", "test-key") { install(Postgrest); httpEngine = engine }
        try {
            val repository = SupabasePostDetailRepository(client, auth)
            assertNull(repository.getPost("0mVxipNVFmBxtJNs9O0J"))
            assertEquals(emptyList(), repository.getComments("0mVxipNVFmBxtJNs9O0J"))
            assertFailsWith<IllegalArgumentException> { repository.addComment("0mVxipNVFmBxtJNs9O0J", "Test") }
        } finally { client.close() }
    }
}
