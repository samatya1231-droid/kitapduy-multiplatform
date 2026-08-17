package com.semtinazizi.kitapduy.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.*

@OptIn(ExperimentalForeignApi::class)
class IosFileDownloader : FileDownloader {

    private val fileManager = NSFileManager.defaultManager()

    private fun getDownloadsDir(): String {
        val urls = fileManager.URLsForDirectory(NSDocumentDirectory, inDomains = NSUserDomainMask)
        val documentDir = urls.first() as NSURL
        val downloadsPath = documentDir.URLByAppendingPathComponent("audio_downloads")?.path ?: return ""
        
        // Create directory if not exists
        if (!fileManager.fileExistsAtPath(downloadsPath)) {
            fileManager.createDirectoryAtPath(downloadsPath, withIntermediateDirectories = true, attributes = null, error = null)
        }
        return downloadsPath
    }

    override suspend fun downloadFile(url: String, fileName: String, onProgress: (Float) -> Unit): String = withContext(Dispatchers.IO) {
        val dir = getDownloadsDir()
        if (dir.isEmpty()) error("Could not get downloads directory")
        
        val targetPath = "$dir/$fileName"
        if (fileManager.fileExistsAtPath(targetPath)) {
            return@withContext "file://$targetPath"
        }

        val nsUrl = NSURL.URLWithString(url) ?: error("Invalid URL")
        val data = NSData.dataWithContentsOfURL(nsUrl) ?: error("Failed to download data")
        
        val success = data.writeToFile(targetPath, atomically = true)
        if (!success) error("Failed to save downloaded file")
        
        onProgress(1.0f) // Simulated instant progress since it's synchronous downloading
        return@withContext "file://$targetPath"
    }

    override fun getDownloadedFilePath(fileName: String): String? {
        val path = "${getDownloadsDir()}/$fileName"
        return if (fileManager.fileExistsAtPath(path)) "file://$path" else null
    }

    override fun deleteFile(fileName: String): Boolean {
        val path = "${getDownloadsDir()}/$fileName"
        if (fileManager.fileExistsAtPath(path)) {
            return fileManager.removeItemAtPath(path, error = null)
        }
        return false
    }
}

actual fun createFileDownloader(): FileDownloader = IosFileDownloader()
