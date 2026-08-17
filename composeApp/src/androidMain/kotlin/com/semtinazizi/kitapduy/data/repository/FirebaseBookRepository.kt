package com.semtinazizi.kitapduy.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.semtinazizi.kitapduy.domain.repository.BookRepository
import com.semtinazizi.kitapduy.model.Book
import kotlinx.coroutines.tasks.await
import android.util.Log

class FirebaseBookRepository(
    private val db: FirebaseFirestore
) : BookRepository {

    override suspend fun getBooks(): List<Book> {
        return try {
            val snapshot = db.collection("books").limit(1000).get().await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val coverUrl = doc.getString("coverUrl") ?: return@mapNotNull null
                    if (title.isBlank() || coverUrl.isBlank()) return@mapNotNull null

                    Book(
                        id = doc.id,
                        title = title,
                        author = doc.getString("author") ?: "Bilinmiyor",
                        category = doc.getString("category") ?: "Genel",
                        coverUrl = coverUrl,
                        audioUrl = doc.getString("audioUrl") ?: "",
                        description = doc.getString("description") ?: "Açıklama bulunmuyor.",
                        duration = doc.getLong("duration") ?: 0L,
                        listenCount = doc.getLong("listenCount") ?: 0L,
                        viewCount = doc.getLong("viewCount")?.toInt() ?: 0,
                        likeCount = doc.getLong("likeCount")?.toInt() ?: 0
                    )
                } catch (e: Exception) {
                    Log.e("FirebaseBookRepository", "Book parse error for doc ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseBookRepository", "Failed to fetch books", e)
            emptyList()
        }
    }

    override suspend fun getFavoriteBookIds(userId: String): Set<String> {
        if (userId.isBlank()) return emptySet()
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("favorites")
                .get()
                .await()
            snapshot.documents.map { it.id }.toSet()
        } catch (e: Exception) {
            Log.e("FirebaseBookRepository", "Failed to fetch favorites", e)
            emptySet()
        }
    }

    override suspend fun getDailySummary(): Pair<String, String>? {
        return try {
            val calendar = java.util.Calendar.getInstance()
            val dayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
            val year = calendar.get(java.util.Calendar.YEAR)
            val seed = (year * 1000 + dayOfYear).toLong()

            val doc = db.collection("app_settings").document("daily_summaries").get().await()
            val listData = doc.get("list") as? List<*>
            if (!listData.isNullOrEmpty()) {
                val summariesList = listData.filterIsInstance<Map<String, Any>>()
                if (summariesList.isNotEmpty()) {
                    val random = java.util.Random(seed)
                    val index = random.nextInt(summariesList.size)
                    val item = summariesList[index]
                    val title = item["title"]?.toString() ?: return null
                    val content = item["content"]?.toString() ?: return null
                    Pair(title, content)
                } else null
            } else null
        } catch (e: Exception) {
            Log.e("FirebaseBookRepository", "Failed to fetch daily summary", e)
            null
        }
    }

    override suspend fun getDailyQuote(): Triple<String, String, String>? {
        return try {
            val calendar = java.util.Calendar.getInstance()
            val dayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
            val year = calendar.get(java.util.Calendar.YEAR)
            val seed = (year * 1000 + dayOfYear).toLong()

            var finalQuotes = try {
                val quotesSnapshot = db.collection("daily_quotes_v2").get().await()
                quotesSnapshot.documents.filter { doc ->
                    val text = doc.getString("text") ?: ""
                    text.isNotBlank()
                }
            } catch (e: Exception) { emptyList() }

            if (finalQuotes.isEmpty()) {
                val oldFixedSnapshot = db.collection("daily_quotes_fixed").get().await()
                finalQuotes = oldFixedSnapshot.documents.filter { doc ->
                    val text = doc.getString("text") ?: ""
                    text.isNotBlank() && !text.contains("Jean Valjean", ignoreCase = true)
                }
            }

            if (finalQuotes.isNotEmpty()) {
                val random = java.util.Random(seed)
                val index = random.nextInt(finalQuotes.size)
                val qDoc = finalQuotes[index]
                
                val text = qDoc.getString("text") ?: ""
                val author = qDoc.getString("author") ?: "Bilinmeyen Yazar"
                val book = qDoc.getString("book") ?: ""
                Triple(text, author, book)
            } else null
        } catch (e: Exception) {
            Log.e("FirebaseBookRepository", "Failed to fetch daily quote", e)
            null
        }
    }
    override suspend fun searchBooks(query: String): List<Book> {
        return try {
            val snapshot = db.collection("books")
                .orderBy("title")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    Book(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        author = doc.getString("author") ?: "",
                        coverUrl = doc.getString("coverUrl") ?: ""
                    )
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) {
            Log.e("FirebaseBookRepo", "Search failed", e)
            emptyList()
        }
    }

    override suspend fun getBookshelf(userId: String, shelfType: String): List<com.semtinazizi.kitapduy.model.SupabaseBookshelf> {
        return emptyList()
    }
    
    override suspend fun getPodcasts(): List<com.semtinazizi.kitapduy.model.Podcast> {
        return emptyList()
    }
    
    override suspend fun searchPodcasts(query: String): List<com.semtinazizi.kitapduy.model.Podcast> {
        return emptyList()
    }
}
