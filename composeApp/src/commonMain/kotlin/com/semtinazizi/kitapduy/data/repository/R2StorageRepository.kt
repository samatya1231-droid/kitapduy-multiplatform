package com.semtinazizi.kitapduy.data.repository

import com.semtinazizi.kitapduy.data.SupabaseManager
import com.semtinazizi.kitapduy.platform.AppConfig
import com.semtinazizi.kitapduy.util.UrlSanitizer
import io.github.jan.supabase.auth.auth
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface StorageRepository {
    suspend fun uploadProfilePicture(userId: String, bytes: ByteArray, extension: String): Result<String>
    suspend fun uploadImage(folder: String, fileNamePrefix: String, bytes: ByteArray, extension: String): Result<String>
}

@Serializable
private data class R2TicketRequest(
    val purpose: String,
    val contentType: String,
    val sizeBytes: Long
)

@Serializable
internal data class R2UploadTicketResponse(
    val uploadUrl: String = "",
    val publicUrl: String = "",
    val contentType: String = "",
    val sizeBytes: Long = 0L,
    val error: String? = null
)

class R2StorageRepository(
    private val client: HttpClient,
    private val ticketEndpointProvider: () -> String = { AppConfig.r2UploadTicketUrl },
    private val accessTokenProvider: () -> String? = {
        runCatching { SupabaseManager.client.auth.currentAccessTokenOrNull() }.getOrNull()
    },
    private val apiKeyProvider: () -> String = { AppConfig.supabaseKey }
) : StorageRepository {

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        private const val MAX_IMAGE_BYTES = 5L * 1024L * 1024L

        internal fun resolvePurpose(folder: String): String = when (folder.trim().lowercase()) {
            "profile", "profiles", "profile_pictures" -> "profile"
            "profile-cover", "cover_photos", "covers", "user_covers" -> "profile-cover"
            "askida", "askida_kitap", "askida-image" -> "askida-image"
            "feed_images", "social_feed", "posts", "feed-image" -> "feed-image"
            else -> "feed-image"
        }

        internal fun resolveContentType(extension: String): String = when (extension.trim().lowercase().removePrefix(".")) {
            "", "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> throw IllegalArgumentException("Sadece JPEG, PNG veya WebP görsel yüklenebilir.")
        }

        internal fun parseAndValidateTicket(
            body: String,
            expectedContentType: String,
            expectedSizeBytes: Long
        ): R2UploadTicketResponse {
            val ticket = json.decodeFromString<R2UploadTicketResponse>(body)
            val uploadUri = ticket.uploadUrl.trim()
            val publicUri = ticket.publicUrl.trim()
            val isLocalTestUpload = uploadUri.startsWith("http://127.0.0.1") || uploadUri.startsWith("http://localhost")
            require(isLocalTestUpload || uploadUri.startsWith("https://")) {
                "Geçersiz yükleme adresi döndü."
            }
            require(
                isLocalTestUpload ||
                    (publicUri.startsWith("https://") && !publicUri.contains("?"))
            ) {
                "Geçersiz görsel adresi döndü."
            }
            require(ticket.contentType == expectedContentType && ticket.sizeBytes == expectedSizeBytes) {
                "Yükleme bileti doğrulaması başarısız."
            }
            return ticket.copy(
                uploadUrl = uploadUri,
                publicUrl = UrlSanitizer.normalizeMediaUrl(publicUri)
            )
        }
    }

    override suspend fun uploadProfilePicture(userId: String, bytes: ByteArray, extension: String): Result<String> =
        uploadWithPurpose("profile", bytes, extension)

    override suspend fun uploadImage(
        folder: String,
        fileNamePrefix: String,
        bytes: ByteArray,
        extension: String
    ): Result<String> = uploadWithPurpose(resolvePurpose(folder), bytes, extension)

    private suspend fun uploadWithPurpose(purpose: String, bytes: ByteArray, extension: String): Result<String> =
        runCatching {
            require(bytes.isNotEmpty()) { "Boş dosya yüklenemez." }
            require(bytes.size.toLong() <= MAX_IMAGE_BYTES) { "Görsel boyutu en fazla 5 MB olabilir." }

            val contentType = resolveContentType(extension)
            val accessToken = accessTokenProvider()?.trim().orEmpty()
            require(accessToken.isNotBlank()) { "Görsel yüklemek için lütfen giriş yapın." }

            val ticketUrl = ticketEndpointProvider().trim()
            val isLoopbackTicket = ticketUrl.startsWith("http://localhost") || ticketUrl.startsWith("http://127.0.0.1")
            require(isLoopbackTicket || ticketUrl.startsWith("https://")) {
                "Yükleme servisi adresi yapılandırılmamış."
            }

            val ticketResponse = client.post(ticketUrl) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                val apiKey = apiKeyProvider().trim()
                if (apiKey.isNotBlank()) {
                    header("apikey", apiKey)
                }
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(R2TicketRequest(purpose, contentType, bytes.size.toLong())))
            }
            val ticketBody = ticketResponse.bodyAsText()
            if (!ticketResponse.status.isSuccess()) {
                val parsedError = runCatching { json.decodeFromString<R2UploadTicketResponse>(ticketBody).error }.getOrNull()
                throw IllegalStateException(
                    parsedError?.takeIf { it.isNotBlank() }
                        ?: "Yükleme izni alınamadı (${ticketResponse.status.value})."
                )
            }

            val ticket = parseAndValidateTicket(ticketBody, contentType, bytes.size.toLong())

            val uploadResponse = client.put(ticket.uploadUrl) {
                header(HttpHeaders.ContentType, contentType)
                setBody(bytes)
            }
            if (!uploadResponse.status.isSuccess()) {
                throw IllegalStateException("Görsel yüklemesi başarısız (${uploadResponse.status.value}).")
            }

            ticket.publicUrl
        }
}
