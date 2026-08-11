package com.semtinazizi.kitapduy.data.repository

import com.semtinazizi.kitapduy.domain.repository.BookRepository
import com.semtinazizi.kitapduy.model.Book
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class SupabaseBookRepository(private val supabaseClient: SupabaseClient) : BookRepository {
    
    override suspend fun getBooks(): List<Book> {
        return try {
            supabaseClient.postgrest["books"]
                .select()
                .decodeList<Book>()
        } catch (e: Exception) {
            e.printStackTrace()
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
            // "daily_quotes" gibi bir tablonuz olduğunu varsayıyoruz
            // Yoksa basit bir listeyle de mocklanabilir
            val result = supabaseClient.postgrest["daily_quotes"]
                .select {
                    limit(1)
                }
                .decodeSingleOrNull<Map<String, String>>()
                
            if (result != null) {
                Triple(
                    result["quote"] ?: "",
                    result["author"] ?: "",
                    result["book"] ?: ""
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Triple(
                "Bir kitap okuyan her şeyi okuyabilir.",
                "Sabahattin Ali",
                "Kürk Mantolu Madonna"
            )
        }
    }
}
