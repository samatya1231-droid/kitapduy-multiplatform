package com.semtinazizi.kitapduy.data

import com.semtinazizi.kitapduy.platform.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Requires the authenticated server endpoint described in docs/account-deletion.md. */
internal object AccountDeletionClient {
    suspend fun delete(expectedUserId: String, accessToken: String): Boolean {
        val baseUrl = AppConfig.supabaseUrl.trimEnd('/')
        check(baseUrl.startsWith("https://"))
        val client = HttpClient { followRedirects = false }
        try {
            val response = client.post("$baseUrl/functions/v1/delete-account") {
                bearerAuth(accessToken)
                header("apikey", AppConfig.supabaseKey)
                contentType(ContentType.Application.Json)
                setBody("{\"confirm\":true}")
            }
            return confirmsDeletion(response.status.value, response.bodyAsText(), expectedUserId)
        } finally {
            client.close()
        }
    }

    internal fun confirmsDeletion(status: Int, body: String, expectedUserId: String): Boolean {
        if (status != 200 || expectedUserId.isBlank()) return false
        return try {
            val result = Json.parseToJsonElement(body).jsonObject
            val deleted = result["deleted"]?.jsonPrimitive
            val userId = result["user_id"]?.jsonPrimitive
            deleted?.isString == false && deleted.booleanOrNull == true &&
                userId?.isString == true && userId.content == expectedUserId
        } catch (_: Exception) { false }
    }
}
