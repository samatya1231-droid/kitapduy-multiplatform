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
class IosAudioPlayer : AudioPlayer {
    private var currentUrl: String = ""
    private var playing: Boolean = false
    private var position: Long = 0L

    override fun initialize() {
        // TODO: AVAudioSession setup - Apple Developer hesabı sonrası
    }

    override fun play(url: String) {
        currentUrl = url
        playing = true
        // TODO: AVPlayer ile gerçek oynatma
    }

    override fun pause() {
        playing = false
    }

    override fun resume() {
        playing = true
    }

    override fun stop() {
        playing = false
        position = 0L
        currentUrl = ""
    }

    override fun seekTo(positionMs: Long) {
        position = positionMs
    }

    override fun getCurrentPosition(): Long = position

    override fun getDuration(): Long = 0L

    override fun isPlaying(): Boolean = playing

    override fun release() {
        playing = false
        currentUrl = ""
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
