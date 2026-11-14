package com.semtinazizi.kitapduy.data.repository

import com.semtinazizi.kitapduy.domain.repository.BookRepository
import com.semtinazizi.kitapduy.model.Book
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import com.semtinazizi.kitapduy.platform.AppConfig
import com.semtinazizi.kitapduy.util.UrlSanitizer

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
    @SerialName("like_count") val likeCount: Int? = null,
    val language: String? = null,
    val tags: List<String>? = null
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
        likeCount = likeCount ?: 0,
        language = language ?: "tr",
        tags = tags ?: emptyList()
    )
}

class SupabaseBookRepository(private val supabaseClient: SupabaseClient) : BookRepository {

    private fun fixUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""
        val resolved = if (url.startsWith("http")) url else "${AppConfig.cfPublicUrl.trimEnd('/')}/${url.trimStart('/')}"
        return UrlSanitizer.sanitizeUrl(resolved)
    }

    private fun fixImageUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""

        val resolved = if (url.contains("r2.cloudflarestorage.com")) {
            AppConfig.generatePresignedUrl(url)
        } else if (url.startsWith("http")) {
            url
        } else {
            "${AppConfig.cfPublicUrl.trimEnd('/')}/${url.trimStart('/')}"
        }
        return UrlSanitizer.sanitizeUrl(resolved)
    }

    companion object {
        val sampleFallbackBooks = listOf(
            Book(
                id = "ci_sample_1",
                title = "Kürk Mantolu Madonna",
                author = "Sabahattin Ali",
                category = "Klasik",
                coverUrl = "https://images.unsplash.com/photo-1544947950-fa07a98d237f?q=80&w=600",
                audioUrl = "https://ancient-shape-df97.samatya231.workers.dev/Birdenbire.MP3",
                description = "Sabahattin Ali'nin ölümsüz eseri Kürk Mantolu Madonna.",
                duration = 180000L
            ),
            Book(
                id = "ci_sample_2",
                title = "Kızıl Dosya",
                author = "Arthur Conan Doyle",
                category = "Polisiye",
                coverUrl = "https://images.unsplash.com/photo-1589829085413-56de8ae18c73?w=400&q=80",
                audioUrl = "https://ancient-shape-df97.samatya231.workers.dev/kizildosya.MP3",
                description = "Sherlock Holmes'un ilk macerası.",
                duration = 240000L
            ),
            Book(
                id = "ci_sample_3",
                title = "Dr. Jekyll ve Mr. Hyde",
                author = "Robert Louis Stevenson",
                category = "Klasik",
                coverUrl = "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=400&q=80",
                audioUrl = "https://ancient-shape-df97.samatya231.workers.dev/drjeky.MP3",
                description = "İnsan doğasının iki zıt kutbu.",
                duration = 210000L
            )
        )
    }

    override suspend fun getBooks(): List<Book> {
        val isLocalFixture = AppConfig.supabaseUrl.startsWith("http://localhost:")
        return try {
            val books = supabaseClient.postgrest["books"]
                .select()
                .decodeList<BookDto>().map { it.toBook() }

            val remote = books.filter { it.title.isNotBlank() && it.author.isNotBlank() }.map {
                it.copy(
                    coverUrl = fixImageUrl(it.coverUrl),
                    audioUrl = fixUrl(it.audioUrl)
                )
            }
            if (isLocalFixture) {
                if (remote.isNotEmpty()) remote else CatalogSeed.books
            } else {
                mergeWithSeedCatalog(remote)
            }
        } catch (e: Throwable) {
            println("GET_BOOKS_ERROR: ${e.stackTraceToString()}")
            CatalogSeed.books.ifEmpty { sampleFallbackBooks }
        }
    }

    private fun mergeWithSeedCatalog(remote: List<Book>): List<Book> {
        val seedById = CatalogSeed.books.associateBy { it.id }
        val merged = remote.map { book ->
            val seed = seedById[book.id]
            if (seed == null) {
                book
            } else {
                book.copy(
                    category = book.category.takeIf { it.isNotBlank() && !it.equals("Genel", ignoreCase = true) } ?: seed.category,
                    coverUrl = book.coverUrl.ifBlank { fixImageUrl(seed.coverUrl) },
                    audioUrl = book.audioUrl.ifBlank { fixUrl(seed.audioUrl) },
                    description = book.description.ifBlank { seed.description },
                    duration = if (book.duration > 0L) book.duration else seed.duration,
                    viewCount = maxOf(book.viewCount, seed.viewCount),
                    likeCount = maxOf(book.likeCount, seed.likeCount),
                    tags = book.tags.ifEmpty { seed.tags }
                )
            }
        }.toMutableList()
        val seenIds = merged.map { it.id }.toHashSet()
        CatalogSeed.books.forEach { seed ->
            if (seed.id !in seenIds) {
                merged.add(
                    seed.copy(
                        coverUrl = fixImageUrl(seed.coverUrl),
                        audioUrl = fixUrl(seed.audioUrl)
                    )
                )
            }
        }
        return merged
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
        val isLocalFixture = AppConfig.supabaseUrl.startsWith("http://localhost:")
        val seedMatches = if (isLocalFixture) {
            emptyList()
        } else {
            CatalogSeed.books.filter {
                it.title.contains(query, ignoreCase = true) || it.author.contains(query, ignoreCase = true)
            }.map {
                it.copy(coverUrl = fixImageUrl(it.coverUrl), audioUrl = fixUrl(it.audioUrl))
            }
        }
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

            val filtered = books.filter { it.title.isNotBlank() && it.author.isNotBlank() }.map {
                it.copy(
                    coverUrl = fixImageUrl(it.coverUrl),
                    audioUrl = fixUrl(it.audioUrl)
                )
            }
            if (isLocalFixture) {
                filtered
            } else {
                val seen = filtered.map { it.id }.toHashSet()
                filtered + seedMatches.filter { it.id !in seen }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            if (seedMatches.isNotEmpty()) seedMatches else sampleFallbackBooks.filter {
                it.title.contains(query, ignoreCase = true) || it.author.contains(query, ignoreCase = true)
            }
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
