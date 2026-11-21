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
    private val authRepository: AuthRepository? = null
) {
    companion object {
        internal const val PREROLL_AD_ID = "kitapduy_preroll_ad"
        internal const val POSTROLL_AD_ID = "kitapduy_postroll_ad"
        internal const val PROGRESS_SAVE_INTERVAL_TICKS = 10 // 10 * 500ms = 5s
    }

    private var sleepTimerJob: Job? = null
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

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _sleepTimerRemainingMinutes = MutableStateFlow(0)
    val sleepTimerRemainingMinutes: StateFlow<Int> = _sleepTimerRemainingMinutes.asStateFlow()

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
        _isAdPlaying.value = activeId != null && (mediaId == PREROLL_AD_ID || mediaId == POSTROLL_AD_ID)
        _playbackError.value = audioPlayer.getPlaybackError()
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
        _sleepTimerRemainingMinutes.value = 0
        audioPlayer.stop()
        audioPlayer.stopAtmosphere()
        audioPlayer.clearPlaybackError()
        resetSaveThrottle()
        _currentBook.value = null
        _currentEpisode.value = null
        _currentPodcastTitle.value = null
        _isPlaying.value = false
        _isBuffering.value = false
        _isAdPlaying.value = false
        _playbackError.value = null
        _currentPosition.value = 0f
        _currentPositionMs.value = 0L
        _duration.value = 0f
    }

    fun savedPositionMsFor(contentId: String): Long {
        if (contentId.isBlank()) return 0L
        return progressManager.getProgress(contentId).coerceAtLeast(0L)
    }

    fun playTrack(book: Book, isPremium: Boolean = false) {
        saveProgress(force = true)
        resetSaveThrottle()
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

        if (!isPremium && localPath == null && !book.id.startsWith("local_") && !isCiSmokeTest()) {
            val preRollAd = AudioMediaItem(
                id = PREROLL_AD_ID,
                url = UrlSanitizer.normalizeMediaUrl("https://ancient-shape-df97.samatya231.workers.dev/kitapd%C4%B1ytan%C4%B1t%C4%B1m.wav"),
                title = "KitapDuy Premium",
                artist = "Sponsorlu İçerik",
                artworkUrl = "https://firebasestorage.googleapis.com/v0/b/semtinazizi.appspot.com/o/kitapduy_logo.png?alt=media"
            )
            val postRollAd = AudioMediaItem(
                id = POSTROLL_AD_ID,
                url = UrlSanitizer.normalizeMediaUrl("https://ancient-shape-df97.samatya231.workers.dev/tan%C4%B1t%C4%B1mbiti%C5%9F.wav"),
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
                startPositionMs = progressManager.getProgress(book.id)
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
                        progressManager.getProgress(book.id)
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
        podcastTitle: String = ""
    ) {
        saveProgress(force = true)
        resetSaveThrottle()
        audioPlayer.clearPlaybackError()
        _playbackError.value = null
        val resolvedTitle = podcastTitle.trim().takeIf { it.isNotBlank() }
        val normalizedAudioUrl = UrlSanitizer.normalizeMediaUrl(episode.audioUrl)
        val normalizedImageUrl = episode.imageUrl?.let { UrlSanitizer.normalizeMediaUrl(it) }.orEmpty()
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
                    normalizedAudioUrl,
                    episode.title,
                    artist = resolvedTitle.orEmpty(),
                    artworkUrl = normalizedImageUrl,
                    startPositionMs = progressManager.getProgress(episode.id)
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
            _isPlaying.value = false
            _isBuffering.value = false
        } else {
            audioPlayer.clearPlaybackError()
            _playbackError.value = null
            audioPlayer.resume()
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

    fun playAtmosphere(url: String, volume: Float = 0.5f) {
        audioPlayer.playAtmosphere(url, volume)
    }

    fun stopAtmosphere() {
        audioPlayer.stopAtmosphere()
    }

    fun setAtmosphereVolume(volume: Float) {
        audioPlayer.setAtmosphereVolume(volume)
    }

    fun downloadBook(book: Book, onComplete: (Boolean) -> Unit) {
        if (_isDownloading.value) { onComplete(false); return }
        _isDownloading.value = true
        scope.launch {
            _isDownloading.value = true
            _downloadProgress.value = 0f
            try {
                fileDownloader.downloadFile(book.audioUrl, book.id + ".mp3") { progress ->
                    _downloadProgress.value = progress
                }
                progressManager.saveContentMetadata(book.id, book.title, book.author, book.coverUrl, book.audioUrl)
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

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _sleepTimerRemainingMinutes.value = minutes

        sleepTimerJob = scope.launch {
            while (_sleepTimerRemainingMinutes.value > 0) {
                delay(60_000) // 1 minute
                _sleepTimerRemainingMinutes.value -= 1
            }
            saveProgress(force = true)
            audioPlayer.pause()
            _isPlaying.value = false
            _isBuffering.value = false
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerRemainingMinutes.value = 0
    }
}
