package com.semtinazizi.kitapduy.domain.repository

import com.semtinazizi.kitapduy.model.Book

interface BookRepository {
    suspend fun getBooks(): List<Book>
    suspend fun getFavoriteBookIds(userId: String): Set<String>
    suspend fun getDailySummary(): Pair<String, String>? // Title, Text
    suspend fun getDailyQuote(): Triple<String, String, String>? // Text, Author, Book
}
