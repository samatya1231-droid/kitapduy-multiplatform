@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.semtinazizi.kitapduy.platform

/** iOS playback, queue transitions and lock-screen commands. */
import platform.AVFoundation.*
import platform.AVFAudio.*
import platform.Foundation.NSLog
import platform.Foundation.NSURL
import kotlinx.coroutines.*
import platform.MediaPlayer.*
import kotlinx.cinterop.ExperimentalForeignApi
import com.semtinazizi.kitapduy.util.UrlSanitizer

class IosAudioPlayer : AudioPlayer {
    private var avPlayer: AVPlayer? = null
    private var atmospherePlayer: AVPlayer? = null
    private var queue: List<AudioMediaItem> = emptyList()
    private var queueIndex = -1
    private var playbackSpeed = 1f
    private var initialized = false
    private var shouldPlay = false
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var readyJob: Job? = null
    private var endObserver: platform.darwin.NSObjectProtocol? = null
    private val remoteTargets = mutableListOf<Pair<MPRemoteCommand, Any>>()
    private var currentTitle: String = ""
    private var currentArtist: String = ""
    private var atmosphereLoopObserver: platform.darwin.NSObjectProtocol? = null
    private var cachedAssetDurationMs: Long = 0L
    private var lastPlaybackError: String? = null
    private var reachedEnd = false
    private var previewPlayer: AVPlayer? = null
    private var previewPlaying: Boolean = false
    private var previewReadyJob: Job? = null
    private var previewEndObserver: platform.darwin.NSObjectProtocol? = null
    private var cachedPreviewDurationMs: Long = 0L

