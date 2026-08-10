package com.semtinazizi.kitapduy.platform

import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerTimeControlStatusPlaying
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.seekToTime
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSURL

/**
 * iOS AudioPlayer implementasyonu - AVFoundation/AVPlayer kullanır
 * commonMain'deki AudioPlayer interface'ini implement eder
 */
class IosAudioPlayer : AudioPlayer {
    private var avPlayer: AVPlayer? = null

    override fun initialize() {
        // AVAudioSession'ı background playback için ayarla
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, error = null)
        session.setActive(true, error = null)
    }

    override fun play(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        val playerItem = AVPlayerItem(url = nsUrl)
        avPlayer = AVPlayer(playerItem = playerItem)
        avPlayer?.play()
    }

    override fun pause() {
        avPlayer?.pause()
    }

    override fun resume() {
        avPlayer?.play()
    }

    override fun stop() {
        avPlayer?.pause()
        avPlayer = null
    }

    override fun seekTo(positionMs: Long) {
        val seconds = positionMs / 1000.0
        val time = CMTimeMake(value = positionMs, timescale = 1000)
        avPlayer?.seekToTime(time)
    }

    override fun getCurrentPosition(): Long {
        val time = avPlayer?.currentTime() ?: return 0L
        return (time.value * 1000L / time.timescale)
    }

    override fun getDuration(): Long {
        val duration = avPlayer?.currentItem?.duration ?: return 0L
        return (duration.value * 1000L / duration.timescale)
    }

    override fun isPlaying(): Boolean {
        return avPlayer?.timeControlStatus == AVPlayerTimeControlStatusPlaying
    }

    override fun release() {
        avPlayer?.pause()
        avPlayer = null
    }
}

/**
 * iOS uygulama config değerleri
 * Codemagic build sırasında environment variables ile doldurulur
 */
object IOSBuildConfig {
    // Bu değerler Codemagic'te build time'da inject edilecek
    // Ya da Info.plist'ten okunabilir
    var SUPABASE_URL: String = ""
    var SUPABASE_KEY: String = ""
    var CF_ACCOUNT_ID: String = ""
    var CF_PUBLIC_URL: String = ""
}
