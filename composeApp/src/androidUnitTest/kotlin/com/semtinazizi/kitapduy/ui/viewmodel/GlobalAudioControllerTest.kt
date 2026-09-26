package com.semtinazizi.kitapduy.ui.viewmodel

import com.russhwolf.settings.Settings
import com.semtinazizi.kitapduy.model.Book
import com.semtinazizi.kitapduy.platform.*
import com.semtinazizi.kitapduy.util.KmpBookProgressManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import java.lang.reflect.Proxy
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class GlobalAudioControllerTest {
    private fun progress(): KmpBookProgressManager {
        val values = mutableMapOf<String, Any>()
        val settings = Proxy.newProxyInstance(Settings::class.java.classLoader, arrayOf(Settings::class.java)) { _, method, args ->
            val key = args?.firstOrNull() as? String
            when {
                method.name.startsWith("put") -> { values[key!!] = args!![1]; Unit }
                method.name.startsWith("get") && key != null -> values[key] ?: args?.getOrNull(1)
                method.name == "hasKey" -> values.containsKey(key)
                method.name == "remove" -> { values.remove(key); Unit }
                method.name == "clear" -> { values.clear(); Unit }
                method.name == "getSize" -> values.size
                method.name == "getKeys" -> values.keys
                else -> null
            }
        } as Settings
        return KmpBookProgressManager(settings)
    }
    private val book = Book(id = "a", title = "A", audioUrl = "https://example.com/a.mp3")

    @Test fun freePlaybackRestoresBookPositionAfterPreroll() = runTest {
        val player = FakePlayer(); val progress = progress()
        progress.saveProgress("a", 42_000)
        val controller = GlobalAudioController(player, FakeDownloader(), progress, backgroundScope)
        controller.playTrack(book)
        assertTrue(player.initialized)
        assertEquals(listOf("kitapduy_preroll_ad", "a", "kitapduy_postroll_ad"), player.items.map { it.id })
        assertEquals(42_000, player.items[1].startPositionMs)
        player.position = 8_000 // Advertisement progress must not overwrite the book.
        runCurrent()
        assertEquals(42_000, progress.getProgress("a"))
        player.index = 1; player.position = 50_000
        advanceTimeBy(501)
        assertEquals(50_000, progress.getProgress("a"))
    }

    @Test fun offlinePlaybackSkipsAdsAndRetainsMetadata() = runTest {
        val player = FakePlayer(); val files = FakeDownloader().apply { local = "file:///a.mp3" }
        val controller = GlobalAudioController(player, files, progress(), backgroundScope)
        controller.playTrack(book)
        assertEquals(1, player.items.size)
        assertEquals("file:///a.mp3", player.items.single().url)
        assertEquals("A", player.items.single().title)
    }

    @Test fun downloadTargetsSelectedBookInsteadOfPlayingBook() = runTest {
        val player = FakePlayer(); val files = FakeDownloader()
        val controller = GlobalAudioController(player, files, progress(), backgroundScope)
        controller.playTrack(book, true)
        var success = false
        controller.downloadBook(book.copy(id = "b", audioUrl = "https://example.com/b.mp3")) { success = it }
        runCurrent()
        assertTrue(success)
        assertEquals("b.mp3", files.requestedFile)
        assertEquals("a", controller.currentBook.value?.id)
    }

    @Test fun downloadDoesNotRequirePlaybackAndReportsFailure() = runTest {
        val files = FakeDownloader().apply { fail = true }
        val controller = GlobalAudioController(FakePlayer(), files, progress(), backgroundScope)
        var result: Boolean? = null
        controller.downloadBook(book) { result = it }
        runCurrent()
        assertEquals(false, result)
        assertFalse(controller.isDownloading.value)
        assertNull(controller.currentBook.value)
    }

    @Test fun remotePauseAndTrackEndUpdateVisibleState() = runTest {
        val player = FakePlayer()
        val controller = GlobalAudioController(player, FakeDownloader(), progress(), backgroundScope)
        controller.playTrack(book, true)
        player.playing = false
        runCurrent()
        assertFalse(controller.isPlaying.value)
    }

    @Test fun sleepTimerStopsPlaybackAndCanBeCancelled() = runTest {
        val player = FakePlayer()
        val controller = GlobalAudioController(player, FakeDownloader(), progress(), backgroundScope)
        controller.playTrack(book, true)
        controller.startSleepTimer(1)
        advanceTimeBy(60_001)
        assertFalse(player.playing)
        controller.togglePlayPause()
        controller.startSleepTimer(1)
        controller.cancelSleepTimer()
        advanceTimeBy(60_001)
        assertTrue(player.playing)
    }
}

private class FakeDownloader : FileDownloader {
    var requestedFile: String? = null
    var local: String? = null
    var fail = false
    override suspend fun downloadFile(url: String, fileName: String, onProgress: (Float) -> Unit): String {
        requestedFile = fileName
        if (fail) error("Network unavailable")
        return "file:///" + fileName
    }
    override fun getDownloadedFilePath(fileName: String) = local
    override fun deleteFile(fileName: String) = true
}

private class FakePlayer : AudioPlayer {
    var initialized = false
    var items = emptyList<AudioMediaItem>()
    var index = 0
    var position = 0L
    var playing = false
    override fun initialize() { initialized = true }
    override fun play(url: String) {}
    override fun playQueue(items: List<AudioMediaItem>, startIndex: Int, startPositionMs: Long) {
        this.items = items; index = startIndex; playing = true
        position = if (startPositionMs > 0) startPositionMs else items[index].startPositionMs
    }
    override fun currentMediaId() = items.getOrNull(index)?.id
    override fun pause() { playing = false }
    override fun resume() { playing = true }
    override fun stop() { playing = false }
    override fun seekTo(positionMs: Long) { position = positionMs }
    override fun getCurrentPosition() = position
    override fun getDuration() = 100_000L
    override fun isPlaying() = playing
    override fun setPlaybackSpeed(speed: Float) {}
    override fun playAtmosphere(url: String, volume: Float) {}
    override fun stopAtmosphere() {}
    override fun setAtmosphereVolume(volume: Float) {}
    override fun release() {}
}
