package com.semtinazizi.kitapduy.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class)
class IosFileDownloader : FileDownloader {
    private val fileManager = NSFileManager.defaultManager
    private fun documents(): String = (fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).first() as NSURL).path!!
    private fun downloads(): String = (documents() + "/audio_downloads").also {
        if (!fileManager.fileExistsAtPath(it)) {
            check(fileManager.createDirectoryAtPath(it, true, null, null)) { "İndirme klasörü oluşturulamadı." }
        }
    }
    private fun validate(fileName: String) {
        require(fileName.isNotBlank() && '/' !in fileName && '\\' !in fileName && fileName != "." && fileName != "..")
    }
    override fun getDownloadedBookIds(): List<String> = listOf(downloads(), documents()).flatMap { directory ->
        (fileManager.contentsOfDirectoryAtPath(directory, null) as? List<String>).orEmpty()
            .filter { it.endsWith(".mp3") }.map { it.removeSuffix(".mp3") }
    }.distinct()

    override fun getDownloadedFilePath(fileName: String): String? {
        validate(fileName)
        return listOf(downloads(), documents()).map { "$it/$fileName" }
            .firstOrNull { fileManager.fileExistsAtPath(it) }?.let { NSURL.fileURLWithPath(it).absoluteString }
    }

    override suspend fun downloadFile(url: String, fileName: String, onProgress: (Float) -> Unit): String {
        validate(fileName)
        getDownloadedFilePath(fileName)?.let { onProgress(1f); return it }
        val destination = NSURL.fileURLWithPath(downloads() + "/" + fileName)
        val source = NSURL.URLWithString(url) ?: error("Geçersiz ses adresi.")
        onProgress(0f)
        val result = suspendCancellableCoroutine<String> { continuation ->
            val task = NSURLSession.sharedSession.downloadTaskWithRequest(NSURLRequest.requestWithURL(source)) { location, response, error ->
                if (!continuation.isActive) return@downloadTaskWithRequest
                val status = (response as? NSHTTPURLResponse)?.statusCode ?: 0L
                if (error != null || location == null || status !in 200L..299L) {
                    continuation.resumeWithException(IllegalStateException("Ses indirilemedi (HTTP $status)."))
                } else if (fileManager.moveItemAtURL(location, destination, null)) {
                    continuation.resume(destination.absoluteString!!)
                } else {
                    continuation.resumeWithException(IllegalStateException("Ses kaydedilemedi."))
                }
            }
            continuation.invokeOnCancellation { task.cancel() }
            task.resume()
        }
        onProgress(1f)
        return result
    }

    override fun deleteFile(fileName: String): Boolean {
        validate(fileName)
        var deleted = false
        listOf(downloads(), documents()).forEach { directory ->
            val path = "$directory/$fileName"
            if (fileManager.fileExistsAtPath(path)) deleted = fileManager.removeItemAtPath(path, null) || deleted
        }
        return deleted
    }
}

actual fun createFileDownloader(): FileDownloader = IosFileDownloader()
