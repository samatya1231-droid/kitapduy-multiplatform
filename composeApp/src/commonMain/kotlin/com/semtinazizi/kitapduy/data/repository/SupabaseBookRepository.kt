package com.semtinazizi.kitapduy.data.repository

import com.semtinazizi.kitapduy.domain.repository.BookRepository
import com.semtinazizi.kitapduy.model.Book
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

import com.semtinazizi.kitapduy.platform.AppConfig

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
                .decodeList<Book>()
            
            books.filter { it.title.isNotBlank() && it.author.isNotBlank() }.map { 
                it.copy(
                    coverUrl = fixImageUrl(it.coverUrl),
                    audioUrl = fixUrl(it.audioUrl)
                ) 
            }
        } catch (e: Exception) {
            println("GET_BOOKS_ERROR: ${e.stackTraceToString()}")
            emptyList()
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
        } catch (e: Exception) {
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
        } catch (e: Exception) {
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
                .decodeList<Book>()
                
            books.filter { it.title.isNotBlank() && it.author.isNotBlank() }.map { 
                it.copy(
                    coverUrl = fixImageUrl(it.coverUrl),
                    audioUrl = fixUrl(it.audioUrl)
                ) 
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getPodcasts(): List<com.semtinazizi.kitapduy.model.Podcast> {
        return try {
            println("SupabaseRepository: Fetching from 'podcasts' table...")
            val response = supabaseClient.postgrest["podcasts"].select()
            
            val podcasts = response.decodeList<com.semtinazizi.kitapduy.model.Podcast>()
            println("SupabaseRepository: Decoded ${podcasts.size} podcasts")
            
            podcasts.map { it.copy(
                imageUrl = fixImageUrl(it.imageUrl),
                rssUrl = fixUrl(it.rssUrl)
            ) }
        } catch (e: Exception) {
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
                .decodeList<com.semtinazizi.kitapduy.model.Podcast>()
                
            podcasts.map { it.copy(
                imageUrl = fixImageUrl(it.imageUrl),
                rssUrl = fixUrl(it.rssUrl)
            ) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getBookshelf(userId: String, shelfType: String): List<com.semtinazizi.kitapduy.model.SupabaseBookshelf> {
        return try {
            supabaseClient.postgrest["bookshelf"]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("shelf_type", shelfType)
                    }
                }
                .decodeList<com.semtinazizi.kitapduy.model.SupabaseBookshelf>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
