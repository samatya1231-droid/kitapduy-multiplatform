package com.semtinazizi.kitapduy.domain.repository

import com.semtinazizi.kitapduy.model.Book

interface BookRepository {
    fun invalidateCache() {}
    suspend fun getBooks(): List<Book>
    suspend fun getFavoriteBookIds(userId: String): Set<String>
    suspend fun getDailySummary(): Pair<String, String>? // Title, Text
    suspend fun getDailyQuote(): Triple<String, String, String>? // Text, Author, Book
    suspend fun searchBooks(query: String): List<Book>
    suspend fun getPodcasts(): List<com.semtinazizi.kitapduy.model.Podcast>
    suspend fun searchPodcasts(query: String): List<com.semtinazizi.kitapduy.model.Podcast>
    suspend fun getBookshelf(userId: String, shelfType: String): List<com.semtinazizi.kitapduy.model.SupabaseBookshelf>
    suspend fun isBookLiked(bookId: String, userId: String): Boolean = false
    suspend fun toggleBookLike(bookId: String, userId: String): Boolean = false
    suspend fun getBookById(bookId: String): Book? = getBooks().firstOrNull { it.id == bookId }
    suspend fun resolvePodcastEpisode(
        bookId: String,
        fallbackTitle: String = "",
        fallbackCover: String = "",
        fallbackAudioUrl: String = "",
        fallbackAuthor: String = ""
    ): Pair<com.semtinazizi.kitapduy.model.PodcastEpisode, String>? = null
}
