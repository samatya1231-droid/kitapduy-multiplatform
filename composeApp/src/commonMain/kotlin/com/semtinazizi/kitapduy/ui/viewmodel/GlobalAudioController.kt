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

    fun playTrack(book: Book) {
        _currentBook.value = book
        val localPath = fileDownloader.getDownloadedFilePath(book.id + ".mp3")
        val playUrl = localPath ?: book.audioUrl

        audioPlayer.play(playUrl)
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
