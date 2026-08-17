package com.semtinazizi.kitapduy.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

import org.koin.core.context.GlobalContext

actual class BookDownloader {
    
    private val context: android.content.Context
        get() = GlobalContext.get().get()

    actual fun isBookDownloaded(bookId: String): Boolean =
        File(context.filesDir, "$bookId.mp3").exists()

    actual fun getLocalFileUri(bookId: String): String =
        File(context.filesDir, "$bookId.mp3").absolutePath

    actual suspend fun downloadBook(url: String, bookId: String, onProgress: (Float) -> Unit): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection()
                val totalLength = connection.contentLengthLong
                val inputStream = connection.getInputStream()
                val file = File(context.filesDir, "$bookId.mp3")
                val outputStream = FileOutputStream(file)
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = 0L
                
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    if (totalLength > 0) {
                        onProgress(totalRead.toFloat() / totalLength)
                    }
                }
                outputStream.close()
                inputStream.close()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    actual fun getDownloadedBookIds(): List<String> {
        val files = context.filesDir.listFiles() ?: return emptyList()
        return files.filter { it.name.endsWith(".mp3") }.map { it.name.removeSuffix(".mp3") }
    }

    actual fun deleteBook(bookId: String) {
        val file = File(context.filesDir, "$bookId.mp3")
        if (file.exists()) {
            file.delete()
        }
    }
}

actual fun createBookDownloader(): BookDownloader = BookDownloader()
