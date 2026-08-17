package com.semtinazizi.kitapduy.platform

/**
 * Platform specific book downloader.
 * Handles downloading audio files, saving them to local storage, and tracking progress.
 */
expect class BookDownloader {
    fun isBookDownloaded(bookId: String): Boolean
    fun getLocalFileUri(bookId: String): String
    suspend fun downloadBook(url: String, bookId: String, onProgress: (Float) -> Unit): Boolean
    fun deleteBook(bookId: String)
    fun getDownloadedBookIds(): List<String>
}

expect fun createBookDownloader(): BookDownloader
