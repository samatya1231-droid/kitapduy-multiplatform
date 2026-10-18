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
                    val codePoint: Int
                    val charCount: Int
                    if (c.isHighSurrogate() && i + 1 < len && trimmed[i + 1].isLowSurrogate()) {
                        codePoint = (c.code - 0xD800 shl 10) + (trimmed[i + 1].code - 0xDC00) + 0x10000
                        charCount = 2
                    } else {
                        codePoint = c.code
                        charCount = 1
                    }
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
