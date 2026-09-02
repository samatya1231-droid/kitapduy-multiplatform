package com.semtinazizi.kitapduy

import org.junit.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import java.io.File

@Serializable
data class BookDtoTest(
    val id: String? = null,
    val title: String? = null,
    val author: String? = null,
    val category: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("audio_url") val audioUrl: String? = null,
    val description: String? = null,
    val duration: Long? = null,
    @SerialName("listen_count") val listenCount: Long? = null,
    @SerialName("view_count") val viewCount: Int? = null,
    @SerialName("like_count") val likeCount: Int? = null
)

class SupabaseTest {
    @Test
    fun testSerialization() {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        try {
            val file = File("../../books.json")
            if (file.exists()) {
                val data = file.readText(Charsets.UTF_16)
                val books = json.decodeFromString<List<BookDtoTest>>(data)
                println("Successfully decoded " + books.size + " books")
            } else {
                println("books.json not found")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
