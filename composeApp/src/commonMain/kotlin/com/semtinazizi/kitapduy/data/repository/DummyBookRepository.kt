package com.semtinazizi.kitapduy.data.repository

import com.semtinazizi.kitapduy.domain.repository.BookRepository
import com.semtinazizi.kitapduy.model.Book
import kotlinx.coroutines.delay

class DummyBookRepository : BookRepository {
    override suspend fun getBooks(): List<Book> {
        delay(800) // Ağ gecikmesi simülasyonu
        return listOf(
            Book(
                id = "1",
                title = "Suç ve Ceza",
                author = "Fyodor Dostoyevski",
                category = "Roman",
                coverUrl = "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=400&q=80",
                description = "Vicdan azabı ve insan psikolojisi üzerine bir başyapıt."
            ),
            Book(
                id = "2",
                title = "Savaş ve Barış",
                author = "Lev Tolstoy",
                category = "Tarih",
                coverUrl = "https://images.unsplash.com/photo-1589829085413-56de8ae18c73?w=400&q=80",
                description = "Napolyon'un Rusya'yı işgali döneminde geçen destansı bir eser."
            ),
            Book(
                id = "3",
                title = "Simyacı",
                author = "Paulo Coelho",
                category = "Kişisel Gelişim",
                coverUrl = "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=400&q=80",
                description = "Kendi efsanesini yaşamak isteyenlere rehberlik eden bir hikaye."
            ),
            Book(
                id = "4",
                title = "Sefiller",
                author = "Victor Hugo",
                category = "Roman",
                coverUrl = "https://images.unsplash.com/photo-1532012197267-da84d127e765?w=400&q=80",
                description = "Jean Valjean'ın kurtuluş ve merhamet dolu yaşam öyküsü."
            ),
            Book(
                id = "5",
                title = "İnsan Ne İle Yaşar?",
                author = "Lev Tolstoy",
                category = "Roman",
                coverUrl = "https://images.unsplash.com/photo-1476275466078-4007374efac4?w=400&q=80",
                description = "İnsan kalbinin derinliklerine inen, sevgi temalı kısa öyküler."
            ),
            Book(
                id = "6",
                title = "Nutuk",
                author = "Mustafa Kemal Atatürk",
                category = "Tarih",
                coverUrl = "https://images.unsplash.com/photo-1461360228754-6e81c478b882?w=400&q=80",
                description = "Modern Türkiye'nin kuruluş destanı."
            ),
            Book(
                id = "7",
                title = "Atomik Alışkanlıklar",
                author = "James Clear",
                category = "Kişisel Gelişim",
                coverUrl = "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400&q=80",
                description = "Küçük değişikliklerin büyük sonuçlar doğurabileceği fikrini açıklar."
            )
        )
    }

    override suspend fun getFavoriteBookIds(userId: String): Set<String> {
        return setOf("1", "3")
    }

    override suspend fun getDailySummary(): Pair<String, String>? {
        return Pair("Günlük Okuma Hedefin", "Hedefine ulaşmak için bugün sadece 15 sayfa daha okuman gerekiyor!")
    }

    override suspend fun getDailyQuote(): Triple<String, String, String>? {
        return Triple(
            "Bir kitap okuyan her şeyi okuyabilir.",
            "Sabahattin Ali",
            "Kürk Mantolu Madonna"
        )
    }

    override suspend fun searchBooks(query: String): List<Book> {
        return emptyList()
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
