package com.semtinazizi.kitapduy.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

class R2StorageRepositoryTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun resolvePurposeMapsFeedAskidaProfileAndCoverFolders() {
        assertEquals("feed-image", R2StorageRepository.resolvePurpose("feed_images"))
        assertEquals("feed-image", R2StorageRepository.resolvePurpose("social_feed"))
        assertEquals("askida-image", R2StorageRepository.resolvePurpose("askida_kitap"))
        assertEquals("profile", R2StorageRepository.resolvePurpose("profile_pictures"))
        assertEquals("profile-cover", R2StorageRepository.resolvePurpose("cover_photos"))
    }

    @Test
    fun uploadImageRequestsSignedTicketWithBearerJwtUploadsBytesAndNormalizesPublicUrl() = runTest {
        val seenRequests = mutableListOf<Pair<HttpMethod, String>>()
        var capturedPurpose = ""
        var capturedAuth = ""
        var capturedPutContentType = ""
        var capturedPutBytes = ByteArray(0)

        val engine = MockEngine { request ->
            seenRequests.add(request.method to request.url.toString())
            when {
                request.url.toString() == "https://tickets.kitapduy.example/createR2UploadHttp" -> {
                    capturedAuth = request.headers[HttpHeaders.Authorization].orEmpty()
                    val reqJson = Json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
                    capturedPurpose = reqJson.getValue("purpose").jsonPrimitive.content
                    val ct = reqJson.getValue("contentType").jsonPrimitive.content
                    val sz = reqJson.getValue("sizeBytes").jsonPrimitive.long
                    respond(
                        content = """{
                          "uploadUrl":"https://r2-presigned.example/put-object?X-Amz-Signature=abc",
                          "publicUrl":"https://pub-7327d7da4dcf4c1bbf6d53bacbbe276c.r2.dev/feed_images/u1/img.webp",
                          "contentType":"$ct",
                          "sizeBytes":$sz
                        }""",
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders
                    )
                }
                request.url.toString().startsWith("https://r2-presigned.example/") -> {
                    capturedPutContentType = request.body.contentType?.toString().orEmpty()
                    if (capturedPutContentType.isBlank()) {
                        capturedPutContentType = request.headers[HttpHeaders.ContentType].orEmpty()
                    }
                    capturedPutBytes = request.body.toByteArray()
                    respond("", HttpStatusCode.OK)
                }
                else -> respond("Unexpected request", HttpStatusCode.NotFound)
            }
        }

        val http = HttpClient(engine)
        try {
            val repo = R2StorageRepository(
                client = http,
                ticketEndpointProvider = { "https://tickets.kitapduy.example/createR2UploadHttp" },
                accessTokenProvider = { "supabase-jwt-token" },
                apiKeyProvider = { "anon-key" }
            )
            val payload = byteArrayOf(1, 2, 3, 4)
            val result = repo.uploadImage("feed_images", "post_1", payload, "webp")

            assertTrue(result.isSuccess)
            assertEquals("https://ses.kitapduy.com.tr/feed_images/u1/img.webp", result.getOrThrow())
            assertEquals("Bearer supabase-jwt-token", capturedAuth)
            assertEquals("feed-image", capturedPurpose)
            assertEquals("image/webp", capturedPutContentType)
            assertEquals(payload.toList(), capturedPutBytes.toList())
            assertEquals(listOf(HttpMethod.Post, HttpMethod.Put), seenRequests.map { it.first })
        } finally {
            http.close()
        }
    }

    @Test
    fun uploadRejectsBlankTokenInvalidExtensionInsecureUrlsAndMismatchedTickets() = runTest {
        val dummyHttp = HttpClient(MockEngine {
            respond(
                """{"uploadUrl":"http://evil.invalid/put","publicUrl":"https://ses.kitapduy.com.tr/a.jpg","contentType":"image/jpeg","sizeBytes":3}""",
                HttpStatusCode.OK,
                jsonHeaders
            )
        })
        try {
            val noTokenRepo = R2StorageRepository(
                client = dummyHttp,
                ticketEndpointProvider = { "https://tickets.kitapduy.example/createR2UploadHttp" },
                accessTokenProvider = { "   " },
                apiKeyProvider = { "" }
            )
            assertTrue(noTokenRepo.uploadImage("feed_images", "p", byteArrayOf(1, 2, 3), "jpg").isFailure)

            val authedRepo = R2StorageRepository(
                client = dummyHttp,
                ticketEndpointProvider = { "https://tickets.kitapduy.example/createR2UploadHttp" },
                accessTokenProvider = { "valid-jwt" },
                apiKeyProvider = { "" }
            )
            assertTrue(authedRepo.uploadImage("feed_images", "p", ByteArray(0), "jpg").isFailure)
            assertTrue(authedRepo.uploadImage("feed_images", "p", byteArrayOf(1), "svg").isFailure)
            // Insecure uploadUrl rejected
            assertTrue(authedRepo.uploadImage("feed_images", "p", byteArrayOf(1, 2, 3), "jpg").isFailure)
        } finally {
            dummyHttp.close()
        }
    }

    @Test
    fun parseAndValidateTicketRejectsQueryParamsOnPublicUrlAndSizeMismatch() {
        val queryErr = kotlin.runCatching {
            R2StorageRepository.parseAndValidateTicket(
                body = """{"uploadUrl":"https://r2.example/put","publicUrl":"https://ses.kitapduy.com.tr/a.jpg?sig=1","contentType":"image/jpeg","sizeBytes":10}""",
                expectedContentType = "image/jpeg",
                expectedSizeBytes = 10
            )
        }
        assertTrue(queryErr.isFailure)

        val sizeErr = kotlin.runCatching {
            R2StorageRepository.parseAndValidateTicket(
                body = """{"uploadUrl":"https://r2.example/put","publicUrl":"https://ses.kitapduy.com.tr/a.jpg","contentType":"image/jpeg","sizeBytes":99}""",
                expectedContentType = "image/jpeg",
                expectedSizeBytes = 10
            )
        }
        assertTrue(sizeErr.isFailure)
    }
}
