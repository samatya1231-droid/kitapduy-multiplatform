package com.semtinazizi.kitapduy

import com.semtinazizi.kitapduy.util.UrlSanitizer
import org.junit.Test
import kotlin.test.assertEquals

class UrlEncodingTest {
    @Test
    fun testTurkishCharactersAndSpaces() {
        val raw = "https://pub-7327d7da4dcf4c1bbf6d53bacbbe276c.r2.dev/audiobooks/Sırça Köşk/01 - Başlangıç.mp3"
        val sanitized = UrlSanitizer.sanitizeUrl(raw)
        val expected = "https://pub-7327d7da4dcf4c1bbf6d53bacbbe276c.r2.dev/audiobooks/S%C4%B1r%C3%A7a%20K%C3%B6%C5%9Fk/01%20-%20Ba%C5%9Flang%C4%B1%C3%A7.mp3"
        assertEquals(expected, sanitized)
    }

    @Test
    fun testIdempotenceDoesNotDoubleEncode() {
        val alreadyEncoded = "https://pub-7327d7da4dcf4c1bbf6d53bacbbe276c.r2.dev/audiobooks/S%C4%B1r%C3%A7a%20K%C3%B6%C5%9Fk/01%20-%20Ba%C5%9Flang%C4%B1%C3%A7.mp3"
        val sanitized = UrlSanitizer.sanitizeUrl(alreadyEncoded)
        assertEquals(alreadyEncoded, sanitized)
    }

    @Test
    fun testAllTurkishCharactersUpperAndLower() {
        // ç, ğ, ı, ö, ş, ü and uppercase Ç, Ğ, İ, Ö, Ş, Ü
        val raw = "https://example.com/çğışöü_ÇĞİŞÖÜ"
        val sanitized = UrlSanitizer.sanitizeUrl(raw)
        val expected = "https://example.com/%C3%A7%C4%9F%C4%B1%C5%9F%C3%B6%C3%BC_%C3%87%C4%9E%C4%B0%C5%9E%C3%96%C3%9C"
        assertEquals(expected, sanitized)
    }

    @Test
    fun testUrlWithQueryAndDelimiters() {
        val queryUrl = "https://example.com/search?q=türkçe kitap&lang=tr#bölüm-1"
        val sanitized = UrlSanitizer.sanitizeUrl(queryUrl)
        val expected = "https://example.com/search?q=t%C3%BCrk%C3%A7e%20kitap&lang=tr#b%C3%B6l%C3%BCm-1"
        assertEquals(expected, sanitized)
    }

    @Test
    fun testLocalFileUri() {
        val local = "file:///var/mobile/Containers/Data/Application/123/Documents/audio_downloads/Sırça Köşk.mp3"
        val sanitized = UrlSanitizer.sanitizeUrl(local)
        val expected = "file:///var/mobile/Containers/Data/Application/123/Documents/audio_downloads/S%C4%B1r%C3%A7a%20K%C3%B6%C5%9Fk.mp3"
        assertEquals(expected, sanitized)
    }

    @Test
    fun testBackslashReplacement() {
        val withBackslashes = "audiobooks\\Sırça Köşk\\01.mp3"
        val sanitized = UrlSanitizer.sanitizeUrl(withBackslashes)
        val expected = "audiobooks/S%C4%B1r%C3%A7a%20K%C3%B6%C5%9Fk/01.mp3"
        assertEquals(expected, sanitized)
    }

    @Test
    fun testBlankAndEmpty() {
        assertEquals("", UrlSanitizer.sanitizeUrl(null))
        assertEquals("", UrlSanitizer.sanitizeUrl(""))
        assertEquals("", UrlSanitizer.sanitizeUrl("   "))
    }
}