    private fun resolveNsUrl(rawUrl: String): NSURL? {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("/")) {
            return NSURL.fileURLWithPath(trimmed)
        }
        val safeUrl = UrlSanitizer.sanitizeUrl(trimmed)
        return NSURL.URLWithString(safeUrl) ?: NSURL.URLWithString(trimmed)
    }

    private fun finiteSecondsToMs(seconds: Double): Long {
        if (!seconds.isFinite() || seconds <= 0.0) return 0L
        val millis = seconds * 1000.0
        if (!millis.isFinite() || millis >= Long.MAX_VALUE.toDouble()) return 0L
        return millis.toLong()
    }

    override fun initialize() {
        if (initialized) return
        initialized = true
        // AVAudioSession ayarları (Arka planda çalma için gerekli)
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, null)
        session.setActive(true, null)
        
        setupRemoteCommandCenter()
    }

    private fun setupRemoteCommandCenter() {
        val center = MPRemoteCommandCenter.sharedCommandCenter()
        remoteTargets += center.playCommand to center.playCommand.addTargetWithHandler {
            resume(); MPRemoteCommandHandlerStatusSuccess
        }
        remoteTargets += center.pauseCommand to center.pauseCommand.addTargetWithHandler {
            pause(); MPRemoteCommandHandlerStatusSuccess
        }
        remoteTargets += center.stopCommand to center.stopCommand.addTargetWithHandler {
            stop(); MPRemoteCommandHandlerStatusSuccess
        }
    }

    private fun updateNowPlayingInfo() {
        platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
            val center = MPNowPlayingInfoCenter.defaultCenter()
            val info = mutableMapOf<Any?, Any?>()
            info[MPMediaItemPropertyTitle] = currentTitle
            info[MPMediaItemPropertyArtist] = currentArtist
            info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = getCurrentPosition() / 1000.0
            info[MPMediaItemPropertyPlaybackDuration] = getDuration() / 1000.0
            info[MPNowPlayingInfoPropertyPlaybackRate] = if (isPlaying()) playbackSpeed else 0.0f
            center.nowPlayingInfo = info
        }
    }

    override fun play(url: String) {
        playQueue(listOf(AudioMediaItem(url, url)))
    }

    override fun playQueue(items: List<AudioMediaItem>, startIndex: Int, startPositionMs: Long) {
        if (items.isEmpty()) { stop(); return }
        initialize()
        shouldPlay = true
        queue = items
        queueIndex = startIndex.coerceIn(items.indices)
        playCurrentItem(if (startPositionMs > 0) startPositionMs else items[queueIndex].startPositionMs)
    }

    private fun playCurrentItem(startPositionMs: Long) {
        readyJob?.cancel()
        readyJob = null
        avPlayer?.pause()
        endObserver?.let { platform.Foundation.NSNotificationCenter.defaultCenter.removeObserver(it) }
        endObserver = null
        cachedAssetDurationMs = 0L
        lastPlaybackError = null
        reachedEnd = false
        val item = queue.getOrNull(queueIndex) ?: return
        val url = resolveNsUrl(item.url)
        if (url == null) {
            NSLog("KitapDuy playback invalid URL: ${item.url}")
            avPlayer?.replaceCurrentItemWithPlayerItem(null)
            if (item.id == "kitapduy_preroll_ad" && queueIndex + 1 < queue.size) {
                queueIndex++
                playCurrentItem(queue[queueIndex].startPositionMs)
            } else if (item.id == "kitapduy_postroll_ad") {
                reachedEnd = true
                shouldPlay = false
                updateNowPlayingInfo()
            } else {
                shouldPlay = false
                lastPlaybackError = "Ses bağlantısı geçersiz veya oynatılamıyor."
                updateNowPlayingInfo()
            }
            return
        }
        currentTitle = item.title
        currentArtist = item.artist
        val playerItem = AVPlayerItem(uRL = url)
        if (avPlayer == null) avPlayer = AVPlayer(playerItem = playerItem)
        else avPlayer?.replaceCurrentItemWithPlayerItem(playerItem)
        endObserver = platform.Foundation.NSNotificationCenter.defaultCenter.addObserverForName(
            AVPlayerItemDidPlayToEndTimeNotification, playerItem, platform.Foundation.NSOperationQueue.mainQueue
        ) {
            if (queueIndex + 1 < queue.size) {
                queueIndex++
                playCurrentItem(queue[queueIndex].startPositionMs)
            } else {
                reachedEnd = true
                shouldPlay = false
                avPlayer?.pause()
                updateNowPlayingInfo()
            }
        }
        // Let AVPlayer begin buffering for playback while it prepares the item.
        if (startPositionMs <= 0 && shouldPlay) startPlayback()
        readyJob = scope.launch {
            val player = avPlayer ?: return@launch
            // Start the watchdog before waiting for readiness. A stalled buffer
            // estimate can delay the ready status as well as playback.
            val startupRetry = if (startPositionMs <= 0) launch {
                retryBufferedStart(player, playerItem)
            } else null
            val ready = withTimeoutOrNull(30_000) {
                while (playerItem.status == AVPlayerItemStatusUnknown) delay(50)
                playerItem.status == AVPlayerItemStatusReadyToPlay
            } == true
            if (!ready) {
                startupRetry?.cancel()
                val errorCode = playerItem.error?.code
                NSLog("KitapDuy playback item unavailable (code ${errorCode ?: "timeout"})")
                if (avPlayer?.currentItem == playerItem) {
                    endObserver?.let { platform.Foundation.NSNotificationCenter.defaultCenter.removeObserver(it) }
                    endObserver = null
                    avPlayer?.replaceCurrentItemWithPlayerItem(null)
                }
                if (item.id == "kitapduy_preroll_ad" && queueIndex + 1 < queue.size) {
                    queueIndex++
                    playCurrentItem(queue[queueIndex].startPositionMs)
                } else if (item.id == "kitapduy_postroll_ad") {
                    reachedEnd = true
                    shouldPlay = false
                    avPlayer?.pause()
                    updateNowPlayingInfo()
                } else {
                    shouldPlay = false
                    avPlayer?.pause()
                    lastPlaybackError = if (errorCode != null) {
                        "Ses dosyası yüklenemedi (Kod: $errorCode). Lütfen bağlantınızı kontrol edip tekrar deneyin."
                    } else {
                        "Ses dosyası zaman aşımına uğradı. Lütfen tekrar deneyin."
                    }
                    updateNowPlayingInfo()
                }
                return@launch
            }
            val itemDurationMs = finiteSecondsToMs(platform.CoreMedia.CMTimeGetSeconds(playerItem.duration))
            if (itemDurationMs > 0L) {
                cachedAssetDurationMs = itemDurationMs
            } else {
                val asset = playerItem.asset
                launch {
                    val assetDurationMs = withContext(Dispatchers.Default) {
                        finiteSecondsToMs(platform.CoreMedia.CMTimeGetSeconds(asset.duration))
                    }
                    if (avPlayer?.currentItem == playerItem && assetDurationMs > 0L) {
                        cachedAssetDurationMs = assetDurationMs
                        updateNowPlayingInfo()
                    }
                }
            }
            if (startPositionMs > 0) {
                seekTo(startPositionMs)
                if (shouldPlay) startPlayback()
            } else if (!shouldPlay) {
                player.pause()
            }
            updateNowPlayingInfo()
            if (startPositionMs > 0 && shouldPlay) retryBufferedStart(player, playerItem)
        }
    }

    private suspend fun retryBufferedStart(player: AVPlayer, item: AVPlayerItem) {
        delay(3_000)
        repeat(7) {
            if (!shouldPlay || player.currentItem != item ||
                player.timeControlStatus != AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate
            ) return
            if (item.loadedTimeRanges.isNotEmpty()) {
                NSLog("KitapDuy playback starting with buffered audio")
                player.playImmediatelyAtRate(playbackSpeed)
                updateNowPlayingInfo()
                return
            }
            delay(1_000)
        }
    }

    private fun startPlayback() {
        val player = avPlayer ?: return
        player.play()
        if (playbackSpeed != 1f) player.rate = playbackSpeed
    }

    override fun currentMediaId(): String? {
        val currentId = queue.getOrNull(queueIndex)?.id ?: return null
        val item = avPlayer?.currentItem ?: return null
        if (currentId == "kitapduy_preroll_ad" || currentId == "kitapduy_postroll_ad") {
            return if (item.status != AVPlayerItemStatusFailed) currentId else null
        }
        return if (item.status == AVPlayerItemStatusReadyToPlay) currentId else null
    }

    override fun pause() {
        shouldPlay = false
        avPlayer?.pause()
        updateNowPlayingInfo()
    }

    override fun resume() {
        val currentItem = avPlayer?.currentItem
        val queuedItem = queue.getOrNull(queueIndex)
        val needsReload = currentItem == null ||
            currentItem.status == AVPlayerItemStatusFailed ||
            (currentItem.status == AVPlayerItemStatusUnknown && readyJob?.isActive != true)
        if (needsReload) {
            if (queuedItem != null) {
                lastPlaybackError = null
                shouldPlay = true
                reachedEnd = false
                playCurrentItem(queuedItem.startPositionMs)
            }
            return
        }
        lastPlaybackError = null
        shouldPlay = true
        if (reachedEnd) {
            reachedEnd = false
            seekTo(0L)
        }
        if (currentItem.status == AVPlayerItemStatusReadyToPlay) startPlayback()
        updateNowPlayingInfo()
    }

    override fun stop() {
        shouldPlay = false
        reachedEnd = false
        readyJob?.cancel()
        readyJob = null
        cachedAssetDurationMs = 0L
        lastPlaybackError = null
        avPlayer?.pause()
        endObserver?.let { platform.Foundation.NSNotificationCenter.defaultCenter.removeObserver(it) }
        endObserver = null
        queue = emptyList()
        queueIndex = -1
        avPlayer?.replaceCurrentItemWithPlayerItem(null)
        updateNowPlayingInfo()
    }

    override fun seekTo(positionMs: Long) {
        reachedEnd = false
        val safePos = positionMs.coerceAtLeast(0L)
        val time = platform.CoreMedia.CMTimeMake(value = safePos, timescale = 1000)
        avPlayer?.seekToTime(time)
        updateNowPlayingInfo()
    }

    override fun getCurrentPosition(): Long {
        val time = avPlayer?.currentTime() ?: return 0L
        val seconds = platform.CoreMedia.CMTimeGetSeconds(time)
        if (!seconds.isFinite() || seconds < 0.0) return 0L
        return (seconds * 1000.0).toLong()
    }

    override fun getDuration(): Long {
        val item = avPlayer?.currentItem ?: return 0L
        val itemDurationMs = finiteSecondsToMs(platform.CoreMedia.CMTimeGetSeconds(item.duration))
        if (itemDurationMs > 0L) return itemDurationMs
        if (cachedAssetDurationMs > 0L) return cachedAssetDurationMs
        return 0L
    }

    override fun isPlaying(): Boolean {
        if (!shouldPlay) return false
        val player = avPlayer ?: return false
        val item = player.currentItem ?: return false
        if (item.status != AVPlayerItemStatusReadyToPlay) return false
        return player.timeControlStatus == AVPlayerTimeControlStatusPlaying
    }

    override fun isBuffering(): Boolean {
        if (!shouldPlay) return false
        val player = avPlayer ?: return false
        val item = player.currentItem ?: return false
        if (item.status == AVPlayerItemStatusFailed) return false
        if (item.status == AVPlayerItemStatusUnknown) return true
        return player.timeControlStatus == AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate
    }

    override fun getPlaybackError(): String? {
        if (lastPlaybackError != null) return lastPlaybackError
        val item = avPlayer?.currentItem
        if (item != null && item.status == AVPlayerItemStatusFailed) {
            val currentId = queue.getOrNull(queueIndex)?.id
            if (currentId != "kitapduy_preroll_ad" && currentId != "kitapduy_postroll_ad") {
                val code = item.error?.code
                lastPlaybackError = if (code != null) {
                    "Oynatma hatası oluştu (Kod: $code). Lütfen tekrar deneyin."
                } else {
                    "Ses akışı kesildi. Lütfen tekrar deneyin."
                }
                shouldPlay = false
            }
        }
        return lastPlaybackError
    }

    override fun clearPlaybackError() {
        lastPlaybackError = null
    }

    override fun setPlaybackSpeed(speed: Float) {
        val playing = isPlaying()
        playbackSpeed = speed
        if (playing && avPlayer?.currentItem?.status == AVPlayerItemStatusReadyToPlay) avPlayer?.rate = speed
        updateNowPlayingInfo()
    }

    override fun playAtmosphere(url: String, volume: Float) {
        initialize()
        val nsUrl = resolveNsUrl(url) ?: return
        val playerItem = AVPlayerItem(uRL = nsUrl)
        if (atmospherePlayer == null) {
            atmospherePlayer = AVPlayer(playerItem = playerItem)
        } else {
            atmospherePlayer?.replaceCurrentItemWithPlayerItem(playerItem)
        }
        atmospherePlayer?.volume = volume.coerceIn(0f, 1f)
        atmospherePlayer?.play()

        atmosphereLoopObserver?.let {
            platform.Foundation.NSNotificationCenter.defaultCenter.removeObserver(it)
            atmosphereLoopObserver = null
        }

        atmosphereLoopObserver = platform.Foundation.NSNotificationCenter.defaultCenter.addObserverForName(
            name = platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification,
            `object` = playerItem,
            queue = platform.Foundation.NSOperationQueue.mainQueue
        ) { _ ->
            atmospherePlayer?.seekToTime(platform.CoreMedia.CMTimeMake(0, 1))
            atmospherePlayer?.play()
        }
    }

    override fun pauseAtmosphere() {
        atmospherePlayer?.pause()
    }

    override fun resumeAtmosphere() {
        atmospherePlayer?.play()
    }

    override fun stopAtmosphere() {
        atmosphereLoopObserver?.let {
            platform.Foundation.NSNotificationCenter.defaultCenter.removeObserver(it)
            atmosphereLoopObserver = null
        }
        atmospherePlayer?.pause()
        atmospherePlayer?.replaceCurrentItemWithPlayerItem(null)
        atmospherePlayer = null
    }

    override fun setAtmosphereVolume(volume: Float) {
        atmospherePlayer?.volume = volume.coerceIn(0f, 1f)
    }

    override fun hasReachedTrackEnd(): Boolean = reachedEnd

    override fun skipToMediaId(mediaId: String): Boolean {
        val targetIndex = queue.indexOfFirst { it.id == mediaId }
        if (targetIndex < 0) return false
        queueIndex = targetIndex
        shouldPlay = true
        playCurrentItem(queue[targetIndex].startPositionMs)
        return true
    }

    override fun startPreview(url: String, startPositionMs: Long) {
        stopPreview()
        initialize()
        val nsUrl = resolveNsUrl(url) ?: return
        val item = AVPlayerItem(uRL = nsUrl)
        val player = AVPlayer(playerItem = item)
        previewPlayer = player
        previewPlaying = true
        previewEndObserver = platform.Foundation.NSNotificationCenter.defaultCenter.addObserverForName(
            AVPlayerItemDidPlayToEndTimeNotification,
            item,
            platform.Foundation.NSOperationQueue.mainQueue
        ) {
            previewPlaying = false
            previewPlayer?.pause()
        }
        val safeStart = startPositionMs.coerceAtLeast(0L)
        if (safeStart <= 0L) {
            player.play()
        }
        previewReadyJob = scope.launch {
            val ready = withTimeoutOrNull(15_000) {
                while (item.status == AVPlayerItemStatusUnknown) delay(50)
                item.status == AVPlayerItemStatusReadyToPlay
            } == true
            if (!ready || previewPlayer != player) {
                if (previewPlayer == player) stopPreview()
                return@launch
            }
            val itemDur = finiteSecondsToMs(platform.CoreMedia.CMTimeGetSeconds(item.duration))
            if (itemDur > 0L) {
                cachedPreviewDurationMs = itemDur
            } else {
                val asset = item.asset
                launch {
                    val assetDur = withContext(Dispatchers.Default) {
                        finiteSecondsToMs(platform.CoreMedia.CMTimeGetSeconds(asset.duration))
                    }
                    if (previewPlayer == player && assetDur > 0L) {
                        cachedPreviewDurationMs = assetDur
                    }
                }
            }
            if (safeStart > 0L) {
                player.seekToTime(platform.CoreMedia.CMTimeMake(value = safeStart, timescale = 1000))
            }
            if (previewPlaying) {
                player.play()
            }
        }
    }

    override fun seekPreviewTo(positionMs: Long) {
        val safePos = positionMs.coerceAtLeast(0L)
        previewPlayer?.seekToTime(platform.CoreMedia.CMTimeMake(value = safePos, timescale = 1000))
    }

    override fun stopPreview() {
        previewPlaying = false
        cachedPreviewDurationMs = 0L
        previewReadyJob?.cancel()
        previewReadyJob = null
        previewEndObserver?.let {
            platform.Foundation.NSNotificationCenter.defaultCenter.removeObserver(it)
            previewEndObserver = null
        }
        previewPlayer?.pause()
        previewPlayer?.replaceCurrentItemWithPlayerItem(null)
        previewPlayer = null
    }

    override fun isPreviewPlaying(): Boolean {
        val player = previewPlayer ?: return false
        return previewPlaying && player.timeControlStatus == AVPlayerTimeControlStatusPlaying
    }

    override fun getPreviewDuration(): Long {
        val item = previewPlayer?.currentItem
        if (item != null) {
            val itemDur = finiteSecondsToMs(platform.CoreMedia.CMTimeGetSeconds(item.duration))
            if (itemDur > 0L) return itemDur
        }
        return cachedPreviewDurationMs
    }

    override fun release() {
        stop()
        stopAtmosphere()
        stopPreview()
        avPlayer = null
        remoteTargets.forEach { (command, target) -> command.removeTarget(target) }
        remoteTargets.clear()
        initialized = false
    }
}

/**
 * iOS uygulama config değerleri
 */
object IOSBuildConfig {
    var SUPABASE_URL: String = "https://hojyvyuoafjogefhtrgg.supabase.co"
    var SUPABASE_KEY: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhvanl2eXVvYWZqb2dlZmh0cmdnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzEzNTg2NzIsImV4cCI6MjA4NjkzNDY3Mn0.8Ia4a5wwzL_XYbhMW7pOztZfqwoGKoK_Mom1cqv7z2Q"
    var CF_ACCOUNT_ID: String = "50b81abf28d218b3b9858e9c69d4ae03"
    var CF_PUBLIC_URL: String = "https://ses.kitapduy.com.tr"
    var CF_WORKER_URL: String = "https://ses.kitapduy.com.tr"
    var CF_UPLOAD_TOKEN: String = ""
    var R2_UPLOAD_TICKET_URL: String = "https://us-central1-kitapduy.cloudfunctions.net/createR2UploadHttp"
    val isCiSmokeTest: Boolean
        get() = platform.Foundation.NSProcessInfo.processInfo.environment["KITAPDUY_CI_SMOKE_TEST"] == "1"
}
