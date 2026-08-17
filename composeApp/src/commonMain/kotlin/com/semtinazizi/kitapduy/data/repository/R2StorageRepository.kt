package com.semtinazizi.kitapduy.data.repository

import com.semtinazizi.kitapduy.platform.AppConfig
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.datetime.Clock
import kotlin.random.Random

interface StorageRepository {
    suspend fun uploadProfilePicture(userId: String, bytes: ByteArray, extension: String): Result<String>
    suspend fun uploadImage(folder: String, fileNamePrefix: String, bytes: ByteArray, extension: String): Result<String>
}

class R2StorageRepository(private val client: HttpClient) : StorageRepository {
    override suspend fun uploadProfilePicture(userId: String, bytes: ByteArray, extension: String): Result<String> = runCatching {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val randomStr = Random.nextInt(1000, 9999)
        val fileName = "profile_pictures/${userId}_${timestamp}_${randomStr}.$extension"

        val contentType = when (extension.lowercase()) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }

        val response = client.put("${AppConfig.cfWorkerUrl.trimEnd('/')}/$fileName") {
            header("Authorization", "Bearer ${AppConfig.cfUploadToken}")
            header("Content-Type", contentType)
            setBody(bytes)
        }

        if (response.status.isSuccess()) {
            "${AppConfig.cfPublicUrl.trimEnd('/')}/$fileName"
        } else {
            throw Exception("Upload failed with status: ${response.status}")
        }
    }

    override suspend fun uploadImage(folder: String, fileNamePrefix: String, bytes: ByteArray, extension: String): Result<String> = runCatching {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val randomStr = Random.nextInt(1000, 9999)
        val fileName = "${folder}/${fileNamePrefix}_${timestamp}_${randomStr}.$extension"

        val contentType = when (extension.lowercase()) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }

        val response = client.put("${AppConfig.cfWorkerUrl.trimEnd('/')}/$fileName") {
            header("Authorization", "Bearer ${AppConfig.cfUploadToken}")
            header("Content-Type", contentType)
            setBody(bytes)
        }

        if (response.status.isSuccess()) {
            "${AppConfig.cfPublicUrl.trimEnd('/')}/$fileName"
        } else {
            throw Exception("Upload failed with status: ${response.status}")
        }
    }
}
