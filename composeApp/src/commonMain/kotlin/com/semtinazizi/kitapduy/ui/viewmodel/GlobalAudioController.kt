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
    private val fileDownloader: FileDownloader
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
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
        // Track position periodically
        scope.launch {
            while (true) {
                if (_isPlaying.value) {
                    val pos = audioPlayer.getCurrentPosition().toFloat()
                    val dur = audioPlayer.getDuration().toFloat()
                    if (dur > 0) {
                        _duration.value = dur
                        _currentPosition.value = pos / dur
                    }
                }
                delay(500)
            }
        }
    }

    fun playTrack(book: Book, isPremium: Boolean = false) {
        _currentBook.value = book
        _currentEpisode.value = null
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
                artworkUrl = book.coverUrl
            )
            audioPlayer.playQueue(listOf(preRollAd, mainItem, postRollAd))
        } else {
            audioPlayer.play(playUrl)
        }

        _isPlaying.value = true
        _playbackSpeed.value = 1.0f
        audioPlayer.setPlaybackSpeed(1.0f)
    }

    fun playPodcastEpisode(episode: com.semtinazizi.kitapduy.model.PodcastEpisode) {
        _currentEpisode.value = episode
        _currentBook.value = null
        audioPlayer.play(episode.audioUrl)
        _isPlaying.value = true
        _playbackSpeed.value = 1.0f
        audioPlayer.setPlaybackSpeed(1.0f)
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            audioPlayer.pause()
            _isPlaying.value = false
        } else {
            audioPlayer.resume()
            _isPlaying.value = true
        }
    }

    fun seekTo(fraction: Float) {
        val dur = audioPlayer.getDuration()
        if (dur > 0) {
            val targetMs = (dur * fraction).toLong()
            audioPlayer.seekTo(targetMs)
            _currentPosition.value = fraction
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

    fun downloadCurrentBook(onComplete: (Boolean) -> Unit) {
        val book = _currentBook.value ?: return
        scope.launch {
            _isDownloading.value = true
            _downloadProgress.value = 0f
            try {
                fileDownloader.downloadFile(book.audioUrl, book.id + ".mp3") { progress ->
                    _downloadProgress.value = progress
                }
                onComplete(true)
            } catch (e: Throwable) {
                onComplete(false)
            } finally {
                _isDownloading.value = false
            }
        }
    }
    
    fun isDownloaded(bookId: String): Boolean {
        return fileDownloader.getDownloadedFilePath(bookId + ".mp3") != null
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _sleepTimerRemainingMinutes.value = minutes
        
        sleepTimerJob = scope.launch {
            while (_sleepTimerRemainingMinutes.value > 0) {
                delay(60_000) // 1 minute
                _sleepTimerRemainingMinutes.value -= 1
            }
            if (_isPlaying.value) {
                togglePlayPause()
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerRemainingMinutes.value = 0
    }
}
