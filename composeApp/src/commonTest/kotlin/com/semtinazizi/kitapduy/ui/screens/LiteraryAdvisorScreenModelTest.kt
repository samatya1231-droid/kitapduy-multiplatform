package com.semtinazizi.kitapduy.ui.screens

import com.semtinazizi.kitapduy.model.Book
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LiteraryAdvisorScreenModelTest {
    private val sampleCatalog = listOf(
        Book(
            id = "0mVxipNVFmBxtJNs9O0J",
            title = "Sherlock Holmes - Kızıl Dosya",
            author = "Arthur Conan Doyle",
            category = "Polisiye",
            description = "Sherlock Holmes ve Dr. Watson'ın ilk ortak soruşturması."
        ),
        Book(
            id = "suc_ve_ceza",
            title = "Suç ve Ceza",
            author = "Fyodor Dostoyevski",
            category = "Klasikler",
            description = "Raskolnikov'un vicdan hesaplaşması."
        )
    )

    @Test
    fun submitPromptUpdatesHistoryHidesDiscoveryAndStartNewChatResets() {
        val model = LiteraryAdvisorScreenModel()
        assertTrue(model.showDiscovery)
        assertTrue(model.messages.isEmpty())

        model.currentInput = "Kızıl Dosya'yı önerir misin?"
        model.spoilerOverride = true
        val acceptedBlank = model.submitPrompt("   ", null, sampleCatalog)
        assertFalse(acceptedBlank)
        assertTrue(model.messages.isEmpty())
        assertTrue(model.showDiscovery)

        val accepted = model.submitPrompt(model.currentInput, null, sampleCatalog)
        assertTrue(accepted)
        assertFalse(model.showDiscovery)
        assertEquals(2, model.messages.size)
        assertEquals(AdvisorSender.USER, model.messages[0].sender)
        assertEquals(AdvisorSender.ADVISOR, model.messages[1].sender)
        assertTrue(model.messages[1].content.isNotBlank())

        val acceptedFollowUp = model.submitPrompt("Suç ve Ceza hakkında ne dersin?", null, sampleCatalog)
        assertTrue(acceptedFollowUp)
        assertEquals(4, model.messages.size)

        model.readingMessage = model.messages.last()
        model.startNewChat()
        assertTrue(model.showDiscovery)
        assertTrue(model.messages.isEmpty())
        assertNull(model.readingMessage)
    }

    @Test
    fun parseBasicMarkdownStripsBoldItalicsAndTagsWithoutLeakingDelimiters() {
        val raw = "Bu bölümde **Suç ve Ceza** ile *Kızıl Dosya* önerilir. [BOOK_REC:0mVxipNVFmBxtJNs9O0J]"
        val parsed = parseBasicMarkdown(raw)
        assertEquals("Bu bölümde Suç ve Ceza ile Kızıl Dosya önerilir.", parsed.text)
        assertFalse(parsed.text.contains("*"))
        assertFalse(parsed.text.contains("BOOK_REC"))

        val unmatched = parseBasicMarkdown("Puan: 5 * 2 = 10")
        assertEquals("Puan: 5 * 2 = 10", unmatched.text)

        val advisorOutput = parseBasicMarkdown(
            "\"**Sherlock Holmes - Kızıl Dosya**\" (*Arthur Conan Doyle*)\n\n" +
                "*(Spoilersız mod: Olay örgüsünün sonunu ve sürpriz düğümleri özellikle saklı tuttum.)*"
        )
        assertEquals(
            "\"Sherlock Holmes - Kızıl Dosya\" (Arthur Conan Doyle)\n\n" +
                "(Spoilersız mod: Olay örgüsünün sonunu ve sürpriz düğümleri özellikle saklı tuttum.)",
            advisorOutput.text
        )
        assertFalse(advisorOutput.text.contains("*"))
    }

    @Test
    fun initialPromptHandledRemainsSetAcrossNewChat() {
        val model = LiteraryAdvisorScreenModel()
        assertFalse(model.initialPromptHandled)
        model.initialPromptHandled = true
        model.submitPrompt("Gizemli bir roman öner", "recommendation", sampleCatalog)
        model.startNewChat()
        assertTrue(model.initialPromptHandled)
        assertTrue(model.messages.isEmpty())
    }

    @Test
    fun namedWorkQuestionsStayOnTheAskedWorkBeforeAndAfterTheCatalogLoads() {
        val model = LiteraryAdvisorScreenModel()
        val orwell = Book(id = "animal_farm", title = "Hayvan Çiftliği", author = "George Orwell", viewCount = 9999)
        val sabahattinAli = Book(id = "seytan", title = "İçimizdeki Şeytan", author = "Sabahattin Ali")

        model.submitPrompt("icimizdeki seytani anlat", null, listOf(orwell))
        val brief = model.messages.last()
        assertTrue(brief.content.contains("Sabahattin Ali"))
        assertTrue(brief.content.contains("Macide"))
        assertTrue(brief.recommendedBooks.isEmpty())

        model.submitPrompt("İçimizdeki Şeytan'ı anlat", null, listOf(orwell, sabahattinAli))
        assertEquals(listOf(sabahattinAli), model.messages.last().recommendedBooks)
        model.submitPrompt("Bu eserdeki temayı inceleyelim", null, listOf(orwell, sabahattinAli))
        assertEquals("İçimizdeki Şeytan", model.messages.last().analyzedTitle)

        model.submitPrompt("Şeker Portakalı'nı anlat", null, listOf(orwell))
        assertFalse(model.messages.last().content.contains(orwell.title))
        assertTrue(model.messages.last().recommendedBooks.isEmpty())
    }
}
