package com.semtinazizi.kitapduy.domain.repository

import com.semtinazizi.kitapduy.model.Book

interface BookRepository {
    suspend fun getBooks(): List<Book>
    suspend fun getFavoriteBookIds(userId: String): Set<String>
    suspend fun getDailySummary(): Pair<String, String>? // Title, Text
    suspend fun getDailyQuote(): Triple<String, String, String>? // Text, Author, Book
    suspend fun searchBooks(query: String): List<Book>
    suspend fun getPodcasts(): List<com.semtinazizi.kitapduy.model.Podcast>
    suspend fun searchPodcasts(query: String): List<com.semtinazizi.kitapduy.model.Podcast>
    suspend fun getBookshelf(userId: String, shelfType: String): List<com.semtinazizi.kitapduy.model.SupabaseBookshelf>
}
