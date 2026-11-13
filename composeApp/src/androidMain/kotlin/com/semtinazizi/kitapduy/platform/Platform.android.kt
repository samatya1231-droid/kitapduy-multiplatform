package com.semtinazizi.kitapduy.platform

import android.os.Build
import com.semtinazizi.kitapduy.BuildConfig

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

actual val platformName: String = "Android ${Build.VERSION.SDK_INT}"


actual fun createAudioPlayer(): AudioPlayer = AndroidAudioPlayer()

actual object AppConfig {
    actual val supabaseUrl: String = BuildConfig.SUPABASE_URL
    actual val supabaseKey: String = BuildConfig.SUPABASE_KEY
    actual val cfAccountId: String = BuildConfig.CF_ACCOUNT_ID
    actual val cfPublicUrl: String = BuildConfig.CF_PUBLIC_URL
    actual val cfWorkerUrl: String = BuildConfig.CF_WORKER_URL
    actual val cfUploadToken: String = BuildConfig.CF_UPLOAD_TOKEN
    actual val cfAccessKey: String = BuildConfig.CF_ACCESS_KEY
    actual val cfSecretKey: String = BuildConfig.CF_SECRET_KEY

    actual fun generatePresignedUrl(url: String): String {
        if (!url.contains("r2.cloudflarestorage.com")) return url

        val endpoint = "https://${cfAccountId}.r2.cloudflarestorage.com"
        val pathAndKey = url.substringAfter(endpoint)
        
        val now = Date()
        val dateFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val dateStampFormat = SimpleDateFormat("yyyyMMdd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val amzDate = dateFormat.format(now)
        val dateStamp = dateStampFormat.format(now)
        val host = "${cfAccountId}.r2.cloudflarestorage.com"
        val credentialScope = "$dateStamp/auto/s3/aws4_request"

        val canonicalHeaders = "host:$host\n"
        val signedHeaders = "host"
        val algorithm = "AWS4-HMAC-SHA256"

        val credential = "${cfAccessKey}/$credentialScope"
        val queryParams = listOf(
            "X-Amz-Algorithm" to algorithm,
            "X-Amz-Credential" to credential,
            "X-Amz-Date" to amzDate,
            "X-Amz-Expires" to "86400",
            "X-Amz-SignedHeaders" to signedHeaders
        ).sortedBy { it.first }

        val canonicalQueryString = queryParams.joinToString("&") { "${it.first}=${android.net.Uri.encode(it.second)}" }

        val canonicalRequest = buildString {
            append("GET\n")
            append("$pathAndKey\n")
            append("$canonicalQueryString\n")
            append(canonicalHeaders)
            append("\n")
            append(signedHeaders)
            append("\n")
            append("UNSIGNED-PAYLOAD")
        }

        val stringToSign = buildString {
            append("$algorithm\n")
            append("$amzDate\n")
            append("$credentialScope\n")
            append(sha256Hex(canonicalRequest.toByteArray()))
        }

        val signingKey = getSigningKey(cfSecretKey, dateStamp, "auto", "s3")
        val signature = hmacSha256Hex(signingKey, stringToSign)

        return "$url?$canonicalQueryString&X-Amz-Signature=$signature"
    }

    private fun sha256Hex(data: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(data).joinToString("") { "%02x".format(it) }
    }

    private fun hmacSha256(key: ByteArray, data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    private fun hmacSha256Hex(key: ByteArray, data: String): String {
        return hmacSha256(key, data).joinToString("") { "%02x".format(it) }
    }

    private fun getSigningKey(secret: String, dateStamp: String, region: String, service: String): ByteArray {
        val kDate = hmacSha256(("AWS4$secret").toByteArray(Charsets.UTF_8), dateStamp)
        val kRegion = hmacSha256(kDate, region)
        val kService = hmacSha256(kRegion, service)
        return hmacSha256(kService, "aws4_request")
    }
}
