package com.semtinazizi.kitapduy.util

object RssTextCleaner {
    private val ENTITY_REGEX = Regex("&(#x[0-9a-fA-F]+|#\\d+|[a-zA-Z]+);")
    private val LINE_BREAK_TAG_REGEX = Regex("<(?:br\\s*/?|/p|/div|/li|/h[1-6])\\s*>", RegexOption.IGNORE_CASE)
    private val LIST_ITEM_OPEN_REGEX = Regex("<li(?:\\s[^>]*)?>", RegexOption.IGNORE_CASE)
    private val SCRIPT_BLOCK_REGEX = Regex("<script(?:\\s[^>]*)?>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE)
    private val STYLE_BLOCK_REGEX = Regex("<style(?:\\s[^>]*)?>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE)
    private val ANY_HTML_TAG_REGEX = Regex("<[^>]*>")
    private val MULTI_SPACE_REGEX = Regex("[ \\t\\x0B\\f\\r]+")
    private val MULTI_NEWLINE_REGEX = Regex("\\n{3,}")

    private val NAMED_ENTITIES = mapOf(
        "amp" to "&",
        "lt" to "<",
        "gt" to ">",
        "quot" to "\"",
        "apos" to "'",
        "nbsp" to " ",
        "ndash" to "–",
        "mdash" to "—",
        "hellip" to "…",
        "lsquo" to "‘",
        "rsquo" to "’",
        "ldquo" to "“",
        "rdquo" to "”",
        "copy" to "©",
        "reg" to "®",
        "trade" to "™",
        "bull" to "•",
        "middot" to "·"
    )

    fun unwrapCdata(raw: String): Pair<String, Boolean> {
        val trimmed = raw.trim()
        return if (trimmed.startsWith("<![CDATA[") && trimmed.endsWith("]]>")) {
            trimmed.substring(9, trimmed.length - 3) to true
        } else {
            val hadCdata = raw.contains("<![CDATA[")
            raw.replace("<![CDATA[", "").replace("]]>", "") to hadCdata
        }
    }

    /**
     * Single-pass XML/HTML entity decoder.
     * Decodes standard XML entities (&amp;, &lt;, &gt;, &quot;, &apos;), common HTML named entities,
     * and numeric character references (&#39;, &#x26;, &#x1F3A7;) without double-decoding.
     */
    fun decodeXmlEntities(input: String): String {
        if (!input.contains('&')) return input
        return ENTITY_REGEX.replace(input) { match ->
            val token = match.groupValues[1]
            when {
                token.startsWith("#x", ignoreCase = true) -> {
                    val codePoint = token.substring(2).toIntOrNull(16)
                    codePointToString(codePoint) ?: match.value
                }
                token.startsWith("#") -> {
                    val codePoint = token.substring(1).toIntOrNull(10)
                    codePointToString(codePoint) ?: match.value
                }
                else -> NAMED_ENTITIES[token.lowercase()] ?: match.value
            }
        }
    }

    /**
     * Cleans an RSS/HTML episode description into readable plain text, preserving paragraph breaks.
     */
    fun cleanHtmlDescription(raw: String, wasCdata: Boolean = false): String {
        if (raw.isBlank()) return ""
        val (unwrapped, detectedCdata) = unwrapCdata(raw)
        val isCdata = wasCdata || detectedCdata

        // Non-CDATA XML descriptions may contain XML-escaped HTML (&lt;p&gt;...&lt;/p&gt;).
        val htmlText = if (isCdata) unwrapped else decodeXmlEntities(unwrapped)
        val withoutScripts = STYLE_BLOCK_REGEX.replace(SCRIPT_BLOCK_REGEX.replace(htmlText, " "), " ")
        val withBullets = LIST_ITEM_OPEN_REGEX.replace(withoutScripts, "\n• ")
        val withBreaks = LINE_BREAK_TAG_REGEX.replace(withBullets, "\n")
        val withoutTags = ANY_HTML_TAG_REGEX.replace(withBreaks, " ")
        val decoded = decodeXmlEntities(withoutTags)
            .replace('\u00A0', ' ')
            .replace("\r\n", "\n")
            .replace('\r', '\n')

        return MULTI_NEWLINE_REGEX.replace(
            decoded.lines()
                .map { line -> MULTI_SPACE_REGEX.replace(line, " ").trim() }
                .joinToString("\n"),
            "\n\n"
        ).trim()
    }

    fun cleanTitle(raw: String): String {
        if (raw.isBlank()) return ""
        val (unwrapped, _) = unwrapCdata(raw)
        val decoded = decodeXmlEntities(unwrapped)
        val withoutTags = ANY_HTML_TAG_REGEX.replace(decoded, " ")
        return MULTI_SPACE_REGEX.replace(withoutTags.replace('\n', ' '), " ").trim()
    }

    private fun codePointToString(codePoint: Int?): String? {
        if (codePoint == null || codePoint <= 0 || codePoint > 0x10FFFF) return null
        if (codePoint in 0xD800..0xDFFF) return null
        return if (codePoint <= 0xFFFF) {
            codePoint.toChar().toString()
        } else {
            val offset = codePoint - 0x10000
            val high = ((offset ushr 10) + 0xD800).toChar()
            val low = ((offset and 0x3FF) + 0xDC00).toChar()
            "$high$low"
        }
    }
}
