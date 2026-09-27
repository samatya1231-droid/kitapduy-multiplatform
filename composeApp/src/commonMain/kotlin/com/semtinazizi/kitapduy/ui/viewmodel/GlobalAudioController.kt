package com.semtinazizi.kitapduy.ui.viewmodel

import com.semtinazizi.kitapduy.model.Book
import com.semtinazizi.kitapduy.platform.AudioMediaItem
import com.semtinazizi.kitapduy.platform.AudioPlayer
import com.semtinazizi.kitapduy.platform.FileDownloader
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
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + kotlinx.coroutines.SupervisorJob())
) {
    private var sleepTimerJob: Job? = null
    
    private val _currentBook = MutableStateFlow<Book?>(null)
    val currentBook: StateFlow<Book?> = _currentBook.asStateFlow()

    private val _currentEpisode = MutableStateFlow<com.semtinazizi.kitapduy.model.PodcastEpisode?>(null)
    val currentEpisode: StateFlow<com.semtinazizi.kitapduy.model.PodcastEpisode?> = _currentEpisode.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0f)
    val currentPosition: StateFlow<Float> = _currentPosition.asStateFlow()

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
        // Track position periodically
        scope.launch {
            while (true) {
                _isPlaying.value = audioPlayer.isPlaying()
                if (audioPlayer.currentMediaId() == (_currentBook.value?.id ?: _currentEpisode.value?.id)) {
                    val pos = audioPlayer.getCurrentPosition().toFloat()
                    val dur = audioPlayer.getDuration().toFloat()
                    if (dur > 0) {
                        _duration.value = dur
                        _currentPosition.value = (pos / dur).coerceIn(0f, 1f)
                        saveProgress()
                    }
                }
                delay(500)
            }
        }
    }

    fun playTrack(book: Book, isPremium: Boolean = false) {
        saveProgress()
        _currentBook.value = book
        _currentEpisode.value = null
        _currentPosition.value = 0f
        _duration.value = 0f
        progressManager.saveLastPlayedBook(book.id)
        progressManager.saveContentMetadata(book.id, book.title, book.author, book.coverUrl)
        val localPath = fileDownloader.getDownloadedFilePath(book.id + ".mp3")
        val playUrl = localPath ?: book.audioUrl

        if (!isPremium && localPath == null && !book.id.startsWith("local_")) {
            val preRollAd = AudioMediaItem(
                id = "kitapduy_preroll_ad",
                url = "https://ancient-shape-df97.samatya231.workers.dev/kitapd%C4%B1ytan%C4%B1t%C4%B1m.wav",
                title = "KitapDuy Premium",
                artist = "Sponsorlu İçerik",
                artworkUrl = "https://firebasestorage.googleapis.com/v0/b/semtinazizi.appspot.com/o/kitapduy_logo.png?alt=media"
            )
            val postRollAd = AudioMediaItem(
                id = "kitapduy_postroll_ad",
                url = "https://ancient-shape-df97.samatya231.workers.dev/tan%C4%B1t%C4%B1mbiti%C5%9F.wav",
                title = "KitapDuy Premium",
                artist = "Sponsorlu İçerik",
                artworkUrl = "https://firebasestorage.googleapis.com/v0/b/semtinazizi.appspot.com/o/kitapduy_logo.png?alt=media"
            )
            val mainItem = AudioMediaItem(
                id = book.id,
                url = playUrl,
                title = book.title,
                artist = book.author,
                artworkUrl = book.coverUrl,
                startPositionMs = progressManager.getProgress(book.id)
            )
            audioPlayer.playQueue(listOf(preRollAd, mainItem, postRollAd))
        } else {
            audioPlayer.playQueue(listOf(AudioMediaItem(book.id, playUrl, book.title, book.author,
                book.coverUrl, progressManager.getProgress(book.id))))
        }

        _isPlaying.value = true
        _playbackSpeed.value = 1.0f
        audioPlayer.setPlaybackSpeed(1.0f)
    }

    fun playPodcastEpisode(episode: com.semtinazizi.kitapduy.model.PodcastEpisode) {
        saveProgress()
        _currentEpisode.value = episode
        _currentBook.value = null
        _currentPosition.value = 0f
        _duration.value = 0f
        audioPlayer.playQueue(listOf(AudioMediaItem(episode.id, episode.audioUrl, episode.title,
            artworkUrl = episode.imageUrl.orEmpty(), startPositionMs = progressManager.getProgress(episode.id))))
        _isPlaying.value = true
        _playbackSpeed.value = 1.0f
        audioPlayer.setPlaybackSpeed(1.0f)
    }

    fun playFeedAudio(item: com.semtinazizi.kitapduy.model.FeedItem, url: String, position: Long) {
        saveProgress()
        val id = "feed_" + item.id
        _currentBook.value = Book(id = id, title = item.bookTitle.ifBlank { item.userName },
            author = item.userName, coverUrl = item.bookCoverUrl, audioUrl = url)
        _currentEpisode.value = null
        _currentPosition.value = 0f
        _duration.value = 0f
        audioPlayer.playQueue(listOf(AudioMediaItem(id, url, item.bookTitle, item.userName,
            item.bookCoverUrl, position)))
        _isPlaying.value = true
    }

    fun togglePlayPause() {
        if (audioPlayer.isPlaying()) {
            saveProgress()
            audioPlayer.pause()
            _isPlaying.value = false
        } else {
            audioPlayer.resume()
            _isPlaying.value = true
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
        audioPlayer.seekTo((duration * clamped.toDouble()).toLong())
        _currentPosition.value = clamped
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
                progressManager.saveContentMetadata(book.id, book.title, book.author, book.coverUrl)
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

    private fun saveProgress() {
        val id = _currentBook.value?.id ?: _currentEpisode.value?.id ?: return
        if (audioPlayer.currentMediaId() == id) {
            progressManager.saveProgress(id, audioPlayer.getCurrentPosition())
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
            saveProgress()
            audioPlayer.pause()
            _isPlaying.value = false
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerRemainingMinutes.value = 0
    }
}
