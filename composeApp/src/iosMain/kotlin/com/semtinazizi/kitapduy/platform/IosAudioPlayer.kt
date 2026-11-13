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
        avPlayer?.pause()
        endObserver?.let { platform.Foundation.NSNotificationCenter.defaultCenter.removeObserver(it) }
        endObserver = null
        val item = queue.getOrNull(queueIndex) ?: return
        val safeUrl = UrlSanitizer.sanitizeUrl(item.url)
        val url = NSURL.URLWithString(safeUrl) ?: NSURL.URLWithString(item.url) ?: return
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
                shouldPlay = false
                avPlayer?.pause()
                updateNowPlayingInfo()
            }
        }
        // Let AVPlayer begin buffering for playback while it prepares the item.
        if (startPositionMs <= 0) startPlayback()
        readyJob = scope.launch {
            val ready = withTimeoutOrNull(30_000) {
                while (playerItem.status == AVPlayerItemStatusUnknown) delay(50)
                playerItem.status == AVPlayerItemStatusReadyToPlay
            } == true
            if (!ready) {
                NSLog("KitapDuy playback item unavailable (code ${playerItem.error?.code ?: "timeout"})")
                if (item.id == "kitapduy_preroll_ad" && queueIndex + 1 < queue.size) {
                    queueIndex++
                    playCurrentItem(queue[queueIndex].startPositionMs)
                } else {
                    shouldPlay = false
                    updateNowPlayingInfo()
                }
                return@launch
            }
            if (startPositionMs > 0) seekTo(startPositionMs)
            if (shouldPlay) startPlayback()
            updateNowPlayingInfo()
        }
    }

    private fun startPlayback() {
        val player = avPlayer ?: return
        player.play()
        if (playbackSpeed != 1f) player.rate = playbackSpeed
    }

    override fun currentMediaId(): String? = if (avPlayer?.currentItem?.status == AVPlayerItemStatusReadyToPlay) queue.getOrNull(queueIndex)?.id else null

    override fun pause() {
        shouldPlay = false
        avPlayer?.pause()
        updateNowPlayingInfo()
    }

    override fun resume() {
        shouldPlay = true
        if (avPlayer?.currentItem?.status == AVPlayerItemStatusReadyToPlay) startPlayback()
        updateNowPlayingInfo()
    }

    override fun stop() {
        shouldPlay = false
        readyJob?.cancel()
        readyJob = null
        avPlayer?.pause()
        endObserver?.let { platform.Foundation.NSNotificationCenter.defaultCenter.removeObserver(it) }
        endObserver = null
        queue = emptyList()
        queueIndex = -1
        avPlayer?.replaceCurrentItemWithPlayerItem(null)
        updateNowPlayingInfo()
    }

    override fun seekTo(positionMs: Long) {
        val time = platform.CoreMedia.CMTimeMake(value = positionMs, timescale = 1000)
        avPlayer?.seekToTime(time)
        updateNowPlayingInfo()
    }

    override fun getCurrentPosition(): Long {
        val time = avPlayer?.currentTime() ?: return 0L
        return (platform.CoreMedia.CMTimeGetSeconds(time) * 1000).toLong()
    }

    override fun getDuration(): Long {
        val duration = avPlayer?.currentItem?.duration ?: return 0L
        val seconds = platform.CoreMedia.CMTimeGetSeconds(duration)
        if (seconds.isNaN()) return 0L
        return (seconds * 1000).toLong()
    }

    override fun isPlaying(): Boolean {
        return shouldPlay
    }

    override fun setPlaybackSpeed(speed: Float) {
        val playing = isPlaying()
        playbackSpeed = speed
        if (playing && avPlayer?.currentItem?.status == AVPlayerItemStatusReadyToPlay) avPlayer?.rate = speed
        updateNowPlayingInfo()
    }

    override fun playAtmosphere(url: String, volume: Float) {
        val safeUrl = UrlSanitizer.sanitizeUrl(url)
        val nsUrl = NSURL.URLWithString(safeUrl) ?: NSURL.URLWithString(url) ?: return
        val playerItem = AVPlayerItem(uRL = nsUrl)
        if (atmospherePlayer == null) {
            atmospherePlayer = AVPlayer(playerItem = playerItem)
        } else {
            atmospherePlayer?.replaceCurrentItemWithPlayerItem(playerItem)
        }
        atmospherePlayer?.volume = volume
        atmospherePlayer?.play()
        
        atmosphereLoopObserver?.let {
            platform.Foundation.NSNotificationCenter.defaultCenter.removeObserver(it)
            atmosphereLoopObserver = null
        }
        
        atmosphereLoopObserver = platform.Foundation.NSNotificationCenter.defaultCenter.addObserverForName(
            name = platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification,
            `object` = atmospherePlayer?.currentItem,
            queue = null
        ) { _ ->
            atmospherePlayer?.seekToTime(platform.CoreMedia.CMTimeMake(0, 1))
            atmospherePlayer?.play()
        }
    }

    override fun stopAtmosphere() {
        atmosphereLoopObserver?.let {
            platform.Foundation.NSNotificationCenter.defaultCenter.removeObserver(it)
            atmosphereLoopObserver = null
        }
        atmospherePlayer?.pause()
        atmospherePlayer = null
    }

    override fun setAtmosphereVolume(volume: Float) {
        atmospherePlayer?.volume = volume
    }

    override fun release() {
        stop()
        stopAtmosphere()
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
    var CF_PUBLIC_URL: String = "https://pub-7327d7da4dcf4c1bbf6d53bacbbe276c.r2.dev"
    var CF_WORKER_URL: String = "https://ancient-shape-df97.samatya231.workers.dev"
    var CF_UPLOAD_TOKEN: String = "DUMMY_TOKEN"
}
