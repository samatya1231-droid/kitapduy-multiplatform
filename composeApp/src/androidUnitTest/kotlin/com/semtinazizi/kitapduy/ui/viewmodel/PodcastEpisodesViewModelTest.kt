package com.semtinazizi.kitapduy.ui.viewmodel

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class PodcastEpisodesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun parsesMoreThan100EpisodesUncappedWithEntityUnescapingAndHtmlCleaning() {
        val itemsXml = (1..135).joinToString("\n") { i ->
            """
            <item>
                <title><![CDATA[Bölüm $i &amp; Özel]]></title>
                <description><![CDATA[<p>Açıklama <b>#$i</b><br/>Detay &amp; notlar</p>]]></description>
                <enclosure url="https://cdn.example.com/audio/ep$i.mp3?token=abc&amp;sig=xyz$i" type="audio/mpeg" />
                <itunes:duration>${600 + i}</itunes:duration>
                <pubDate>Wed, 14 May 2025 18:00:00 +0300</pubDate>
            </item>
            """.trimIndent()
        }
        val rss = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
              <channel>
                <title>Meksika Açmazı</title>
                <itunes:image href="https://cdn.example.com/cover.jpg?w=600&amp;q=80" />
                $itemsXml
              </channel>
            </rss>
        """.trimIndent()

        val episodes = PodcastEpisodesViewModel.parseEpisodesXml(rss, podcastId = "meksika")
        assertEquals(135, episodes.size)
        val first = episodes.first()
        val last = episodes.last()

        assertEquals("Bölüm 1 & Özel", first.title)
        assertEquals("https://cdn.example.com/audio/ep1.mp3?token=abc&sig=xyz1", first.audioUrl)
        assertEquals("https://cdn.example.com/cover.jpg?w=600&q=80", first.imageUrl)
        assertFalse(first.description.contains("<p>"))
        assertFalse(first.description.contains("<b>"))
        assertTrue(first.description.contains("Açıklama #1"))
        assertTrue(first.description.contains("Detay & notlar"))
        assertEquals("601", first.duration)
        assertEquals("Wed, 14 May 2025 18:00:00 +0300", first.pubDate)
        assertEquals("Bölüm 135 & Özel", last.title)

        // Candidate audio hashes must match both legacy raw (&amp;) and decoded (&) URLs.
        val hashes = PodcastEpisodesViewModel.extractCandidateAudioHashes(rss)
        val ep1Hashes = hashes["https://cdn.example.com/audio/ep1.mp3?token=abc&sig=xyz1"]
        assertNotNull(ep1Hashes)
        assertTrue("https://cdn.example.com/audio/ep1.mp3?token=abc&amp;sig=xyz1".hashCode() in ep1Hashes)
        assertTrue("https://cdn.example.com/audio/ep1.mp3?token=abc&sig=xyz1".hashCode() in ep1Hashes)
    }

    @Test
    fun parsesAtomFeedEntriesAndItunesSummaryFallback() {
        val atom = """
            <feed xmlns="http://www.w3.org/2005/Atom" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
              <entry xml:lang="tr">
                <title>Atom Bölümü</title>
                <itunes:summary>Özet metni</itunes:summary>
                <link rel="enclosure" type="audio/mpeg" href="https://cdn.example.com/atom.mp3?a=1&amp;b=2" />
                <published>2026-11-21T10:00:00Z</published>
              </entry>
            </feed>
        """.trimIndent()

        val episodes = PodcastEpisodesViewModel.parseEpisodesXml(atom, podcastId = "atom_pod")
        assertEquals(1, episodes.size)
        assertEquals("Atom Bölümü", episodes.single().title)
        assertEquals("Özet metni", episodes.single().description)
        assertEquals("https://cdn.example.com/atom.mp3?a=1&b=2", episodes.single().audioUrl)
        assertEquals("2026-11-21T10:00:00Z", episodes.single().pubDate)
    }

    @Test
    fun loadEpisodesSurfacesHttpErrorAndRecoversOnForceRefresh() = runTest(testDispatcher) {
        var failRequest = true
        val client = HttpClient(MockEngine.create {
            dispatcher = testDispatcher
            addHandler { request ->
                assertTrue(request.headers[HttpHeaders.UserAgent]?.contains("KitapDuy") == true)
                if (failRequest) {
                    respond("Not Found", HttpStatusCode.NotFound, headersOf(HttpHeaders.ContentType, "text/plain"))
                } else {
                    respond(
                        """
                        <rss version="2.0"><channel><item>
                          <title>Geri Gelen Bölüm</title>
                          <enclosure url="https://cdn.example.com/recovered.mp3" type="audio/mpeg" />
                        </item></channel></rss>
                        """.trimIndent(),
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/rss+xml")
                    )
                }
            }
        })

        try {
            val vm = PodcastEpisodesViewModel(client, ioDispatcher = testDispatcher)
            vm.loadEpisodes("https://feeds.example.com/pod.xml", podcastId = "p1")
            advanceUntilIdle()

            assertFalse(vm.isLoading.value)
            assertTrue(vm.episodes.value.isEmpty())
            assertNotNull(vm.error.value)
            assertTrue(vm.error.value!!.contains("404"))

            failRequest = false
            vm.loadEpisodes("https://feeds.example.com/pod.xml", podcastId = "p1", forceRefresh = true)
            advanceUntilIdle()

            assertNull(vm.error.value)
            assertEquals(listOf("Geri Gelen Bölüm"), vm.episodes.value.map { it.title })
        } finally {
            client.close()
        }
    }

    @Test
    fun loadEpisodesSurfacesErrorForNonRssHtmlResponseAndBlankUrl() = runTest(testDispatcher) {
        val client = HttpClient(MockEngine.create {
            dispatcher = testDispatcher
            addHandler {
                respond("<html><body>503 Service Temporarily Unavailable</body></html>", HttpStatusCode.OK)
            }
        })

        try {
            val vm = PodcastEpisodesViewModel(client, ioDispatcher = testDispatcher)
            vm.loadEpisodes("   ", podcastId = "p_blank")
            assertNotNull(vm.error.value)
            assertFalse(vm.isLoading.value)

            vm.loadEpisodes("https://feeds.example.com/html-page", podcastId = "p_html")
            advanceUntilIdle()

            assertTrue(vm.episodes.value.isEmpty())
            assertNotNull(vm.error.value)
            assertFalse(vm.isLoading.value)
        } finally {
            client.close()
        }
    }

    @Test
    fun switchingPodcastsCancelsPreviousRequestAndClearsStaleEpisodes() = runTest(testDispatcher) {
        val slowGate = CompletableDeferred<Unit>()
        val client = HttpClient(MockEngine.create {
            dispatcher = testDispatcher
            addHandler { request ->
                if (request.url.encodedPath.contains("slow")) {
                    slowGate.await()
                    respond(
                        """<rss><channel><item><title>Yavaş Bölüm</title><enclosure url="https://cdn.example.com/slow.mp3"/></item></channel></rss>""",
                        HttpStatusCode.OK
                    )
                } else {
                    respond(
                        """<rss><channel><item><title>Hızlı Bölüm</title><enclosure url="https://cdn.example.com/fast.mp3"/></item></channel></rss>""",
                        HttpStatusCode.OK
                    )
                }
            }
        })

        try {
            val vm = PodcastEpisodesViewModel(client, ioDispatcher = testDispatcher)
            vm.loadEpisodes("https://feeds.example.com/slow.xml", podcastId = "slow")
            testDispatcher.scheduler.runCurrent()
            assertTrue(vm.isLoading.value)

            // Switch to fast podcast while slow request is still pending.
            vm.loadEpisodes("https://feeds.example.com/fast.xml", podcastId = "fast")
            advanceUntilIdle()
            assertEquals(listOf("Hızlı Bölüm"), vm.episodes.value.map { it.title })

            // Even if the slow gate completes later, cancelled job must not overwrite episodes.
            slowGate.complete(Unit)
            advanceUntilIdle()
            assertEquals(listOf("Hızlı Bölüm"), vm.episodes.value.map { it.title })
        } finally {
            client.close()
        }
    }
}
