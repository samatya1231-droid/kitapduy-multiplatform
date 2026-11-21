package com.semtinazizi.kitapduy.util

object PlaybackTimeFormatter {
    private val RFC2822_DATE_REGEX = Regex("(\\d{1,2})\\s+([A-Za-z]{3})\\s+(\\d{4})")
    private val ISO_DATE_REGEX = Regex("(\\d{4})-(\\d{2})-(\\d{2})")

    private val TURKISH_MONTHS_BY_ABBR = mapOf(
        "jan" to "Ocak",
        "feb" to "Şubat",
        "mar" to "Mart",
        "apr" to "Nisan",
        "may" to "Mayıs",
        "jun" to "Haziran",
        "jul" to "Temmuz",
        "aug" to "Ağustos",
        "sep" to "Eylül",
        "oct" to "Ekim",
        "nov" to "Kasım",
        "dec" to "Aralık"
    )

    private val TURKISH_MONTHS_BY_NUMBER = mapOf(
        1 to "Ocak",
        2 to "Şubat",
        3 to "Mart",
        4 to "Nisan",
        5 to "Mayıs",
        6 to "Haziran",
        7 to "Temmuz",
        8 to "Ağustos",
        9 to "Eylül",
        10 to "Ekim",
        11 to "Kasım",
        12 to "Aralık"
    )

    fun formatDurationMs(milliseconds: Long, placeholderWhenZero: Boolean = false): String {
        if (milliseconds <= 0L) {
            return if (placeholderWhenZero) "--:--" else "00:00"
        }
        val totalSeconds = milliseconds / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        } else {
            "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        }
    }

    fun parseRssDurationToMs(raw: String?): Long {
        val clean = raw?.trim().orEmpty()
        if (clean.isEmpty()) return 0L

        if (clean.contains(':')) {
            val parts = clean.split(':').map { it.trim().toLongOrNull() }
            if (parts.any { it == null || it < 0L }) return 0L
            return when (parts.size) {
                2 -> {
                    val mins = parts[0]!!
                    val secs = parts[1]!!
                    (mins * 60L + secs) * 1000L
                }
                3 -> {
                    val hours = parts[0]!!
                    val mins = parts[1]!!
                    val secs = parts[2]!!
                    (hours * 3600L + mins * 60L + secs) * 1000L
                }
                else -> 0L
            }
        }

        val rawSeconds = clean.toDoubleOrNull()?.toLong() ?: return 0L
        return if (rawSeconds > 0L) rawSeconds * 1000L else 0L
    }

    fun formatRssDuration(raw: String?, emptyPlaceholder: String = "--:--"): String {
        val ms = parseRssDurationToMs(raw)
        if (ms > 0L) return formatDurationMs(ms)
        val clean = raw?.trim().orEmpty()
        return clean.ifBlank { emptyPlaceholder }
    }

    fun formatPubDateTr(raw: String?, fallback: String = "Tarih Yok"): String {
        val clean = raw?.trim().orEmpty()
        if (clean.isEmpty()) return fallback

        val rfcMatch = RFC2822_DATE_REGEX.find(clean)
        if (rfcMatch != null) {
            val day = rfcMatch.groupValues[1].toIntOrNull()?.toString() ?: rfcMatch.groupValues[1]
            val monthKey = rfcMatch.groupValues[2].lowercase()
            val year = rfcMatch.groupValues[3]
            val monthTr = TURKISH_MONTHS_BY_ABBR[monthKey] ?: rfcMatch.groupValues[2]
            return "$day $monthTr $year"
        }

        val isoMatch = ISO_DATE_REGEX.find(clean)
        if (isoMatch != null) {
            val year = isoMatch.groupValues[1]
            val monthNum = isoMatch.groupValues[2].toIntOrNull()
            val day = isoMatch.groupValues[3].toIntOrNull()?.toString() ?: isoMatch.groupValues[3]
            val monthTr = monthNum?.let { TURKISH_MONTHS_BY_NUMBER[it] }
            if (monthTr != null) return "$day $monthTr $year"
        }

        return clean.take(16)
    }
}
