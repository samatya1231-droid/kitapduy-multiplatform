@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.semtinazizi.kitapduy.platform

/**
 * iOS AudioPlayer - Stub implementasyon
 * 
 * Şu an için derleme başarısının sağlanması amacıyla basit bir stub.
 * Gerçek AVFoundation implementasyonu Apple Developer hesabı alındıktan
 * ve Xcode ortamında test edilebildikten sonra eklenecek.
 * 
 * Android tarafında ExoPlayer ile tam ses oynatma desteklenmektedir.
 */
import platform.AVFoundation.*
import platform.AVFAudio.*
import platform.Foundation.NSURL
import platform.MediaPlayer.*
import kotlinx.cinterop.ExperimentalForeignApi

class IosAudioPlayer : AudioPlayer {
    private var avPlayer: AVPlayer? = null
    private var atmospherePlayer: AVPlayer? = null
    private var currentUrl: String = ""
    private var currentTitle: String = ""
    private var currentArtist: String = ""

    override fun initialize() {
        // AVAudioSession ayarları (Arka planda çalma için gerekli)
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, null)
        session.setActive(true, null)
        
        setupRemoteCommandCenter()
    }

    private fun setupRemoteCommandCenter() {
        // Devre disi birakildi - iOS'ta arka plan thread'inde crash'e sebep oluyor
    }

    private fun updateNowPlayingInfo() {
        // Devre disi birakildi - iOS'ta arka plan thread'inde crash'e sebep oluyor
    }

    override fun play(url: String) {
        currentUrl = url
        val nsUrl = NSURL.URLWithString(url) ?: return
        val playerItem = AVPlayerItem(uRL = nsUrl)
        if (avPlayer == null) {
            avPlayer = AVPlayer(playerItem = playerItem)
        } else {
            avPlayer?.replaceCurrentItemWithPlayerItem(playerItem)
        }
        avPlayer?.play()
        updateNowPlayingInfo()
    }

    override fun playQueue(items: List<AudioMediaItem>, startIndex: Int, startPositionMs: Long) {
        if (items.isEmpty()) return
        val item = items.getOrNull(startIndex) ?: items.first()
        currentTitle = item.title
        currentArtist = item.artist
        play(item.url)
        if (startPositionMs > 0) {
            seekTo(startPositionMs)
        }
        updateNowPlayingInfo()
    }

    override fun pause() {
        avPlayer?.pause()
        updateNowPlayingInfo()
    }

    override fun resume() {
        avPlayer?.play()
        updateNowPlayingInfo()
    }

    override fun stop() {
        avPlayer?.pause()
        avPlayer?.replaceCurrentItemWithPlayerItem(null)
        updateNowPlayingInfo()
    }

    override fun seekTo(positionMs: Long) {
        val timescale = avPlayer?.currentItem?.asset?.duration?.let { platform.CoreMedia.CMTimeGetSeconds(it) } ?: 1.0
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
        return avPlayer?.rate != 0.0f
    }

    override fun setPlaybackSpeed(speed: Float) {
        avPlayer?.rate = speed
    }

    override fun playAtmosphere(url: String, volume: Float) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        val playerItem = AVPlayerItem(uRL = nsUrl)
        if (atmospherePlayer == null) {
            atmospherePlayer = AVPlayer(playerItem = playerItem)
        } else {
            atmospherePlayer?.replaceCurrentItemWithPlayerItem(playerItem)
        }
        atmospherePlayer?.volume = volume
        atmospherePlayer?.play()
        
        // Loop için observer eklenebilir, şimdilik tek seferlik çalıyor.
    }

    override fun stopAtmosphere() {
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
