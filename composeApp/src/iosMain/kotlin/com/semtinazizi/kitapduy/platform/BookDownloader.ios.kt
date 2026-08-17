@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.semtinazizi.kitapduy.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual class BookDownloader {
    
    private val fileManager = NSFileManager.defaultManager
    
    private fun getDocumentsDirectory(): String {
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        return paths.first() as String
    }

    actual fun isBookDownloaded(bookId: String): Boolean {
        val filePath = "${getDocumentsDirectory()}/$bookId.mp3"
        return fileManager.fileExistsAtPath(filePath)
    }

    actual fun getLocalFileUri(bookId: String): String {
        return "file://${getDocumentsDirectory()}/$bookId.mp3"
    }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun downloadBook(url: String, bookId: String, onProgress: (Float) -> Unit): Boolean {
        return suspendCoroutine { continuation ->
            val nsUrl = NSURL.URLWithString(url)
            if (nsUrl == null) {
                continuation.resume(false)
                return@suspendCoroutine
            }

            val request = NSURLRequest.requestWithURL(nsUrl)
            val session = NSURLSession.sharedSession
            
            val task = session.downloadTaskWithRequest(request) { location, response, error ->
                if (error != null || location == null) {
                    continuation.resume(false)
                    return@downloadTaskWithRequest
                }
                
                val destinationPath = "${getDocumentsDirectory()}/$bookId.mp3"
                val destinationUrl = NSURL.fileURLWithPath(destinationPath)
                
                // Eğer varsa önce sil
                if (fileManager.fileExistsAtPath(destinationPath)) {
                    fileManager.removeItemAtURL(destinationUrl, null)
                }
                
                // Dosyayı taşı
                val success = fileManager.moveItemAtURL(location, destinationUrl, null)
                
                continuation.resume(success)
            }
            
            task.resume()
        }
    }

    actual fun getDownloadedBookIds(): List<String> {
        val path = getDocumentsDirectory()
        val contents = fileManager.contentsOfDirectoryAtPath(path, null) as? List<String> ?: emptyList()
        return contents.filter { it.endsWith(".mp3") }.map { it.removeSuffix(".mp3") }
    }

    actual fun deleteBook(bookId: String) {
        val filePath = "${getDocumentsDirectory()}/$bookId.mp3"
        val fileUrl = NSURL.fileURLWithPath(filePath)
        if (fileManager.fileExistsAtPath(filePath)) {
            fileManager.removeItemAtURL(fileUrl, null)
        }
    }
}

actual fun createBookDownloader(): BookDownloader = BookDownloader()
