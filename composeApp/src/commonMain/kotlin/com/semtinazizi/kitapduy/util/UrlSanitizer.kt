package com.semtinazizi.kitapduy.util

/**
 * URL içindeki boşluk, Türkçe karakterler (ı, ö, ü, ç, ş, ğ, İ, Ö, Ü, Ç, Ş, Ğ)
 * ve diğer ASCII dışı Unicode karakterleri RFC 3986 uyumlu percent-encoding formatına çevirir.
 *
 * Özellikler:
 * - Halihazırda percent-encoded olan kısımları (%20, %C4%B1 vb.) tekrar encode etmez (idempotent).
 * - Boşlukları '%20' yapar.
 * - Ters eğik çizgileri ('\') URL standartlarına uygun olarak '/' yapar.
 * - Tek başına duran '%' karakterlerini '%25' yapar.
 * - Standart URI ayırıcı ve karakterlerini (: / ? # [ ] @ ! $ & ' ( ) * + , ; = - . _ ~) korur.
 * - KMP commonMain uyumludur (Android ve iOS'ta harici bağımlılık gerektirmeden çalışır).
 */
object UrlSanitizer {
    const val CUSTOM_R2_BASE_URL = "https://ses.kitapduy.com.tr"

    fun normalizeMediaUrl(rawUrl: String?, defaultBaseUrl: String = CUSTOM_R2_BASE_URL): String {
        val trimmed = rawUrl?.trim().orEmpty()
        if (trimmed.isEmpty()) return ""
        if (trimmed.startsWith("data:image", ignoreCase = true)) return trimmed

        val isLoopback = trimmed.startsWith("http://localhost", ignoreCase = true) ||
            trimmed.startsWith("http://127.0.0.1", ignoreCase = true)
        val isLocalUri = trimmed.startsWith("file://", ignoreCase = true) ||
            trimmed.startsWith("content://", ignoreCase = true) ||
            trimmed.startsWith("asset://", ignoreCase = true)
        if (isLoopback || isLocalUri) return sanitizeUrl(trimmed)

        var targetUrl = if (trimmed.startsWith("http://", ignoreCase = true)) {
            "https://" + trimmed.removePrefix("http://")
        } else if (trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed
        } else {
            val cleanBase = defaultBaseUrl.trim().trimEnd('/').let {
                if (it.isBlank() || (!it.startsWith("http://localhost") && !it.startsWith("http://127.0.0.1") &&
                        (it.contains("workers.dev") || it.contains(".r2.dev") || it.contains("r2.cloudflarestorage.com")))) {
                    CUSTOM_R2_BASE_URL
                } else it
            }
            "$cleanBase/${trimmed.trimStart('/')}"
        }

        val isLegacyR2 = targetUrl.contains("r2.cloudflarestorage.com", ignoreCase = true) ||
            targetUrl.contains(".r2.dev", ignoreCase = true) ||
            targetUrl.contains("workers.dev", ignoreCase = true)

        if (isLegacyR2) {
            var cleanPath = when {
                targetUrl.contains("r2.cloudflarestorage.com/", ignoreCase = true) ->
                    targetUrl.substringAfter("r2.cloudflarestorage.com/")
                targetUrl.contains(".r2.dev/", ignoreCase = true) ->
                    targetUrl.substringAfter(".r2.dev/")
                targetUrl.contains("workers.dev/", ignoreCase = true) ->
                    targetUrl.substringAfter("workers.dev/")
                else -> targetUrl.substringAfter("://").substringAfter('/', "")
            }
            cleanPath = cleanPath.substringBefore("?t=").substringBefore("?").trimStart('/')
            val bucket = "kitapduyprofiller/"
            if (cleanPath.startsWith(bucket)) {
                cleanPath = cleanPath.removePrefix(bucket)
            }
            targetUrl = "$CUSTOM_R2_BASE_URL/${cleanPath.trimStart('/')}"
        }

        return sanitizeUrl(targetUrl)
    }

    fun sanitizeUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return ""

        val sb = StringBuilder(trimmed.length + 16)
        var i = 0
        val len = trimmed.length

        while (i < len) {
            val c = trimmed[i]
            when {
                // Halihazırda geçerli bir hex escape mi: %XX
                c == '%' -> {
                    if (i + 2 < len && isHex(trimmed[i + 1]) && isHex(trimmed[i + 2])) {
                        sb.append(c)
                        sb.append(trimmed[i + 1])
                        sb.append(trimmed[i + 2])
                        i += 3
                    } else {
                        sb.append("%25")
                        i++
                    }
                }
                // Ters eğik çizgi URL'lerde '/' olmalı
                c == '\\' -> {
                    sb.append('/')
                    i++
                }
                // Standart URL karakterleri (RFC 3986 unreserved + reserved delimiters)
                c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' ||
                c in "-._~:/?#[]@!$&'()*+,;=" -> {
                    sb.append(c)
                    i++
                }
                // Boşluk karakteri
                c == ' ' -> {
                    sb.append("%20")
                    i++
                }
                // Türkçe karakterler, genişletilmiş Latin, Unicode / UTF-8 karakterler
                else -> {
                    val charCount = if (c.isHighSurrogate() && i + 1 < len && trimmed[i + 1].isLowSurrogate()) 2 else 1
                    val str = trimmed.substring(i, i + charCount)
                    val bytes = str.encodeToByteArray()
                    for (b in bytes) {
                        sb.append('%')
                        val hex = (b.toInt() and 0xFF).toString(16).uppercase()
                        if (hex.length == 1) sb.append('0')
                        sb.append(hex)
                    }
                    i += charCount
                }
            }
        }
        return sb.toString()
    }

    private fun isHex(c: Char): Boolean =
        c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F'
}
