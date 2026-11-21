package com.semtinazizi.kitapduy.data.repository

import com.semtinazizi.kitapduy.data.model.QuizLeaderboardEntry
import com.semtinazizi.kitapduy.model.SupabaseInsertNotification
import com.semtinazizi.kitapduy.model.UserProfile
import com.semtinazizi.kitapduy.platform.AppConfig
import com.semtinazizi.kitapduy.util.UrlSanitizer
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Count
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal object CrossPlatformBridgeHelper {
    private const val PUSH_SOCIAL_URL =
        "https://us-central1-kitapduy.cloudfunctions.net/pushSocialNotificationToIos"
    private const val BRIDGE_URL =
        "https://us-central1-kitapduy.cloudfunctions.net/getCrossPlatformBridge"

    private val UUID_REGEX =
        Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class BridgeUserDto(
        val uid: String = "",
        val name: String = "KitapDuy Okuru",
        val username: String? = null,
        val bio: String = "",
        @kotlinx.serialization.SerialName("photo_url") val photoUrl: String? = null,
        @kotlinx.serialization.SerialName("cover_photo_url") val coverPhotoUrl: String? = null,
        val badges: List<String> = emptyList(),
        @kotlinx.serialization.SerialName("premium_until") val premiumUntil: Long = 0L,
        @kotlinx.serialization.SerialName("is_premium") val isPremium: Boolean = false,
        val streak: Int = 0
    )

    fun isLocalFixture(): Boolean {
        val base = AppConfig.supabaseUrl.trim()
        return base.startsWith("http://localhost", ignoreCase = true) ||
            base.startsWith("http://127.0.0.1", ignoreCase = true)
    }

    suspend fun fetchAskidaAvailable(httpClient: HttpClient?): List<com.semtinazizi.kitapduy.model.AskidaKitap> {
        if (isLocalFixture() || httpClient == null) return emptyList()
        return try {
            val raw = httpClient.get("$BRIDGE_URL?action=askida_list").bodyAsText()
            json.decodeFromString<List<com.semtinazizi.kitapduy.model.AskidaKitap>>(raw)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            emptyList()
        }
    }

    suspend fun fetchAskidaMyChats(httpClient: HttpClient?, uid: String): List<com.semtinazizi.kitapduy.model.AskidaKitap> {
        val cleanUid = uid.trim()
        if (isLocalFixture() || httpClient == null || cleanUid.isBlank()) return emptyList()
        return try {
            val response = httpClient.get(BRIDGE_URL) {
                parameter("action", "askida_my_chats")
                parameter("uid", cleanUid)
            }
            if (!response.status.isSuccess()) return emptyList()
            val raw = response.bodyAsText()
            json.decodeFromString<List<com.semtinazizi.kitapduy.model.AskidaKitap>>(raw)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            emptyList()
        }
    }

    suspend fun donateAskida(httpClient: HttpClient?, book: com.semtinazizi.kitapduy.model.AskidaKitap): Boolean {
        if (isLocalFixture() || httpClient == null) return false
        return try {
            val response = httpClient.post("$BRIDGE_URL?action=askida_donate") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(com.semtinazizi.kitapduy.model.AskidaKitap.serializer(), book))
            }
            response.status.isSuccess()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun claimAskida(httpClient: HttpClient?, bookId: String, claimerId: String): Boolean {
        if (isLocalFixture() || httpClient == null || bookId.isBlank() || claimerId.isBlank()) return false
        return try {
            val payload = buildJsonObject {
                put("id", bookId)
                put("claimer_id", claimerId)
            }.toString()
            val response = httpClient.post("$BRIDGE_URL?action=askida_claim") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            response.status.isSuccess()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun fetchAskidaMessages(httpClient: HttpClient?, bookId: String): List<com.semtinazizi.kitapduy.model.AskidaMesaj> {
        val cleanBookId = bookId.trim()
        if (isLocalFixture() || httpClient == null || cleanBookId.isBlank()) return emptyList()
        return try {
            val response = httpClient.get(BRIDGE_URL) {
                parameter("action", "askida_messages")
                parameter("bookId", cleanBookId)
            }
            if (!response.status.isSuccess()) return emptyList()
            val raw = response.bodyAsText()
            json.decodeFromString<List<com.semtinazizi.kitapduy.model.AskidaMesaj>>(raw)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            emptyList()
        }
    }

    suspend fun sendAskidaMessage(httpClient: HttpClient?, msg: com.semtinazizi.kitapduy.model.AskidaMesaj): Boolean {
        if (isLocalFixture() || httpClient == null) return false
        return try {
            val response = httpClient.post("$BRIDGE_URL?action=askida_send_message") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(com.semtinazizi.kitapduy.model.AskidaMesaj.serializer(), msg))
            }
            response.status.isSuccess()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            false
        }
    }

    fun deterministicArenaUuid(rawId: String): String {
        val clean = rawId.trim()
        if (UUID_REGEX.matches(clean)) return clean.lowercase()
        val seed = "kitapduy-arena:$clean"
        val words = IntArray(4)
        for (lane in 0 until 4) {
            var hash = (0x811c9dc5.toInt()) xor (lane * 0x9e3779b9.toInt())
            for (ch in seed) {
                hash = hash xor (ch.code + lane * 31)
                hash *= 0x01000193
            }
            words[lane] = hash
        }
        val b = ByteArray(16)
        for (i in 0 until 4) {
            val w = words[i]
            b[i * 4] = ((w ushr 24) and 0xFF).toByte()
            b[i * 4 + 1] = ((w ushr 16) and 0xFF).toByte()
            b[i * 4 + 2] = ((w ushr 8) and 0xFF).toByte()
            b[i * 4 + 3] = (w and 0xFF).toByte()
        }
        b[6] = ((b[6].toInt() and 0x0F) or 0x50).toByte()
        b[8] = ((b[8].toInt() and 0x3F) or 0x80).toByte()
        val hex = b.joinToString("") { byte ->
            val v = byte.toInt() and 0xFF
            val hi = (v ushr 4).toString(16)
            val lo = (v and 0x0F).toString(16)
            "$hi$lo"
        }
        return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20, 32)}"
    }

    suspend fun sendSocialNotification(
        supabaseClient: SupabaseClient,
        httpClient: HttpClient?,
        receiverId: String,
        senderId: String,
        senderName: String,
        type: String,
        message: String,
        targetId: String? = null
    ) {
        val cleanReceiver = receiverId.trim()
        if (cleanReceiver.isBlank() || cleanReceiver == senderId.trim()) return
        val normalizedType = type.trim().lowercase()
        try {
            supabaseClient.postgrest["notifications"].insert(
                SupabaseInsertNotification(
                    receiverId = cleanReceiver,
                    message = message,
                    bookId = targetId?.takeIf { it.isNotBlank() },
                    type = normalizedType,
                    isRead = false
                )
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
        }

        if (!isLocalFixture() && httpClient != null) {
            try {
                val payload = buildJsonObject {
                    put("receiverId", cleanReceiver)
                    put("senderName", senderName.ifBlank { "Bir okur" })
                    put("type", normalizedType.uppercase())
                    put("title", notificationTitleForType(normalizedType))
                    put("body", message)
                    put("postId", targetId.orEmpty())
                }.toString()
                httpClient.post(PUSH_SOCIAL_URL) {
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
            }
        }
    }

    internal fun notificationTitleForType(type: String): String = when (type.trim().lowercase()) {
        "like" -> "❤️ Yeni Beğeni"
        "comment" -> "💬 Yeni Yorum"
        "reply" -> "💬 Yorumuna Yanıt"
        "follow" -> "👋 Yeni Takipçi"
        "poll_vote" -> "📊 Ankete Yeni Oy"
        else -> "📚 KitapDuy Bildirimi"
    }

    suspend fun unreadNotificationCount(supabaseClient: SupabaseClient, userId: String): Int {
        if (userId.isBlank()) return 0
        return try {
            val count = supabaseClient.postgrest["notifications"].select {
                filter {
                    eq("receiver_id", userId)
                    eq("is_read", false)
                }
                count(Count.EXACT)
                head = true
            }.countOrNull()?.toInt()
            count ?: 0
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            0
        }
    }

    suspend fun fetchBridgeLeaderboard(httpClient: HttpClient?): List<QuizLeaderboardEntry> {
        if (isLocalFixture() || httpClient == null) return emptyList()
        return try {
            val raw = httpClient.get("$BRIDGE_URL?action=leaderboard").bodyAsText()
            json.decodeFromString<List<QuizLeaderboardEntry>>(raw)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            emptyList()
        }
    }

    suspend fun fetchBridgeUserProfile(
        httpClient: HttpClient?,
        userId: String,
        useLocalFixture: Boolean = isLocalFixture()
    ): UserProfile? {
        val cleanUid = userId.trim()
        if (useLocalFixture || httpClient == null || cleanUid.isBlank()) return null
        return try {
            val response = httpClient.get(BRIDGE_URL) {
                parameter("action", "user")
                parameter("uid", cleanUid)
            }
            if (!response.status.isSuccess()) return null
            val raw = response.bodyAsText()
            val dto = json.decodeFromString<BridgeUserDto>(raw)
            if (dto.uid.isBlank()) return null
            UserProfile(
                uid = dto.uid,
                name = dto.name.ifBlank { "KitapDuy Okuru" },
                username = dto.username,
                bio = dto.bio,
                photoUrl = UrlSanitizer.normalizeMediaUrl(dto.photoUrl, UrlSanitizer.CUSTOM_R2_BASE_URL),
                coverPhotoUrl = UrlSanitizer.normalizeMediaUrl(dto.coverPhotoUrl, UrlSanitizer.CUSTOM_R2_BASE_URL),
                badges = dto.badges,
                premiumUntil = dto.premiumUntil,
                isPremium = dto.isPremium,
                streak = dto.streak
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            null
        }
    }

    @Serializable
    private data class SupabaseBookRequestInsert(
        @kotlinx.serialization.SerialName("user_id") val userId: String,
        @kotlinx.serialization.SerialName("user_name") val userName: String,
        @kotlinx.serialization.SerialName("book_title") val bookTitle: String,
        val author: String = "",
        val status: String = "pending"
    )

    @Serializable
    private data class BridgeBookRequestResponse(
        val ok: Boolean = false,
        val status: String? = null,
        val error: String? = null
    )

    internal data class BookRequestSubmitResult(
        val success: Boolean,
        val alreadyRequested: Boolean = false,
        val errorMessage: String? = null
    )

    suspend fun searchBridgeUsers(
        httpClient: HttpClient?,
        query: String,
        useLocalFixture: Boolean = isLocalFixture()
    ): List<UserProfile> {
        val cleanQuery = query.trim()
        if (useLocalFixture || httpClient == null || cleanQuery.length < 2) return emptyList()
        return try {
            val response = httpClient.get(BRIDGE_URL) {
                parameter("action", "search_users")
                parameter("q", cleanQuery)
            }
            if (!response.status.isSuccess()) return emptyList()
            val raw = response.bodyAsText()
            json.decodeFromString<List<BridgeUserDto>>(raw)
                .filter { it.uid.isNotBlank() }
                .map { dto ->
                    UserProfile(
                        uid = dto.uid,
                        name = dto.name.ifBlank { "KitapDuy Okuru" },
                        username = dto.username,
                        bio = dto.bio,
                        photoUrl = UrlSanitizer.normalizeMediaUrl(dto.photoUrl, UrlSanitizer.CUSTOM_R2_BASE_URL),
                        coverPhotoUrl = UrlSanitizer.normalizeMediaUrl(dto.coverPhotoUrl, UrlSanitizer.CUSTOM_R2_BASE_URL),
                        badges = dto.badges,
                        premiumUntil = dto.premiumUntil,
                        isPremium = dto.isPremium,
                        streak = dto.streak
                    )
                }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            emptyList()
        }
    }

    suspend fun submitBookRequestDetailed(
        httpClient: HttpClient?,
        supabaseClient: SupabaseClient? = null,
        userId: String?,
        userName: String?,
        bookTitle: String,
        author: String,
        useLocalFixture: Boolean = isLocalFixture()
    ): BookRequestSubmitResult {
        val cleanTitle = bookTitle.trim()
        if (cleanTitle.length !in 2..160) {
            return BookRequestSubmitResult(
                success = false,
                errorMessage = "Kitap adı 2 ile 160 karakter arasında olmalıdır."
            )
        }
        val cleanAuthor = author.trim()
        if (cleanAuthor.length > 100) {
            return BookRequestSubmitResult(
                success = false,
                errorMessage = "Yazar adı en fazla 100 karakter olabilir."
            )
        }
        val cleanUserId = userId?.trim()?.takeIf { it.isNotBlank() } ?: "anonymous"
        val cleanUserName = userName?.trim()?.takeIf { it.isNotBlank() } ?: "KitapDuy Okuru"
        return try {
            if (useLocalFixture) {
                val client = supabaseClient
                    ?: return BookRequestSubmitResult(success = false, errorMessage = "Bağlantı kurulamadı.")
                client.postgrest["book_requests"].insert(
                    SupabaseBookRequestInsert(
                        userId = cleanUserId,
                        userName = cleanUserName,
                        bookTitle = cleanTitle,
                        author = cleanAuthor,
                        status = "pending"
                    )
                )
                BookRequestSubmitResult(success = true)
            } else {
                val client = httpClient
                    ?: return BookRequestSubmitResult(success = false, errorMessage = "Bağlantı kurulamadı.")
                val payload = buildJsonObject {
                    put("user_id", cleanUserId)
                    put("user_name", cleanUserName)
                    put("title", cleanTitle)
                    put("author", cleanAuthor)
                }.toString()
                val response = client.post("$BRIDGE_URL?action=submit_book_request") {
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }
                val parsed = runCatching {
                    json.decodeFromString<BridgeBookRequestResponse>(response.bodyAsText())
                }.getOrNull()
                if (response.status.isSuccess()) {
                    BookRequestSubmitResult(
                        success = true,
                        alreadyRequested = parsed?.status == "already_requested"
                    )
                } else {
                    val fallbackError = if (response.status.value == 429) {
                        "Bugün en fazla 5 kitap talep edebilirsiniz."
                    } else {
                        "Talebiniz gönderilemedi. Lütfen tekrar deneyin."
                    }
                    BookRequestSubmitResult(
                        success = false,
                        errorMessage = parsed?.error?.takeIf { it.isNotBlank() && !it.contains('_') } ?: fallbackError
                    )
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            BookRequestSubmitResult(
                success = false,
                errorMessage = "Talebiniz gönderilemedi. Bağlantınızı kontrol edip tekrar deneyin."
            )
        }
    }

    suspend fun submitBookRequest(
        httpClient: HttpClient?,
        supabaseClient: SupabaseClient? = null,
        userId: String?,
        userName: String?,
        bookTitle: String,
        author: String,
        useLocalFixture: Boolean = isLocalFixture()
    ): Boolean = submitBookRequestDetailed(
        httpClient = httpClient,
        supabaseClient = supabaseClient,
        userId = userId,
        userName = userName,
        bookTitle = bookTitle,
        author = author,
        useLocalFixture = useLocalFixture
    ).success

    suspend fun syncUserProfileToFirestore(
        httpClient: HttpClient?,
        uid: String,
        name: String? = null,
        username: String? = null,
        bio: String? = null,
        photoUrl: String? = null,
        coverPhotoUrl: String? = null,
        includePhotoUrl: Boolean = false,
        includeCoverPhotoUrl: Boolean = false
    ) {
        val cleanUid = uid.trim()
        if (isLocalFixture() || httpClient == null || cleanUid.isBlank()) return
        try {
            val payload = buildJsonObject {
                put("uid", cleanUid)
                if (name != null) put("name", name)
                if (username != null) put("username", username)
                if (bio != null) put("bio", bio)
                if (includePhotoUrl) put("photoUrl", photoUrl.orEmpty())
                if (includeCoverPhotoUrl) put("coverPhotoUrl", coverPhotoUrl.orEmpty())
            }.toString()
            httpClient.post("$BRIDGE_URL?action=sync_user_profile") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
        }
    }
}
