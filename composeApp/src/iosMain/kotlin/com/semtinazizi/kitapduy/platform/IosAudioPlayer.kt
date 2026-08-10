package com.semtinazizi.kitapduy.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
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
 * Kotlin/Native ObjC interop kuralları:
 *   - CMTime gibi struct'lar CValue<T> döner → .useContents {} ile erişilir
 *   - ObjC property'ler (currentItem, timeControlStatus) Kotlin'de getter metod olarak gelir
 */
@OptIn(ExperimentalForeignApi::class)
class IosAudioPlayer : AudioPlayer {
    private var avPlayer: AVPlayer? = null

    override fun initialize() {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, error = null)
        session.setActive(true, error = null)
    }

    override fun play(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        val playerItem = AVPlayerItem.playerItemWithURL(nsUrl)
        avPlayer = AVPlayer.playerWithPlayerItem(playerItem)
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
        // CMTimeMake CValue<CMTime> döner, seekToTime bunu direkt kabul eder
        val time = CMTimeMake(value = positionMs, timescale = 1000)
        avPlayer?.seekToTime(time)
    }

    override fun getCurrentPosition(): Long {
        val player = avPlayer ?: return 0L
        // currentTime() → CValue<CMTime> — .useContents{} ile alanlar okunur
        return player.currentTime().useContents {
            if (timescale != 0) value * 1000L / timescale else 0L
        }
    }

    override fun getDuration(): Long {
        // currentItem → ObjC property → Kotlin getter
        val item = avPlayer?.currentItem ?: return 0L
        return item.duration.useContents {
            if (timescale != 0) value * 1000L / timescale else 0L
        }
    }

    override fun isPlaying(): Boolean {
        // timeControlStatus → ObjC property → Kotlin getter
        return avPlayer?.timeControlStatus == AVPlayerTimeControlStatusPlaying
    }

    override fun release() {
        avPlayer?.pause()
        avPlayer = null
    }
}

/**
 * iOS uygulama config değerleri
 */
object IOSBuildConfig {
    var SUPABASE_URL: String = ""
    var SUPABASE_KEY: String = ""
    var CF_ACCOUNT_ID: String = ""
    var CF_PUBLIC_URL: String = ""
}
