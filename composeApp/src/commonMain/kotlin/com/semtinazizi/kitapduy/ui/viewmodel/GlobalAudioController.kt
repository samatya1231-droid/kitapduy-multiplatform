package com.semtinazizi.kitapduy.ui.viewmodel

import com.semtinazizi.kitapduy.data.repository.SupabaseFeedRepository
import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.domain.repository.FeedRepository
import com.semtinazizi.kitapduy.model.Book
import com.semtinazizi.kitapduy.platform.AudioMediaItem
import com.semtinazizi.kitapduy.platform.AudioPlayer
import com.semtinazizi.kitapduy.platform.FileDownloader
import com.semtinazizi.kitapduy.util.UrlSanitizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GlobalAudioController(
    private val audioPlayer: AudioPlayer,
    private val fileDownloader: FileDownloader,
    private val progressManager: com.semtinazizi.kitapduy.util.KmpBookProgressManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + kotlinx.coroutines.SupervisorJob()),
    private val isCiSmokeTest: () -> Boolean = { com.semtinazizi.kitapduy.platform.AppConfig.isCiSmokeTest },
    private val feedRepository: FeedRepository? = null,
    private val authRepository: AuthRepository? = null,
    private val resolveBundledAudio: (String, String) -> String? = { name, ext ->
        com.semtinazizi.kitapduy.platform.resolveBundledAudioUrl(name, ext)
    }
) {
    companion object {
        internal const val PREROLL_AD_ID = "kitapduy_preroll_ad"
        internal const val POSTROLL_AD_ID = "kitapduy_postroll_ad"
        internal const val PREROLL_RESOURCE = "kitapduy_tanitim"
        internal const val POSTROLL_RESOURCE = "kitapduy_tanitim_bitis"
        internal const val PROGRESS_SAVE_INTERVAL_TICKS = 10 // 10 * 500ms = 5s
        internal val ATMOSPHERE_RESOURCES = mapOf(
            "rain" to "rain_sound",
            "fire" to "fire_sound",
            "cafe" to "cafe_sound",
            "forest" to "forest_sound"
        )
    }

    private var sleepTimerJob: Job? = null
    private var quotePreviewJob: Job? = null
    private var quotePreviewClipDurationMs: Long = 15_000L
    private var quotePreviewObservedPlaying: Boolean = false
    private var currentBookIsPremium: Boolean = false
    private var seenActiveTrackForBookEnd: Boolean = false
    private var lastPostedListeningContentId: String? = null
    private var lastSavedContentId: String? = null
    private var lastSavedPositionMs: Long = -1L
    private var progressSaveTicks: Int = 0

    private val _currentBook = MutableStateFlow<Book?>(null)
    val currentBook: StateFlow<Book?> = _currentBook.asStateFlow()

    private val _currentEpisode = MutableStateFlow<com.semtinazizi.kitapduy.model.PodcastEpisode?>(null)
    val currentEpisode: StateFlow<com.semtinazizi.kitapduy.model.PodcastEpisode?> = _currentEpisode.asStateFlow()

    private val _currentPodcastTitle = MutableStateFlow<String?>(null)
    val currentPodcastTitle: StateFlow<String?> = _currentPodcastTitle.asStateFlow()

    private val _isFullPlayerVisible = MutableStateFlow(false)
    val isFullPlayerVisible: StateFlow<Boolean> = _isFullPlayerVisible.asStateFlow()
    private val fullPlayerTokens = mutableSetOf<Any>()

    fun setFullPlayerVisible(visible: Boolean, token: Any = Unit) {
        if (visible) {
            fullPlayerTokens.add(token)
        } else {
            fullPlayerTokens.remove(token)
        }
        _isFullPlayerVisible.value = fullPlayerTokens.isNotEmpty()
    }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _isAdPlaying = MutableStateFlow(false)
    val isAdPlaying: StateFlow<Boolean> = _isAdPlaying.asStateFlow()

    private val _isPostRollAd = MutableStateFlow(false)
    val isPostRollAd: StateFlow<Boolean> = _isPostRollAd.asStateFlow()

    private val _adRemainingSeconds = MutableStateFlow(0)
    val adRemainingSeconds: StateFlow<Int> = _adRemainingSeconds.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    private val _currentPosition = MutableStateFlow(0f)
    val currentPosition: StateFlow<Float> = _currentPosition.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _duration = MutableStateFlow(0f)
    val duration: StateFlow<Float> = _duration.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _currentAtmosphereId = MutableStateFlow<String?>(null)
    val currentAtmosphereId: StateFlow<String?> = _currentAtmosphereId.asStateFlow()

    private val _atmosphereVolume = MutableStateFlow(0.5f)
    val atmosphereVolume: StateFlow<Float> = _atmosphereVolume.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _sleepTimerRemainingMinutes = MutableStateFlow(0)
    val sleepTimerRemainingMinutes: StateFlow<Int> = _sleepTimerRemainingMinutes.asStateFlow()

    private val _sleepTimerRemainingSeconds = MutableStateFlow(0)
    val sleepTimerRemainingSeconds: StateFlow<Int> = _sleepTimerRemainingSeconds.asStateFlow()

    private val _sleepTimerAtBookEnd = MutableStateFlow(false)
    val sleepTimerAtBookEnd: StateFlow<Boolean> = _sleepTimerAtBookEnd.asStateFlow()

    private val _isQuotePreviewPlaying = MutableStateFlow(false)
    val isQuotePreviewPlaying: StateFlow<Boolean> = _isQuotePreviewPlaying.asStateFlow()

    private val _quotePreviewDurationMs = MutableStateFlow(0L)
    val quotePreviewDurationMs: StateFlow<Long> = _quotePreviewDurationMs.asStateFlow()

    init {
        audioPlayer.initialize()
        // Track position, buffering, ad state and errors periodically
        scope.launch {
            while (true) {
                syncPlaybackStates()
                val activeId = _currentBook.value?.id ?: _currentEpisode.value?.id
                if (activeId != null && audioPlayer.currentMediaId() == activeId) {
                    val pos = audioPlayer.getCurrentPosition().coerceAtLeast(0L)
                    val dur = audioPlayer.getDuration().coerceAtLeast(0L)
                    _currentPositionMs.value = pos
                    if (dur > 0L) {
                        _duration.value = dur.toFloat()
                        _currentPosition.value = (pos.toDouble() / dur.toDouble()).toFloat().coerceIn(0f, 1f)
                    }
                    if (dur > 0L || pos > 0L) {
                        saveProgress(force = false)
                    }
                }
                delay(500)
            }
        }
    }

    private fun syncPlaybackStates() {
        val activeId = _currentBook.value?.id ?: _currentEpisode.value?.id
        val mediaId = audioPlayer.currentMediaId()
        _isPlaying.value = audioPlayer.isPlaying()
        _isBuffering.value = audioPlayer.isBuffering()
        val adPlaying = activeId != null && (mediaId == PREROLL_AD_ID || mediaId == POSTROLL_AD_ID)
        _isAdPlaying.value = adPlaying
        _isPostRollAd.value = activeId != null && mediaId == POSTROLL_AD_ID
        if (adPlaying) {
            val dur = audioPlayer.getDuration().coerceAtLeast(0L)
            val pos = audioPlayer.getCurrentPosition().coerceAtLeast(0L)
            _adRemainingSeconds.value = if (dur > 0L) {
                (((dur - pos).coerceAtLeast(0L) + 999L) / 1000L).toInt()
            } else {
                0
            }
        } else {
            _adRemainingSeconds.value = 0
        }
        if (_sleepTimerAtBookEnd.value && activeId != null) {
            if (mediaId == activeId) {
                seenActiveTrackForBookEnd = true
            }
            if (seenActiveTrackForBookEnd && (mediaId == POSTROLL_AD_ID || audioPlayer.hasReachedTrackEnd())) {
                triggerSleepTimerExpiry()
                return
            }
        }
        if (_isQuotePreviewPlaying.value) {
            val previewDur = audioPlayer.getPreviewDuration().coerceAtLeast(0L)
            if (previewDur > 0L) {
                _quotePreviewDurationMs.value = previewDur
            }
            if (audioPlayer.isPreviewPlaying()) {
                quotePreviewObservedPlaying = true
            } else if (quotePreviewObservedPlaying) {
                stopQuotePreview()
            }
        }
        _playbackError.value = audioPlayer.getPlaybackError()
    }

    private fun resolvePromoAudioUrl(resourceName: String, fallbackFileName: String): String {
        return resolveBundledAudio(resourceName, "wav")
            ?: UrlSanitizer.normalizeMediaUrl("${UrlSanitizer.CUSTOM_R2_BASE_URL}/$fallbackFileName")
    }

    fun skipActiveAdIfPremium(isPremium: Boolean) {
        if (isPremium) currentBookIsPremium = true
        if (!isPremium || !_isAdPlaying.value) return
        val activeBook = _currentBook.value
        if (_isPostRollAd.value || activeBook == null) {
            audioPlayer.pause()
            _isAdPlaying.value = false
            _isPostRollAd.value = false
            _adRemainingSeconds.value = 0
            _isPlaying.value = false
            return
        }
        if (!audioPlayer.skipToMediaId(activeBook.id)) {
            playTrack(activeBook, isPremium = true)
        } else {
            syncPlaybackStates()
        }
    }

    private fun resetSaveThrottle() {
        lastSavedContentId = null
        lastSavedPositionMs = -1L
        progressSaveTicks = 0
    }

    fun clearPlaybackError() {
        audioPlayer.clearPlaybackError()
        _playbackError.value = null
    }

    fun stopPlayback() {
        saveProgress(force = true)
        sleepTimerJob?.cancel()
        seenActiveTrackForBookEnd = false
        _sleepTimerRemainingMinutes.value = 0
        _sleepTimerRemainingSeconds.value = 0
        _sleepTimerAtBookEnd.value = false
        stopQuotePreview()
        audioPlayer.stop()
        stopAtmosphere()
        audioPlayer.clearPlaybackError()
        resetSaveThrottle()
        _currentBook.value = null
        _currentEpisode.value = null
        _currentPodcastTitle.value = null
        _isPlaying.value = false
        _isBuffering.value = false
        _isAdPlaying.value = false
        _isPostRollAd.value = false
        _adRemainingSeconds.value = 0
        _playbackError.value = null
        _currentPosition.value = 0f
        _currentPositionMs.value = 0L
        _duration.value = 0f
    }

    fun savedPositionMsFor(contentId: String): Long {
        if (contentId.isBlank()) return 0L
        return progressManager.getProgress(contentId).coerceAtLeast(0L)
    }

    fun playTrack(book: Book, isPremium: Boolean = false, restartFromBeginning: Boolean = false) {
        saveProgress(force = true)
        resetSaveThrottle()
        seenActiveTrackForBookEnd = false
        currentBookIsPremium = isPremium
        audioPlayer.clearPlaybackError()
        _playbackError.value = null
        _currentBook.value = book
        _currentEpisode.value = null
        _currentPodcastTitle.value = null
        _currentPosition.value = 0f
        _currentPositionMs.value = 0L
        _duration.value = 0f
        progressManager.saveLastPlayedBook(book.id)
        val localPath = fileDownloader.getDownloadedFilePath(book.id + ".mp3")
        val playUrl = localPath ?: UrlSanitizer.normalizeMediaUrl(book.audioUrl)
        val normalizedCoverUrl = UrlSanitizer.normalizeMediaUrl(book.coverUrl)
        progressManager.saveContentMetadata(book.id, book.title, book.author, normalizedCoverUrl, playUrl)
        val startPositionMs = if (restartFromBeginning) 0L else progressManager.getProgress(book.id)

        if (!isPremium && localPath == null && !book.id.startsWith("local_") && !isCiSmokeTest()) {
            val preRollAd = AudioMediaItem(
                id = PREROLL_AD_ID,
                url = resolvePromoAudioUrl(PREROLL_RESOURCE, "kitapduy_tanitim.wav"),
                title = "KitapDuy Premium",
                artist = "Sponsorlu İçerik",
                artworkUrl = "https://firebasestorage.googleapis.com/v0/b/semtinazizi.appspot.com/o/kitapduy_logo.png?alt=media"
            )
            val postRollAd = AudioMediaItem(
                id = POSTROLL_AD_ID,
                url = resolvePromoAudioUrl(POSTROLL_RESOURCE, "kitapduy_tanitim_bitis.wav"),
                title = "KitapDuy Premium",
                artist = "Sponsorlu İçerik",
                artworkUrl = "https://firebasestorage.googleapis.com/v0/b/semtinazizi.appspot.com/o/kitapduy_logo.png?alt=media"
            )
            val mainItem = AudioMediaItem(
                id = book.id,
                url = playUrl,
                title = book.title,
                artist = book.author,
                artworkUrl = normalizedCoverUrl,
                startPositionMs = startPositionMs
            )
            audioPlayer.playQueue(listOf(preRollAd, mainItem, postRollAd))
        } else {
            audioPlayer.playQueue(
                listOf(
                    AudioMediaItem(
                        book.id,
                        playUrl,
                        book.title,
                        book.author,
                        normalizedCoverUrl,
                        startPositionMs
                    )
                )
            )
        }

        _playbackSpeed.value = 1.0f
        audioPlayer.setPlaybackSpeed(1.0f)
        syncPlaybackStates()
        emitListeningActivity(
            contentId = book.id,
            title = book.title,
            coverUrl = normalizedCoverUrl,
            audioUrl = book.audioUrl
        )
    }

    fun playPodcastEpisode(
        episode: com.semtinazizi.kitapduy.model.PodcastEpisode,
        podcastTitle: String = "",
        restartFromBeginning: Boolean = false
    ) {
        saveProgress(force = true)
        resetSaveThrottle()
        seenActiveTrackForBookEnd = false
        audioPlayer.clearPlaybackError()
        _playbackError.value = null
        val resolvedTitle = podcastTitle.trim().takeIf { it.isNotBlank() }
        val localPath = fileDownloader.getDownloadedFilePath(episode.id + ".mp3")
        val normalizedAudioUrl = UrlSanitizer.normalizeMediaUrl(episode.audioUrl)
        val playUrl = localPath ?: normalizedAudioUrl
        val normalizedImageUrl = episode.imageUrl?.let { UrlSanitizer.normalizeMediaUrl(it) }.orEmpty()
        val startPositionMs = if (restartFromBeginning) 0L else progressManager.getProgress(episode.id)
        _currentEpisode.value = episode
        _currentPodcastTitle.value = resolvedTitle
        _currentBook.value = null
        _currentPosition.value = 0f
        _currentPositionMs.value = 0L
        _duration.value = 0f
        progressManager.saveContentMetadata(
            id = episode.id,
            title = episode.title.ifBlank { resolvedTitle ?: "Podcast Bölümü" },
            author = resolvedTitle ?: "Podcast",
            coverUrl = normalizedImageUrl,
            audioUrl = normalizedAudioUrl
        )
        audioPlayer.playQueue(
            listOf(
                AudioMediaItem(
                    episode.id,
                    playUrl,
                    episode.title,
                    artist = resolvedTitle.orEmpty(),
                    artworkUrl = normalizedImageUrl,
                    startPositionMs = startPositionMs
                )
            )
        )
        _playbackSpeed.value = 1.0f
        audioPlayer.setPlaybackSpeed(1.0f)
        syncPlaybackStates()
        emitListeningActivity(
            contentId = episode.id,
            title = episode.title.ifBlank { resolvedTitle.orEmpty() },
            coverUrl = normalizedImageUrl,
            audioUrl = normalizedAudioUrl
        )
    }

    fun currentPositionMsFor(bookId: String): Long {
        return if (matchesPlayingContent(expectedBookId = bookId, expectedEpisodeId = null)) {
            audioPlayer.getCurrentPosition().coerceAtLeast(0L)
        } else {
            progressManager.getProgress(bookId).coerceAtLeast(0L)
        }
    }

    private fun emitListeningActivity(
        contentId: String,
        title: String,
        coverUrl: String,
        audioUrl: String
    ) {
        if (isCiSmokeTest()) return
        val feed = feedRepository ?: return
        val auth = authRepository ?: return
        if (contentId.isBlank() || contentId.startsWith("local_") || contentId.startsWith("feed_")) return
        if (lastPostedListeningContentId == contentId) return
        val uid = auth.currentUserUid?.trim()?.takeIf { it.isNotBlank() && !it.startsWith("guest_") } ?: return
        lastPostedListeningContentId = contentId

        scope.launch {
            try {
                val profile = auth.getUserProfile(uid) ?: run {
                    if (lastPostedListeningContentId == contentId) lastPostedListeningContentId = null
                    return@launch
                }
                val displayName = profile.name.ifBlank { profile.username?.takeIf { it.isNotBlank() } ?: "KitapDuy Okuru" }
                val posted = feed.createPost(
                    userId = uid,
                    userName = displayName,
                    userPhoto = profile.photoUrl,
                    bookId = contentId,
                    bookName = title.ifBlank { "Sesli Kitap" },
                    comment = SupabaseFeedRepository.LISTEN_COMMENT,
                    isPremium = profile.hasPremium(),
                    imageUrl = UrlSanitizer.normalizeMediaUrl(coverUrl).ifBlank { null },
                    audioUrl = UrlSanitizer.normalizeMediaUrl(audioUrl).ifBlank { null },
                    pollOptions = null
                )
                if (!posted && lastPostedListeningContentId == contentId) {
                    lastPostedListeningContentId = null
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                if (lastPostedListeningContentId == contentId) {
                    lastPostedListeningContentId = null
                }
            }
        }
    }

    fun playFeedAudio(item: com.semtinazizi.kitapduy.model.FeedItem, url: String, position: Long) {
        saveProgress(force = true)
        resetSaveThrottle()
        seenActiveTrackForBookEnd = false
        audioPlayer.clearPlaybackError()
        _playbackError.value = null
        val id = "feed_" + item.id
        _currentBook.value = Book(
            id = id,
            title = item.bookTitle.ifBlank { item.userName },
            author = item.userName,
            coverUrl = item.bookCoverUrl,
            audioUrl = url
        )
        _currentEpisode.value = null
        _currentPodcastTitle.value = null
        _currentPosition.value = 0f
        _currentPositionMs.value = 0L
        _duration.value = 0f
        audioPlayer.playQueue(
            listOf(
                AudioMediaItem(
                    id,
                    url,
                    item.bookTitle,
                    item.userName,
                    item.bookCoverUrl,
                    position
                )
            )
        )
        syncPlaybackStates()
    }

    fun togglePlayPause() {
        if (audioPlayer.isPlaying() || audioPlayer.isBuffering()) {
            saveProgress(force = true)
            audioPlayer.pause()
            if (_currentAtmosphereId.value != null) {
                audioPlayer.pauseAtmosphere()
            }
            _isPlaying.value = false
            _isBuffering.value = false
        } else {
            stopQuotePreview()
            audioPlayer.clearPlaybackError()
            _playbackError.value = null
            if (audioPlayer.hasReachedTrackEnd()) {
                val activeBook = _currentBook.value
                val activeEp = _currentEpisode.value
                when {
                    activeBook != null && !activeBook.id.startsWith("feed_") -> {
                        playTrack(activeBook, isPremium = currentBookIsPremium, restartFromBeginning = true)
                        if (_currentAtmosphereId.value != null) audioPlayer.resumeAtmosphere()
                        return
                    }
                    activeEp != null -> {
                        playPodcastEpisode(activeEp, _currentPodcastTitle.value.orEmpty(), restartFromBeginning = true)
                        if (_currentAtmosphereId.value != null) audioPlayer.resumeAtmosphere()
                        return
                    }
                }
            }
            audioPlayer.resume()
            if (_currentAtmosphereId.value != null) {
                audioPlayer.resumeAtmosphere()
            }
            syncPlaybackStates()
        }
    }

    private fun matchesPlayingContent(expectedBookId: String?, expectedEpisodeId: String?): Boolean {
        if (expectedBookId != null && _currentBook.value?.id != expectedBookId) return false
        if (expectedEpisodeId != null && _currentEpisode.value?.id != expectedEpisodeId) return false
        val contentId = _currentBook.value?.id ?: _currentEpisode.value?.id ?: return false
        return audioPlayer.currentMediaId() == contentId
    }

    fun seekTo(fraction: Float, expectedBookId: String? = null, expectedEpisodeId: String? = null) {
        if (!fraction.isFinite() || !matchesPlayingContent(expectedBookId, expectedEpisodeId)) return
        val duration = audioPlayer.getDuration()
        if (duration <= 0) return
        val clamped = fraction.coerceIn(0f, 1f)
        val targetMs = (duration * clamped.toDouble()).toLong()
        audioPlayer.seekTo(targetMs)
        _currentPosition.value = clamped
        _currentPositionMs.value = targetMs
        val contentId = _currentBook.value?.id ?: _currentEpisode.value?.id
        if (contentId != null && targetMs > 0L) {
            progressManager.saveProgress(contentId, targetMs)
            lastSavedContentId = contentId
            lastSavedPositionMs = targetMs
            progressSaveTicks = 0
        }
    }

    fun skipBy(offsetMs: Long, expectedBookId: String? = null, expectedEpisodeId: String? = null) {
        if (!matchesPlayingContent(expectedBookId, expectedEpisodeId)) return
        val duration = audioPlayer.getDuration()
        if (duration <= 0) return
        val position = audioPlayer.getCurrentPosition().coerceIn(0, duration)
        // Clamp the delta before adding it to avoid overflow and use the native position,
        // so repeated taps do not depend on the UI's 500 ms progress refresh.
        val target = position + offsetMs.coerceIn(-position, duration - position)
        audioPlayer.seekTo(target)
        _currentPosition.value = target.toFloat() / duration
        _currentPositionMs.value = target
        val contentId = _currentBook.value?.id ?: _currentEpisode.value?.id
        if (contentId != null && target > 0L) {
            progressManager.saveProgress(contentId, target)
            lastSavedContentId = contentId
            lastSavedPositionMs = target
            progressSaveTicks = 0
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        audioPlayer.setPlaybackSpeed(speed)
    }

    fun selectAtmosphere(soundId: String?, fallbackUrl: String? = null) {
        val cleanId = soundId?.trim()?.takeIf { it.isNotBlank() }
        if (cleanId == null) {
            stopAtmosphere()
            return
        }
        val resourceName = ATMOSPHERE_RESOURCES[cleanId]
        val resolvedUrl = resourceName?.let { resolveBundledAudio(it, "mp3") }
            ?: fallbackUrl?.takeIf { it.isNotBlank() }?.let { UrlSanitizer.normalizeMediaUrl(it) }
            ?: return
        _currentAtmosphereId.value = cleanId
        audioPlayer.playAtmosphere(resolvedUrl, _atmosphereVolume.value)
    }

    fun playAtmosphere(url: String, volume: Float = _atmosphereVolume.value) {
        val clamped = volume.coerceIn(0f, 1f)
        _atmosphereVolume.value = clamped
        val matchedId = ATMOSPHERE_RESOURCES.entries.firstOrNull { (_, res) ->
            url.contains(res, ignoreCase = true)
        }?.key
        _currentAtmosphereId.value = matchedId ?: url
        audioPlayer.playAtmosphere(url, clamped)
    }

    fun stopAtmosphere() {
        _currentAtmosphereId.value = null
        audioPlayer.stopAtmosphere()
    }

    fun setAtmosphereVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _atmosphereVolume.value = clamped
        audioPlayer.setAtmosphereVolume(clamped)
    }

    fun startQuotePreview(book: Book, startPositionMs: Long, durationMs: Long = 15_000L) {
        val localPath = fileDownloader.getDownloadedFilePath(book.id + ".mp3")
        val targetUrl = localPath ?: UrlSanitizer.normalizeMediaUrl(book.audioUrl)
        if (targetUrl.isBlank()) return
        if (audioPlayer.isPlaying() || audioPlayer.isBuffering()) {
            saveProgress(force = true)
            audioPlayer.pause()
            if (_currentAtmosphereId.value != null) {
                audioPlayer.pauseAtmosphere()
            }
            _isPlaying.value = false
            _isBuffering.value = false
        }
        quotePreviewJob?.cancel()
        quotePreviewClipDurationMs = durationMs.coerceAtLeast(1_000L)
        quotePreviewObservedPlaying = false
        _quotePreviewDurationMs.value = 0L
        _isQuotePreviewPlaying.value = true
        audioPlayer.startPreview(targetUrl, startPositionMs.coerceAtLeast(0L))
        scheduleQuotePreviewAutoStop(quotePreviewClipDurationMs)
    }

    private fun scheduleQuotePreviewAutoStop(durationMs: Long) {
        quotePreviewJob?.cancel()
        quotePreviewJob = scope.launch {
            delay(durationMs.coerceAtLeast(1_000L))
            stopQuotePreview()
        }
    }

    fun seekQuotePreview(positionMs: Long) {
        if (_isQuotePreviewPlaying.value) {
            audioPlayer.seekPreviewTo(positionMs.coerceAtLeast(0L))
            scheduleQuotePreviewAutoStop(quotePreviewClipDurationMs)
        }
    }

    fun stopQuotePreview() {
        quotePreviewJob?.cancel()
        quotePreviewJob = null
        quotePreviewObservedPlaying = false
        audioPlayer.stopPreview()
        _isQuotePreviewPlaying.value = false
    }

    fun downloadBook(book: Book, onComplete: (Boolean) -> Unit) {
        if (_isDownloading.value) { onComplete(false); return }
        val normalizedAudioUrl = UrlSanitizer.normalizeMediaUrl(book.audioUrl)
        if (normalizedAudioUrl.isBlank()) { onComplete(false); return }
        val normalizedCoverUrl = UrlSanitizer.normalizeMediaUrl(book.coverUrl)
        _isDownloading.value = true
        scope.launch {
            _isDownloading.value = true
            _downloadProgress.value = 0f
            try {
                fileDownloader.downloadFile(normalizedAudioUrl, book.id + ".mp3") { progress ->
                    _downloadProgress.value = progress
                }
                progressManager.saveContentMetadata(book.id, book.title, book.author, normalizedCoverUrl, normalizedAudioUrl)
                onComplete(true)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                onComplete(false)
            } finally {
                _isDownloading.value = false
            }
        }
    }

    fun downloadPodcastEpisode(
        episode: com.semtinazizi.kitapduy.model.PodcastEpisode,
        podcastTitle: String,
        onComplete: (Boolean) -> Unit
    ) {
        if (_isDownloading.value) { onComplete(false); return }
        val normalizedUrl = UrlSanitizer.normalizeMediaUrl(episode.audioUrl)
        if (normalizedUrl.isBlank()) { onComplete(false); return }
        _isDownloading.value = true
        scope.launch {
            _isDownloading.value = true
            _downloadProgress.value = 0f
            try {
                fileDownloader.downloadFile(normalizedUrl, episode.id + ".mp3") { progress ->
                    _downloadProgress.value = progress
                }
                progressManager.saveContentMetadata(
                    id = episode.id,
                    title = episode.title.ifBlank { podcastTitle.ifBlank { "Podcast Bölümü" } },
                    author = podcastTitle.ifBlank { "Podcast" },
                    coverUrl = episode.imageUrl.orEmpty(),
                    audioUrl = normalizedUrl
                )
                onComplete(true)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                onComplete(false)
            } finally {
                _isDownloading.value = false
            }
        }
    }

    fun isDownloaded(bookId: String): Boolean {
        return fileDownloader.getDownloadedFilePath(bookId + ".mp3") != null
    }

    private fun saveProgress(force: Boolean = false) {
        val id = _currentBook.value?.id ?: _currentEpisode.value?.id ?: return
        if (audioPlayer.currentMediaId() != id) return
        val pos = audioPlayer.getCurrentPosition().coerceAtLeast(0L)
        if (pos <= 0L) return

        val isFirstSaveForItem = lastSavedContentId != id
        if (force || isFirstSaveForItem) {
            if (isFirstSaveForItem || pos != lastSavedPositionMs) {
                progressManager.saveProgress(id, pos)
                lastSavedContentId = id
                lastSavedPositionMs = pos
            }
            progressSaveTicks = 0
            return
        }

        if (!audioPlayer.isPlaying()) return
        progressSaveTicks++
        if (progressSaveTicks >= PROGRESS_SAVE_INTERVAL_TICKS) {
            progressSaveTicks = 0
            if (pos != lastSavedPositionMs) {
                progressManager.saveProgress(id, pos)
                lastSavedContentId = id
                lastSavedPositionMs = pos
            }
        }
    }

    private fun triggerSleepTimerExpiry() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        seenActiveTrackForBookEnd = false
        _sleepTimerRemainingMinutes.value = 0
        _sleepTimerRemainingSeconds.value = 0
        _sleepTimerAtBookEnd.value = false
        saveProgress(force = true)
        audioPlayer.pause()
        stopAtmosphere()
        _isPlaying.value = false
        _isBuffering.value = false
    }

    fun startSleepTimer(minutes: Int) {
        if (minutes <= 0) {
            cancelSleepTimer()
            return
        }
        sleepTimerJob?.cancel()
        seenActiveTrackForBookEnd = false
        _sleepTimerAtBookEnd.value = false
        var remainingSec = minutes * 60
        _sleepTimerRemainingSeconds.value = remainingSec
        _sleepTimerRemainingMinutes.value = minutes

        sleepTimerJob = scope.launch {
            while (remainingSec > 0) {
                delay(1_000)
                remainingSec -= 1
                _sleepTimerRemainingSeconds.value = remainingSec
                _sleepTimerRemainingMinutes.value = if (remainingSec > 0) (remainingSec + 59) / 60 else 0
            }
            triggerSleepTimerExpiry()
        }
    }

    fun startSleepTimerAtBookEnd() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingMinutes.value = 0
        _sleepTimerRemainingSeconds.value = 0
        val activeId = _currentBook.value?.id ?: _currentEpisode.value?.id
        seenActiveTrackForBookEnd = activeId != null && audioPlayer.currentMediaId() == activeId
        _sleepTimerAtBookEnd.value = true
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        seenActiveTrackForBookEnd = false
        _sleepTimerRemainingMinutes.value = 0
        _sleepTimerRemainingSeconds.value = 0
        _sleepTimerAtBookEnd.value = false
    }
}
