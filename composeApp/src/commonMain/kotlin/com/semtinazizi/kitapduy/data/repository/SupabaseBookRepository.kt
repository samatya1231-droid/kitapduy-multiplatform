package com.semtinazizi.kitapduy.data.repository

import com.semtinazizi.kitapduy.domain.repository.BookRepository
import com.semtinazizi.kitapduy.model.Book
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import com.semtinazizi.kitapduy.platform.AppConfig
import com.semtinazizi.kitapduy.util.UrlSanitizer
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlin.concurrent.Volatile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

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
        language = language?.trim()?.lowercase()?.ifBlank { "tr" } ?: "tr",
        tags = tags ?: emptyList()
    )
}

class SupabaseBookRepository(
    private val supabaseClient: SupabaseClient,
    private val httpClient: io.ktor.client.HttpClient = io.ktor.client.HttpClient()
) : BookRepository {

    private val booksCacheMutex = Mutex()
    private val podcastsCacheMutex = Mutex()
    private val cacheTtlMs = 5L * 60L * 1000L

    @Volatile
    private var cachedBooks: List<Book>? = null
    @Volatile
    private var cachedBooksTimestampMs: Long = 0L
    @Volatile
    private var cachedPodcasts: List<com.semtinazizi.kitapduy.model.Podcast>? = null
    @Volatile
    private var cachedPodcastsTimestampMs: Long = 0L
    @Volatile
    private var cacheGeneration: Long = 0L

    override fun invalidateCache() {
        cacheGeneration++
        cachedBooksTimestampMs = 0L
        cachedPodcastsTimestampMs = 0L
        cachedBooks = null
        cachedPodcasts = null
    }

    private fun resolvedBaseUrl(): String {
        val publicBase = AppConfig.cfPublicUrl.trim().trimEnd('/')
        return if (publicBase.startsWith("http://localhost", ignoreCase = true) ||
            publicBase.startsWith("http://127.0.0.1", ignoreCase = true)
        ) {
            publicBase
        } else {
            UrlSanitizer.CUSTOM_R2_BASE_URL
        }
    }

    private fun fixUrl(url: String?): String = UrlSanitizer.normalizeMediaUrl(url, resolvedBaseUrl())

    private fun fixImageUrl(url: String?): String = UrlSanitizer.normalizeMediaUrl(url, resolvedBaseUrl())

    private fun isValidCatalogTitle(title: String): Boolean {
        val clean = title.trim()
        if (clean.isBlank()) return false
        val normalized = clean
            .replace("\u0307", "")
            .replace('İ', 'i')
            .lowercase()
        return normalized != "isimsiz kitap" && normalized != "bilinmeyen kitap" && normalized != "untitled"
    }

    private fun normalizeCatalogBook(book: Book): Book = book.copy(
        title = book.title.trim(),
        author = book.author.trim().ifBlank { "Bilinmeyen Yazar" },
        category = book.category.trim().ifBlank { "Genel" },
        coverUrl = fixImageUrl(book.coverUrl),
        audioUrl = fixUrl(book.audioUrl),
        language = book.language.trim().lowercase().ifBlank { "tr" }
    )

    private fun isUuidId(id: String): Boolean =
        Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$").matches(id.trim())

    private fun deduplicateCatalogBooks(books: List<Book>): List<Book> {
        return books
            .distinctBy { it.id }
            .groupBy { it.title.trim().lowercase() to it.author.trim().lowercase() }
            .values
            .map { group ->
                group.sortedWith(
                    compareBy<Book> { isUuidId(it.id) }
                        .thenByDescending { it.viewCount }
                        .thenByDescending { it.likeCount }
                ).first()
            }
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
        val now = Clock.System.now().toEpochMilliseconds()
        cachedBooks?.takeIf { it.isNotEmpty() && now - cachedBooksTimestampMs < cacheTtlMs }?.let {
            return it
        }
        return booksCacheMutex.withLock {
            val nowLocked = Clock.System.now().toEpochMilliseconds()
            cachedBooks?.takeIf { it.isNotEmpty() && nowLocked - cachedBooksTimestampMs < cacheTtlMs }?.let {
                return@withLock it
            }
            val generationAtStart = cacheGeneration
            fetchBooksRemote(generationAtStart)
        }
    }

    private suspend fun fetchBooksRemote(generationAtStart: Long): List<Book> {
        return try {
            val allRemote = mutableListOf<Book>()
            val pageSize = 1000L
            var offset = 0L
            while (true) {
                val batch = supabaseClient.postgrest["books"]
                    .select {
                        order("id", order = io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                        range(offset, offset + pageSize - 1)
                    }
                    .decodeList<BookDto>()
                    .map { it.toBook() }
                allRemote.addAll(batch)
                if (batch.size.toLong() < pageSize) break
                offset += pageSize
            }

            val remote = deduplicateCatalogBooks(
                allRemote
                    .filter { isValidCatalogTitle(it.title) && (it.author.isNotBlank() || it.audioUrl.isNotBlank() || it.coverUrl.isNotBlank()) }
                    .map { normalizeCatalogBook(it) }
            )

            if (remote.isNotEmpty()) {
                if (generationAtStart == cacheGeneration) {
                    cachedBooks = remote
                    cachedBooksTimestampMs = Clock.System.now().toEpochMilliseconds()
                }
                remote
            } else {
                CatalogSeed.books.map { normalizeCatalogBook(it) }.ifEmpty { sampleFallbackBooks }
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (e: Throwable) {
            println("GET_BOOKS_ERROR: ${e.stackTraceToString()}")
            CatalogSeed.books.map { normalizeCatalogBook(it) }.ifEmpty { sampleFallbackBooks }
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
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return emptyList()
        val isLocalFixture = AppConfig.supabaseUrl.startsWith("http://localhost:")
        return try {
            val books = supabaseClient.postgrest["books"]
                .select {
                    filter {
                        or {
                            ilike("title", "%$cleanQuery%")
                            ilike("author", "%$cleanQuery%")
                            ilike("category", "%$cleanQuery%")
                        }
                    }
                }
                .decodeList<BookDto>()
                .map { it.toBook() }

            deduplicateCatalogBooks(
                books.filter {
                    isValidCatalogTitle(it.title) && (it.author.isNotBlank() || it.audioUrl.isNotBlank() || it.coverUrl.isNotBlank())
                }.map { normalizeCatalogBook(it) }
            )
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (e: Throwable) {
            e.printStackTrace()
            if (isLocalFixture) {
                emptyList()
            } else {
                val seedMatches = CatalogSeed.books.filter {
                    it.title.contains(cleanQuery, ignoreCase = true) ||
                        it.author.contains(cleanQuery, ignoreCase = true) ||
                        it.category.contains(cleanQuery, ignoreCase = true) ||
                        it.tags.any { tag -> tag.contains(cleanQuery, ignoreCase = true) }
                }.map { normalizeCatalogBook(it) }
                if (seedMatches.isNotEmpty()) {
                    seedMatches
                } else {
                    sampleFallbackBooks.filter {
                        it.title.contains(cleanQuery, ignoreCase = true) ||
                            it.author.contains(cleanQuery, ignoreCase = true)
                    }.map { normalizeCatalogBook(it) }
                }
            }
        }
    }

    override suspend fun getBookById(bookId: String): Book? {
        if (bookId.isBlank()) return null
        cachedBooks?.firstOrNull { it.id == bookId }?.let { return it }
        return try {
            supabaseClient.postgrest["books"]
                .select { filter { eq("id", bookId) } }
                .decodeList<BookDto>()
                .firstOrNull()
                ?.toBook()
                ?.takeIf { isValidCatalogTitle(it.title) }
                ?.let { normalizeCatalogBook(it) }
                ?: CatalogSeed.books.firstOrNull { it.id == bookId }?.let { normalizeCatalogBook(it) }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            CatalogSeed.books.firstOrNull { it.id == bookId }?.let { normalizeCatalogBook(it) }
        }
    }

    override suspend fun resolvePodcastEpisode(
        bookId: String,
        fallbackTitle: String,
        fallbackCover: String,
        fallbackAudioUrl: String,
        fallbackAuthor: String
    ): Pair<com.semtinazizi.kitapduy.model.PodcastEpisode, String>? {
        if (!bookId.startsWith("podcast_")) return null
        val withoutPrefix = bookId.removePrefix("podcast_")
        val podcastId = withoutPrefix.substringBefore("_EP_").trim()
        val epHash = withoutPrefix.substringAfterLast("_EP_", "").toIntOrNull()

        val normalizedFallbackAudio = fixUrl(fallbackAudioUrl)
        val normalizedFallbackCover = fixImageUrl(fallbackCover)

        val podcastIdCandidates = setOf(
            podcastId.lowercase(),
            "podcast_${podcastId.lowercase()}",
            podcastId.removePrefix("podcast_").lowercase()
        )
        val podcasts = runCatching { getPodcasts() }.getOrDefault(emptyList())
        val matchedPodcast = podcasts.firstOrNull {
            it.id.lowercase() in podcastIdCandidates ||
                (fallbackAuthor.isNotBlank() && (it.name.contains(fallbackAuthor, ignoreCase = true) || fallbackAuthor.contains(it.name, ignoreCase = true)))
        }

        if (matchedPodcast != null && matchedPodcast.rssUrl.isNotBlank()) {
            val rssXml = runCatching {
                httpClient.get(matchedPodcast.rssUrl).bodyAsText()
            }.getOrNull().orEmpty()
            val episodes = if (rssXml.isNotBlank()) {
                com.semtinazizi.kitapduy.ui.viewmodel.PodcastEpisodesViewModel.parseEpisodesXml(
                    xmlContent = rssXml,
                    podcastId = matchedPodcast.id
                )
            } else emptyList()
            val hashByNormalizedUrl = if (rssXml.isNotBlank()) {
                com.semtinazizi.kitapduy.ui.viewmodel.PodcastEpisodesViewModel.extractCandidateAudioHashes(rssXml)
            } else emptyMap()

            val matchedEp = episodes.firstOrNull { ep ->
                val normEpAudio = fixUrl(ep.audioUrl)
                val candidateHashes = hashByNormalizedUrl[normEpAudio].orEmpty() + ep.audioUrl.hashCode()
                (epHash != null && epHash in candidateHashes) ||
                    (normalizedFallbackAudio.isNotBlank() && normEpAudio == normalizedFallbackAudio) ||
                    (fallbackTitle.isNotBlank() && ep.title.equals(fallbackTitle.trim(), ignoreCase = true))
            }
            if (matchedEp != null) {
                return matchedEp.copy(
                    id = bookId,
                    audioUrl = fixUrl(matchedEp.audioUrl),
                    imageUrl = matchedEp.imageUrl?.takeIf { it.isNotBlank() }?.let { fixImageUrl(it) }
                        ?: matchedPodcast.imageUrl.takeIf { it.isNotBlank() }
                        ?: normalizedFallbackCover.ifBlank { null }
                ) to matchedPodcast.name.ifBlank { fallbackAuthor.ifBlank { "Podcast" } }
            }
        }

        if (normalizedFallbackAudio.isNotBlank()) {
            return com.semtinazizi.kitapduy.model.PodcastEpisode(
                id = bookId,
                title = fallbackTitle.ifBlank { "Podcast Bölümü" },
                description = "Podcast Bölümü",
                audioUrl = normalizedFallbackAudio,
                imageUrl = normalizedFallbackCover.ifBlank { matchedPodcast?.imageUrl }
            ) to (matchedPodcast?.name ?: fallbackAuthor.ifBlank { "Podcast" })
        }
        return null
    }

    override suspend fun getPodcasts(): List<com.semtinazizi.kitapduy.model.Podcast> {
        val now = Clock.System.now().toEpochMilliseconds()
        cachedPodcasts?.takeIf { it.isNotEmpty() && now - cachedPodcastsTimestampMs < cacheTtlMs }?.let {
            return it
        }
        return podcastsCacheMutex.withLock {
            val nowLocked = Clock.System.now().toEpochMilliseconds()
            cachedPodcasts?.takeIf { it.isNotEmpty() && nowLocked - cachedPodcastsTimestampMs < cacheTtlMs }?.let {
                return@withLock it
            }
            val generationAtStart = cacheGeneration
            try {
                println("SupabaseRepository: Fetching from 'podcasts' table...")
                val response = supabaseClient.postgrest["podcasts"].select()

                val podcasts = response.decodeList<com.semtinazizi.kitapduy.model.PodcastDto>().map { it.toPodcast() }
                println("SupabaseRepository: Decoded ${podcasts.size} podcasts")

                val normalized = podcasts.map {
                    it.copy(
                        imageUrl = fixImageUrl(it.imageUrl),
                        rssUrl = fixUrl(it.rssUrl)
                    )
                }
                if (normalized.isNotEmpty() && generationAtStart == cacheGeneration) {
                    cachedPodcasts = normalized
                    cachedPodcastsTimestampMs = Clock.System.now().toEpochMilliseconds()
                }
                normalized
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (e: Throwable) {
                println("SupabaseRepository ERROR: ${e.message}")
                emptyList()
            }
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

    @Serializable
    private data class FavoriteBookInsertDto(
        @SerialName("user_id") val userId: String,
        @SerialName("book_id") val bookId: String
    )

    override suspend fun isBookLiked(bookId: String, userId: String): Boolean {
        if (bookId.isBlank() || userId.isBlank()) return false
        val inShelf = runCatching {
            supabaseClient.postgrest["user_bookshelves"]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("book_id", bookId)
                        eq("shelf_type", "FAVORITE")
                    }
                }
                .decodeList<com.semtinazizi.kitapduy.model.SupabaseBookshelf>()
                .isNotEmpty()
        }.getOrDefault(false)
        if (inShelf) return true
        return runCatching {
            getFavoriteBookIds(userId).contains(bookId)
        }.getOrDefault(false)
    }

    override suspend fun toggleBookLike(bookId: String, userId: String): Boolean {
        require(bookId.isNotBlank() && userId.isNotBlank()) { "Beğenmek için giriş yapmalısınız." }
        val currentlyLiked = isBookLiked(bookId, userId)
        val book = runCatching { getBookById(bookId) }.getOrNull()
        val nowLiked = !currentlyLiked
        if (nowLiked) {
            runCatching {
                supabaseClient.postgrest["favorites"].insert(FavoriteBookInsertDto(userId = userId, bookId = bookId))
            }
            val existingShelf = runCatching {
                supabaseClient.postgrest["user_bookshelves"]
                    .select {
                        filter {
                            eq("user_id", userId)
                            eq("book_id", bookId)
                            eq("shelf_type", "FAVORITE")
                        }
                    }
                    .decodeList<com.semtinazizi.kitapduy.model.SupabaseBookshelf>()
            }.getOrDefault(emptyList())
            if (existingShelf.isEmpty()) {
                supabaseClient.postgrest["user_bookshelves"].insert(
                    com.semtinazizi.kitapduy.model.SupabaseBookshelf(
                        userId = userId,
                        bookId = bookId,
                        shelfType = "FAVORITE",
                        bookTitle = book?.title?.takeIf { it.isNotBlank() },
                        bookCoverUrl = book?.coverUrl?.takeIf { it.isNotBlank() }
                    )
                )
            }
        } else {
            runCatching {
                supabaseClient.postgrest["favorites"].delete {
                    filter {
                        eq("user_id", userId)
                        eq("book_id", bookId)
                    }
                }
            }
            supabaseClient.postgrest["user_bookshelves"].delete {
                filter {
                    eq("user_id", userId)
                    eq("book_id", bookId)
                    eq("shelf_type", "FAVORITE")
                }
            }
        }
        val currentCount = book?.likeCount ?: 0
        val nextCount = if (nowLiked) currentCount + 1 else maxOf(0, currentCount - 1)
        runCatching {
            supabaseClient.postgrest["books"].update({
                set("like_count", nextCount)
            }) {
                filter { eq("id", bookId) }
            }
        }
        invalidateCache()
        return nowLiked
    }
}
