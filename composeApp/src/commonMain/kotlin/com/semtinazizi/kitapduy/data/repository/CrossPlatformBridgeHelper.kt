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
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
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

    private fun isLocalFixture(): Boolean {
        val base = AppConfig.supabaseUrl.trim()
        return base.startsWith("http://localhost", ignoreCase = true) ||
            base.startsWith("http://127.0.0.1", ignoreCase = true)
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

    suspend fun fetchBridgeUserProfile(httpClient: HttpClient?, userId: String): UserProfile? {
        if (isLocalFixture() || httpClient == null || userId.isBlank()) return null
        return try {
            val raw = httpClient.get("$BRIDGE_URL?action=user&uid=$userId").bodyAsText()
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
}
