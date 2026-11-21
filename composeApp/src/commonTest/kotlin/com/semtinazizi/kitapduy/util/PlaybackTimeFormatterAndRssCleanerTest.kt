package com.semtinazizi.kitapduy.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackTimeFormatterAndRssCleanerTest {

    @Test
    fun formatDurationMsHandlesZeroNegativeSubHourAndMultiHourValues() {
        assertEquals("00:00", PlaybackTimeFormatter.formatDurationMs(0L))
        assertEquals("00:00", PlaybackTimeFormatter.formatDurationMs(-5_000L))
        assertEquals("--:--", PlaybackTimeFormatter.formatDurationMs(0L, placeholderWhenZero = true))
        assertEquals("--:--", PlaybackTimeFormatter.formatDurationMs(-1L, placeholderWhenZero = true))
        assertEquals("00:09", PlaybackTimeFormatter.formatDurationMs(9_000L))
        assertEquals("05:42", PlaybackTimeFormatter.formatDurationMs(342_000L))
        assertEquals("59:59", PlaybackTimeFormatter.formatDurationMs(3_599_000L))
        assertEquals("01:00:00", PlaybackTimeFormatter.formatDurationMs(3_600_000L))
        assertEquals("02:07:05", PlaybackTimeFormatter.formatDurationMs((2 * 3600 + 7 * 60 + 5) * 1000L))
    }

    @Test
    fun parseAndFormatRssDurationSupportsSecondsMmSsHhMmSsAndInvalidValues() {
        assertEquals(0L, PlaybackTimeFormatter.parseRssDurationToMs(null))
        assertEquals(0L, PlaybackTimeFormatter.parseRssDurationToMs(""))
        assertEquals(0L, PlaybackTimeFormatter.parseRssDurationToMs("-12"))
        assertEquals(0L, PlaybackTimeFormatter.parseRssDurationToMs("invalid"))
        assertEquals(90_000L, PlaybackTimeFormatter.parseRssDurationToMs("90"))
        assertEquals(90_000L, PlaybackTimeFormatter.parseRssDurationToMs("90.7"))
        assertEquals(2_730_000L, PlaybackTimeFormatter.parseRssDurationToMs("45:30"))
        assertEquals(754_000L, PlaybackTimeFormatter.parseRssDurationToMs("12:34.5"))
        assertEquals(5_415_000L, PlaybackTimeFormatter.parseRssDurationToMs("01:30:15"))
        assertEquals(3_723_000L, PlaybackTimeFormatter.parseRssDurationToMs("01:02:03.8"))
        assertEquals(0L, PlaybackTimeFormatter.parseRssDurationToMs("1:2:3:4"))

        assertEquals("--:--", PlaybackTimeFormatter.formatRssDuration(""))
        assertEquals("01:30", PlaybackTimeFormatter.formatRssDuration("90"))
        assertEquals("01:30:15", PlaybackTimeFormatter.formatRssDuration("1:30:15"))
        assertEquals("45:00", PlaybackTimeFormatter.formatRssDuration("2700"))
        assertEquals("Bilinmiyor", PlaybackTimeFormatter.formatRssDuration("Bilinmiyor"))
    }

    @Test
    fun formatPubDateTrFormatsRfc2822IsoAndFallbackStrings() {
        assertEquals("Tarih Yok", PlaybackTimeFormatter.formatPubDateTr(null))
        assertEquals("Tarih Yok", PlaybackTimeFormatter.formatPubDateTr("   "))
        assertEquals("14 Mayıs 2025", PlaybackTimeFormatter.formatPubDateTr("Wed, 14 May 2025 18:00:00 +0300"))
        assertEquals("3 Ocak 2026", PlaybackTimeFormatter.formatPubDateTr("Sat, 03 Jan 2026 09:30:00 GMT"))
        assertEquals("21 Kasım 2026", PlaybackTimeFormatter.formatPubDateTr("2026-11-21T10:15:00Z"))
        assertEquals("Özel Tarih", PlaybackTimeFormatter.formatPubDateTr("Özel Tarih"))
    }

    @Test
    fun decodeXmlEntitiesDecodesNamedDecimalHexAndEmojiWithoutDoubleDecoding() {
        assertEquals(
            "https://anchor.fm/s/123/podcast/play/ep.mp3?token=abc&expires=999&sig=xyz",
            RssTextCleaner.decodeXmlEntities(
                "https://anchor.fm/s/123/podcast/play/ep.mp3?token=abc&amp;expires=999&amp;sig=xyz"
            )
        )
        // Single-pass: &amp;lt; becomes &lt;, not <
        assertEquals("&lt;tag&gt;", RssTextCleaner.decodeXmlEntities("&amp;lt;tag&amp;gt;"))
        assertEquals("O'Connor & \"Dost\" — 🎧", RssTextCleaner.decodeXmlEntities("O&#39;Connor &#x26; &quot;Dost&quot; &mdash; &#x1F3A7;"))
        // Unknown or invalid entities remain intact
        assertEquals("A &unknown; &#99999999; B", RssTextCleaner.decodeXmlEntities("A &unknown; &#99999999; B"))
    }

    @Test
    fun cleanHtmlDescriptionStripsTagsScriptsStylesAndPreservesParagraphsAndLists() {
        val cdataHtml = """
            <![CDATA[
                <script>alert('xss')</script>
                <style>.hidden { display:none; }</style>
                <p>İlk paragraf &amp; <b>kalın</b> metin.<br/>Yeni satır.</p>
                <ul><li>Birinci madde</li><li>İkinci&nbsp;madde</li></ul>
            ]]>
        """.trimIndent()
        val cleaned = RssTextCleaner.cleanHtmlDescription(cdataHtml)
        assertFalse(cleaned.contains("<p>"))
        assertFalse(cleaned.contains("<b>"))
        assertFalse(cleaned.contains("alert("))
        assertFalse(cleaned.contains("display:none"))
        assertTrue(cleaned.contains("İlk paragraf & kalın metin."))
        assertTrue(cleaned.contains("Yeni satır."))
        assertTrue(cleaned.contains("• Birinci madde"))
        assertTrue(cleaned.contains("• İkinci madde"))

        // Non-CDATA XML-escaped HTML
        val escapedHtml = "&lt;p&gt;Bölüm özeti &amp;amp; notlar&lt;/p&gt;&lt;p&gt;Devamı&lt;/p&gt;"
        val cleanedEscaped = RssTextCleaner.cleanHtmlDescription(escapedHtml)
        assertEquals("Bölüm özeti & notlar\nDevamı", cleanedEscaped)
    }

    @Test
    fun cleanTitleUnwrapsCdataDecodesEntitiesAndStripsInlineTags() {
        assertEquals(
            "Meksika Açmazı #142 — \"Soru & Cevap\"",
            RssTextCleaner.cleanTitle("<![CDATA[<b>Meksika Açmazı</b> #142 &mdash; &quot;Soru &amp; Cevap&quot;]]>")
        )
        assertEquals("", RssTextCleaner.cleanTitle("   "))
    }
}
