package com.semtinazizi.kitapduy.ui.screens

import com.semtinazizi.kitapduy.model.Book
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiscoveryCategoriesTest {

    private fun book(id: String, category: String) = Book(
        id = id,
        title = "Book $id",
        author = "Author $id",
        category = category,
        coverUrl = "https://example.com/$id.png",
        audioUrl = "https://example.com/$id.mp3"
    )

    @Test
    fun emptyBooksReturnsDefaultGenresAndCollectionsIncludingTurkishAndEnglish() {
        val (genres, collections) = buildActiveDiscoveryCategories(emptyList())
        val genreTitles = genres.map { it.title }
        val collectionTitles = collections.map { it.title }

        assertTrue("Roman" in genreTitles)
        assertTrue("Türk Edebiyatı" in genreTitles)
        assertTrue("Kişisel Gelişim" in genreTitles)
        assertTrue("İngilizce Kitaplar" in genreTitles)
        assertEquals(listOf("Dünya Edebiyatı", "Klasik"), collectionTitles)
    }

    @Test
    fun nonEmptyBooksFilterKnownCategoriesExcludePodcastAndAppendUnknown() {
        val books = listOf(
            book("1", "Roman"),
            book("2", "Türk Edebiyatı"),
            book("3", "Kişisel Gelişim"),
            book("4", "İngilizce Kitaplar"),
            book("5", "Dünya Edebiyatı"),
            book("6", "Podcast"),
            book("7", "   "),
            book("8", "Bilim Kurgu"),
            book("9", "Biyografi")
        )

        val (genres, collections) = buildActiveDiscoveryCategories(books)
        assertEquals(
            listOf("Roman", "Bilim Kurgu", "Kişisel Gelişim", "Türk Edebiyatı", "İngilizce Kitaplar", "Biyografi"),
            genres.map { it.title }
        )
        assertEquals(listOf("Dünya Edebiyatı"), collections.map { it.title })
        assertFalse(genres.any { it.title.equals("Podcast", ignoreCase = true) })
        assertFalse(genres.any { it.title.isBlank() })
    }

    @Test
    fun categoryMatchingHandlesWhitespaceAndTurkishCaseFolding() {
        val books = listOf(
            book("1", "  türk edebiyatı "),
            book("2", "İNGİLİZCE KİTAPLAR"),
            book("3", "ingilizce kitaplar"),
            book("4", "I\u0307ngilizce Kitaplar"),
            book("5", "i\u0307ngilizce kitaplar")
        )

        val (genres, collections) = buildActiveDiscoveryCategories(books)
        assertEquals(listOf("Türk Edebiyatı", "İngilizce Kitaplar"), genres.map { it.title })
        assertTrue(collections.isEmpty())

        assertTrue(categoryMatches("  türk edebiyatı ", "Türk Edebiyatı"))
        assertTrue(categoryMatches("İNGİLİZCE KİTAPLAR", "İngilizce Kitaplar"))
        assertTrue(categoryMatches("ingilizce kitaplar", "İngilizce Kitaplar"))
        assertTrue(categoryMatches("I\u0307ngilizce Kitaplar", "İngilizce Kitaplar"))
        assertTrue(categoryMatches("i\u0307ngilizce kitaplar", "İngilizce Kitaplar"))
        assertTrue(categoryMatches("kİşİsel gelİşİm", "Kişisel Gelişim"))
        assertTrue(categoryMatches("KİŞİSEL GELİŞİM", "Kişisel Gelişim"))
        assertFalse(categoryMatches("Podcast", "Roman"))
        assertFalse(categoryMatches("", "Roman"))
    }

    @Test
    fun booksWithOnlyPodcastAndBlankCategoriesFallBackToDefaults() {
        val books = listOf(
            book("1", "Podcast"),
            book("2", "  ")
        )
        val (genres, collections) = buildActiveDiscoveryCategories(books)
        assertEquals(genreCategories, genres)
        assertEquals(collectionCategories, collections)
    }

    @Test
    fun tagsContributeToDiscoveryCategoriesAndMatching() {
        val taggedBook = book("1", "Roman").copy(tags = listOf("Polisiye", "Klasik", "öykü"))
        val (genres, collections) = buildActiveDiscoveryCategories(listOf(taggedBook))

        assertEquals(listOf("Roman", "Polisiye", "Hikâye"), genres.map { it.title })
        assertEquals(listOf("Klasik"), collections.map { it.title })
        assertTrue(bookMatchesCategory(taggedBook, "Polisiye"))
        assertTrue(bookMatchesCategory(taggedBook, "Hikâye"))
        assertTrue(bookMatchesCategory(taggedBook, "Klasik"))
        assertTrue(bookMatchesCategory(taggedBook, "Tümü"))
        assertFalse(bookMatchesCategory(taggedBook, "Bilim"))
    }
}
