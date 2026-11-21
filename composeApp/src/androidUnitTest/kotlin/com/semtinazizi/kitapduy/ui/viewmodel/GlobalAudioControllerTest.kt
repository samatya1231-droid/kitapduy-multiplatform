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

    @Test fun staleBookScreenCannotSeekNewlyPlayingBook() = runTest {
        val player = FakePlayer()
        val controller = GlobalAudioController(player, FakeDownloader(), progress(), backgroundScope)
        controller.playTrack(book.copy(id = "b"), true)
        player.position = 20_000
        controller.seekTo(0.8f, expectedBookId = "a")
        controller.skipBy(15_000, expectedBookId = "a")
        assertEquals(20_000, player.position)
    }

    @Test fun bookAndPodcastWithSameIdAreNotInterchangeable() = runTest {
        val player = FakePlayer()
        val controller = GlobalAudioController(player, FakeDownloader(), progress(), backgroundScope)
        controller.playPodcastEpisode(com.semtinazizi.kitapduy.model.PodcastEpisode(id = "a", audioUrl = "https://example.com/episode.mp3"))
        player.position = 20_000
        controller.seekTo(0.8f, expectedBookId = "a")
        assertEquals(20_000, player.position)
        controller.skipBy(15_000, expectedEpisodeId = "a")
        assertEquals(35_000, player.position)
    }

    @Test fun repeatedSkipUsesNativePositionAndClampsAtBothEnds() = runTest {
        val player = FakePlayer()
        val controller = GlobalAudioController(player, FakeDownloader(), progress(), backgroundScope)
        controller.playTrack(book, true)
        player.position = 70_000
        repeat(3) { controller.skipBy(15_000, expectedBookId = "a") }
        assertEquals(100_000, player.position)
        repeat(8) { controller.skipBy(-15_000, expectedBookId = "a") }
        assertEquals(0, player.position)
    }

    @Test fun seekRejectsInvalidValuesAndPreroll() = runTest {
        val player = FakePlayer()
        val controller = GlobalAudioController(player, FakeDownloader(), progress(), backgroundScope)
        controller.playTrack(book)
        player.position = 3_000
        controller.skipBy(15_000, expectedBookId = "a")
        controller.seekTo(0.5f, expectedBookId = "a")
        assertEquals(3_000, player.position)
        player.index = 1
        controller.seekTo(Float.NaN, expectedBookId = "a")
        assertEquals(3_000, player.position)
        controller.seekTo(2f, expectedBookId = "a")
        assertEquals(100_000, player.position)
        assertEquals(1f, controller.currentPosition.value)
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

    @Test fun fullPlayerVisibilityHandlesNestedTokensAndPodcastTitleMetadata() = runTest {
        val player = FakePlayer()
        val controller = GlobalAudioController(player, FakeDownloader(), progress(), backgroundScope)
        assertFalse(controller.isFullPlayerVisible.value)

        val tokenA = Any()
        val tokenB = Any()
        controller.setFullPlayerVisible(true, tokenA)
        assertTrue(controller.isFullPlayerVisible.value)

        controller.setFullPlayerVisible(true, tokenB)
        controller.setFullPlayerVisible(false, tokenA)
        assertTrue(controller.isFullPlayerVisible.value)

        controller.setFullPlayerVisible(false, tokenB)
        assertFalse(controller.isFullPlayerVisible.value)

        controller.playPodcastEpisode(
            com.semtinazizi.kitapduy.model.PodcastEpisode(
                id = "ep1",
                title = "Bölüm 1",
                audioUrl = "https://example.com/ep1.mp3",
                imageUrl = "https://example.com/cover.png"
            ),
            podcastTitle = "Deneme Podcasti"
        )
        assertEquals("Deneme Podcasti", controller.currentPodcastTitle.value)
        assertEquals("Deneme Podcasti", player.items.single().artist)
        assertEquals("https://example.com/cover.png", player.items.single().artworkUrl)

        controller.playTrack(book, isPremium = true)
        assertNull(controller.currentPodcastTitle.value)
    }

    @Test fun ciSmokeTestSkipsAdsAndRestoresSavedPosition() = runTest {
        val player = FakePlayer(); val progress = progress()
        progress.saveProgress("a", 42_000)
        val controller = GlobalAudioController(
            audioPlayer = player,
            fileDownloader = FakeDownloader(),
            progressManager = progress,
            scope = backgroundScope,
            isCiSmokeTest = { true }
        )
        controller.playTrack(book, isPremium = false)
        assertTrue(player.initialized)
        assertEquals(listOf("a"), player.items.map { it.id })
        assertEquals(42_000, player.items.single().startPositionMs)
        player.position = 45_000
        advanceTimeBy(501)
        assertEquals(45_000, progress.getProgress("a"))
    }

    @Test fun authenticatedPlaybackEmitsListenPostSafelyAndSurvivesFeedErrors() = runTest {
        val created = mutableListOf<String>()
        val failingAuth = object : com.semtinazizi.kitapduy.domain.repository.AuthRepository by com.semtinazizi.kitapduy.data.repository.DummyAuthRepository() {
            override val currentUserUid: String? = "user-123"
            override suspend fun getUserProfile(uid: String) = com.semtinazizi.kitapduy.model.UserProfile(
                uid = uid, name = "Ayşe Okur", username = "ayse"
            )
        }
        val throwingFeed = object : com.semtinazizi.kitapduy.domain.repository.FeedRepository {
            override suspend fun getFeedItems(excludeListen: Boolean, limit: Long, offset: Long) = emptyList<com.semtinazizi.kitapduy.model.FeedItem>()
            override suspend fun createPost(
                userId: String, userName: String, userPhoto: String?, bookId: String, bookName: String,
                comment: String, isPremium: Boolean, imageUrl: String?, audioUrl: String?, pollOptions: List<String>?
            ): Boolean {
                created.add("$bookId:$comment")
                if (bookId == "fail-book") throw RuntimeException("Simulated network error")
                return true
            }
            override fun startRealtimeListening(onNewPost: (com.semtinazizi.kitapduy.model.FeedItem) -> Unit) {}
            override fun stopRealtimeListening() {}
        }

        val controller = GlobalAudioController(
            audioPlayer = FakePlayer(),
            fileDownloader = FakeDownloader(),
            progressManager = progress(),
            scope = backgroundScope,
            isCiSmokeTest = { false },
            feedRepository = throwingFeed,
            authRepository = failingAuth
        )

        controller.playTrack(book.copy(id = "fail-book"), isPremium = true)
        runCurrent()
        // Throwing feed repository must not crash playback or scope
        controller.playTrack(book, isPremium = true)
        controller.playTrack(book, isPremium = true) // same content deduped
        runCurrent()
        assertEquals(listOf("fail-book:Bu kitabı dinlemeye başladı \uD83C\uDFA7", "a:Bu kitabı dinlemeye başladı \uD83C\uDFA7"), created)
    }

    @Test fun ciSmokeModeAndGuestUsersDoNotEmitListenPosts() = runTest {
        var calls = 0
        val guestAuth = object : com.semtinazizi.kitapduy.domain.repository.AuthRepository by com.semtinazizi.kitapduy.data.repository.DummyAuthRepository() {
            override val currentUserUid: String? = "guest_999999"
        }
        val spyFeed = object : com.semtinazizi.kitapduy.domain.repository.FeedRepository {
            override suspend fun getFeedItems(excludeListen: Boolean, limit: Long, offset: Long) = emptyList<com.semtinazizi.kitapduy.model.FeedItem>()
            override suspend fun createPost(
                userId: String, userName: String, userPhoto: String?, bookId: String, bookName: String,
                comment: String, isPremium: Boolean, imageUrl: String?, audioUrl: String?, pollOptions: List<String>?
            ): Boolean {
                calls++
                return true
            }
            override fun startRealtimeListening(onNewPost: (com.semtinazizi.kitapduy.model.FeedItem) -> Unit) {}
            override fun stopRealtimeListening() {}
        }

        val guestController = GlobalAudioController(FakePlayer(), FakeDownloader(), progress(), backgroundScope, { false }, spyFeed, guestAuth)
        guestController.playTrack(book, isPremium = true)
        runCurrent()

        val ciController = GlobalAudioController(FakePlayer(), FakeDownloader(), progress(), backgroundScope, { true }, spyFeed, guestAuth)
        ciController.playTrack(book, isPremium = true)
        runCurrent()

        assertEquals(0, calls)
    }

    @Test fun exposesBufferingAdAndErrorStatesAndStopPlaybackClearsAllState() = runTest {
        val player = FakePlayer()
        val progress = progress()
        val controller = GlobalAudioController(player, FakeDownloader(), progress, backgroundScope)

        controller.playTrack(book, isPremium = false)
        assertTrue(controller.isAdPlaying.value)

        player.index = 1
        player.buffering = true
        player.playing = false
        advanceTimeBy(501)
        assertFalse(controller.isAdPlaying.value)
        assertTrue(controller.isBuffering.value)
        assertFalse(controller.isPlaying.value)

        // Tapping togglePlayPause while buffering pauses playback
        controller.togglePlayPause()
        assertFalse(player.buffering)
        assertFalse(controller.isBuffering.value)

        // Player errors surface and can be cleared or cleared on resume
        player.simulatedError = "Ses dosyası yüklenemedi"
        advanceTimeBy(501)
        assertEquals("Ses dosyası yüklenemedi", controller.playbackError.value)
        controller.clearPlaybackError()
        assertNull(controller.playbackError.value)

        // stopPlayback resets active item, progress and error state
        player.position = 30_000
        player.playing = true
        advanceTimeBy(501)
        controller.stopPlayback()
        assertNull(controller.currentBook.value)
        assertNull(controller.currentEpisode.value)
        assertNull(controller.currentPodcastTitle.value)
        assertFalse(controller.isPlaying.value)
        assertFalse(controller.isBuffering.value)
        assertFalse(controller.isAdPlaying.value)
        assertEquals(0f, controller.currentPosition.value)
        assertEquals(0f, controller.duration.value)
        assertEquals(30_000L, controller.savedPositionMsFor("a"))
    }

    @Test fun playPodcastEpisodePersistsListeningHistoryMetadataAndAudioUrl() = runTest {
        val player = FakePlayer()
        val progress = progress()
        val controller = GlobalAudioController(player, FakeDownloader(), progress, backgroundScope)
        val ep = com.semtinazizi.kitapduy.model.PodcastEpisode(
            id = "podcast_111hz_EP_42",
            title = "Bölüm 42",
            audioUrl = "https://cdn.example.com/ep42.mp3?token=1&sig=2",
            imageUrl = "https://cdn.example.com/cover.jpg"
        )
        controller.playPodcastEpisode(ep, podcastTitle = "111 Hz")
        val meta = progress.getContentMetadata(ep.id)
        assertNotNull(meta)
        assertEquals("Bölüm 42", meta.first)
        assertEquals("111 Hz", meta.second)
        assertEquals("https://cdn.example.com/cover.jpg", meta.third)
        assertEquals("https://cdn.example.com/ep42.mp3?token=1&sig=2", progress.getContentAudioUrl(ep.id))

        player.position = 18_000
        advanceTimeBy(501)
        assertEquals(18_000L, progress.getProgress(ep.id))
        assertTrue(progress.getHistoryIds().contains(ep.id))
    }

    @Test fun progressWritesAreThrottledDuringPlaybackZeroWhilePausedAndImmediateOnPauseOrSeek() = runTest {
        var progressWriteCount = 0
        val values = mutableMapOf<String, Any>()
        val countingSettings = Proxy.newProxyInstance(Settings::class.java.classLoader, arrayOf(Settings::class.java)) { _, method, args ->
            val key = args?.firstOrNull() as? String
            when {
                method.name.startsWith("put") && key != null && args.size >= 2 -> {
                    if (method.name == "putLong" && key == book.id) progressWriteCount++
                    values[key] = args[1]
                    Unit
                }
                method.name.startsWith("get") && key != null -> values[key] ?: args.getOrNull(1)
                method.name == "hasKey" -> values.containsKey(key)
                method.name == "remove" -> { values.remove(key); Unit }
                method.name == "clear" -> { values.clear(); Unit }
                method.name == "getSize" -> values.size
                method.name == "getKeys" -> values.keys
                else -> null
            }
        } as Settings

        val player = FakePlayer()
        val progress = KmpBookProgressManager(countingSettings)
        val controller = GlobalAudioController(player, FakeDownloader(), progress, backgroundScope)
        controller.playTrack(book, isPremium = true)

        // First poll at t=0 writes initial progress immediately
        player.position = 1_000
        runCurrent()
        assertEquals(1, progressWriteCount)

        // Next 9 ticks (4.5 seconds) while playing must NOT write on every 500ms poll
        repeat(9) { idx ->
            player.position = 2_000L + idx * 500L
            advanceTimeBy(500)
            runCurrent()
        }
        assertEquals(1, progressWriteCount)

        // 10th tick (5 seconds since last save) triggers throttled save
        player.position = 7_000L
        advanceTimeBy(500)
        runCurrent()
        assertEquals(2, progressWriteCount)
        assertEquals(7_000L, progress.getProgress("a"))

        // Pausing immediately saves latest position, and subsequent paused ticks perform 0 writes
        player.position = 8_500L
        controller.togglePlayPause()
        assertEquals(3, progressWriteCount)
        assertEquals(8_500L, progress.getProgress("a"))

        repeat(20) {
            advanceTimeBy(500)
        }
        assertEquals(3, progressWriteCount)

        // Seeking immediately persists new position
        controller.seekTo(0.5f, expectedBookId = "a")
        assertEquals(4, progressWriteCount)
        assertEquals(50_000L, progress.getProgress("a"))
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
    var buffering = false
    var simulatedError: String? = null
    override fun initialize() { initialized = true }
    override fun play(url: String) {}
    override fun playQueue(items: List<AudioMediaItem>, startIndex: Int, startPositionMs: Long) {
        this.items = items; index = startIndex; playing = true; buffering = false; simulatedError = null
        position = if (startPositionMs > 0) startPositionMs else items[index].startPositionMs
    }
    override fun currentMediaId() = items.getOrNull(index)?.id
    override fun pause() { playing = false; buffering = false }
    override fun resume() { playing = true; simulatedError = null }
    override fun stop() { playing = false; buffering = false; simulatedError = null; items = emptyList(); index = 0 }
    override fun seekTo(positionMs: Long) { position = positionMs }
    override fun getCurrentPosition() = position
    override fun getDuration() = 100_000L
    override fun isPlaying() = playing
    override fun isBuffering() = buffering
    override fun getPlaybackError() = simulatedError
    override fun clearPlaybackError() { simulatedError = null }
    override fun setPlaybackSpeed(speed: Float) {}
    override fun playAtmosphere(url: String, volume: Float) {}
    override fun stopAtmosphere() {}
    override fun setAtmosphereVolume(volume: Float) {}
    override fun release() {}
}
