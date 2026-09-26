package com.semtinazizi.kitapduy.platform

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class AndroidFileDownloader(private val context: Context) : FileDownloader {
    override fun getDownloadedBookIds(): List<String> = downloadsDir.listFiles().orEmpty()
        .filter { it.isFile && it.extension == "mp3" }.map { it.nameWithoutExtension }
    
    private val downloadsDir: File
        get() = File(context.filesDir, "audio_downloads").apply {
            if (!exists()) mkdirs()
        }

    override suspend fun downloadFile(url: String, fileName: String, onProgress: (Float) -> Unit): String = withContext(Dispatchers.IO) {
        val targetFile = File(downloadsDir, fileName)
        if (targetFile.exists()) {
            return@withContext targetFile.absolutePath
        }

        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connect()
            
            val fileLength = connection.contentLength
            val input = connection.inputStream
            val output = FileOutputStream(targetFile)
            
            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            
            while (input.read(data).also { count = it } != -1) {
                total += count
                if (fileLength > 0) {
                    onProgress((total * 100 / fileLength).toFloat() / 100f)
                }
                output.write(data, 0, count)
            }
            
            output.flush()
            output.close()
            input.close()
            
            return@withContext targetFile.absolutePath
        } catch (e: Exception) {
            if (targetFile.exists()) targetFile.delete()
            throw e
        }
    }

    override fun getDownloadedFilePath(fileName: String): String? {
        val targetFile = File(downloadsDir, fileName)
        return if (targetFile.exists()) targetFile.absolutePath else null
    }

    override fun deleteFile(fileName: String): Boolean {
        val targetFile = File(downloadsDir, fileName)
        return if (targetFile.exists()) targetFile.delete() else false
    }
}

actual fun createFileDownloader(): FileDownloader {
    val context = com.semtinazizi.kitapduy.MainActivity.instance 
        ?: error("FileDownloader requires MainActivity context")
    return AndroidFileDownloader(context)
}
