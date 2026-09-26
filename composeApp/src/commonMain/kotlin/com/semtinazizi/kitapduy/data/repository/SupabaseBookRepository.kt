package com.semtinazizi.kitapduy.data.repository

import com.semtinazizi.kitapduy.domain.repository.BookRepository
import com.semtinazizi.kitapduy.model.Book
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import com.semtinazizi.kitapduy.platform.AppConfig

@Serializable
private data class BookDto(
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
) {
    fun toBook() = Book(
        id = id ?: "",
        title = title ?: "",
        author = author ?: "",
        category = category ?: "",
        coverUrl = coverUrl ?: "",
        audioUrl = audioUrl ?: "",
        description = description ?: "",
        duration = duration ?: 0L,
        listenCount = listenCount ?: 0L,
        viewCount = viewCount ?: 0,
        likeCount = likeCount ?: 0
    )
}

class SupabaseBookRepository(private val supabaseClient: SupabaseClient) : BookRepository {

    private fun fixUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""
        if (url.startsWith("http")) return url
        return "${AppConfig.cfPublicUrl.trimEnd('/')}/${url.trimStart('/')}"
    }

    private fun fixImageUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""

        if (url.contains("r2.cloudflarestorage.com")) {
            return AppConfig.generatePresignedUrl(url)
        }

        if (url.startsWith("http")) return url

        return "${AppConfig.cfPublicUrl.trimEnd('/')}/${url.trimStart('/')}"
    }

    override suspend fun getBooks(): List<Book> {
        return try {
            val books = supabaseClient.postgrest["books"]
                .select()
                .decodeList<BookDto>().map { it.toBook() }

            books.filter { it.title.isNotBlank() && it.author.isNotBlank() }.map {
                it.copy(
                    coverUrl = fixImageUrl(it.coverUrl),
                    audioUrl = fixUrl(it.audioUrl)
                )
            }
        } catch (e: Throwable) {
            println("GET_BOOKS_ERROR: ${e.stackTraceToString()}")
            listOf(Book(id = "error", title = "Hata", author = e.message ?: "Bilinmeyen Hata"))
        }
    }

    override suspend fun getFavoriteBookIds(userId: String): Set<String> {
        return try {
            // "favorites" tablosundan sadece book_id sütununu çekeriz
            // Tablo adı ve sütun adları DB şemasına göre düzenlenebilir
            val result = supabaseClient.postgrest["favorites"]
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<Map<String, String>>()

            result.mapNotNull { it["book_id"] }.toSet()
        } catch (e: Throwable) {
            e.printStackTrace()
            emptySet()
        }
    }

    override suspend fun getDailySummary(): Pair<String, String>? {
        // Gerçekte ayrı bir tablodan veya API'den gelebilir. Şimdilik sabit / yedek veri:
        return Pair("Günlük Okuma Hedefin", "Hedefine ulaşmak için bugün sadece 15 sayfa daha okuman gerekiyor!")
    }

    override suspend fun getDailyQuote(): Triple<String, String, String>? {
        return try {
            val result = supabaseClient.postgrest["daily_quotes"]
                .select { limit(1) }
                .decodeSingleOrNull<Map<String, String>>()

            if (result != null) {
                Triple(result["quote"] ?: "", result["author"] ?: "", result["book"] ?: "")
            } else null
        } catch (e: Throwable) {
            // Local Fallback
            Triple("Bir kitap okuyan her şeyi okuyabilir.", "Sabahattin Ali", "Kürk Mantolu Madonna")
        }
    }

    override suspend fun searchBooks(query: String): List<Book> {
        if (query.isBlank()) return emptyList()
        return try {
            val books = supabaseClient.postgrest["books"]
                .select {
                    filter {
                        or {
                            ilike("title", "%$query%")
                            ilike("author", "%$query%")
                        }
                    }
                }
                .decodeList<BookDto>().map { it.toBook() }

            books.filter { it.title.isNotBlank() && it.author.isNotBlank() }.map {
                it.copy(
                    coverUrl = fixImageUrl(it.coverUrl),
                    audioUrl = fixUrl(it.audioUrl)
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getPodcasts(): List<com.semtinazizi.kitapduy.model.Podcast> {
        return try {
            println("SupabaseRepository: Fetching from 'podcasts' table...")
            val response = supabaseClient.postgrest["podcasts"].select()

            val podcasts = response.decodeList<com.semtinazizi.kitapduy.model.PodcastDto>().map { it.toPodcast() }
            println("SupabaseRepository: Decoded ${podcasts.size} podcasts")

            podcasts.map { it.copy(
                imageUrl = fixImageUrl(it.imageUrl),
                rssUrl = fixUrl(it.rssUrl)
            ) }
        } catch (e: Throwable) {
            println("SupabaseRepository ERROR: ${e.message}")
            emptyList()
        }
    }

    override suspend fun searchPodcasts(query: String): List<com.semtinazizi.kitapduy.model.Podcast> {
        if (query.isBlank()) return emptyList()
        return try {
            val podcasts = supabaseClient.postgrest["podcasts"]
                .select {
                    filter {
                        or {
                            ilike("name", "%$query%")
                            ilike("author", "%$query%")
                        }
                    }
                }
                .decodeList<com.semtinazizi.kitapduy.model.PodcastDto>().map { it.toPodcast() }

            podcasts.map { it.copy(
                imageUrl = fixImageUrl(it.imageUrl),
                rssUrl = fixUrl(it.rssUrl)
            ) }
        } catch (e: Throwable) {
            emptyList()
        }
    }

    override suspend fun getBookshelf(userId: String, shelfType: String): List<com.semtinazizi.kitapduy.model.SupabaseBookshelf> {
        return supabaseClient.postgrest["user_bookshelves"]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("shelf_type", shelfType)
                    }
                }
                .decodeList<com.semtinazizi.kitapduy.model.SupabaseBookshelf>()
    }
}
