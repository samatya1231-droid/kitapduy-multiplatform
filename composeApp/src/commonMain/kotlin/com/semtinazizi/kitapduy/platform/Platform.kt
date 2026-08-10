package com.semtinazizi.kitapduy.platform

// ─────────────────────────────────────────────────────────────────────────────
// EXPECT DECLARATIONS - commonMain
// Her platform kendi 'actual' implementasyonunu sağlar
// ─────────────────────────────────────────────────────────────────────────────

/** Platform adı - debug/logging için */
expect val platformName: String

/**
 * Ses oynatıcı arayüzü
 * Android: Media3/ExoPlayer (androidMain)
 * iOS: AVFoundation/AVPlayer (iosMain)
 */
interface AudioPlayer {
    fun initialize()
    fun play(url: String)
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(positionMs: Long)
    fun getCurrentPosition(): Long
    fun getDuration(): Long
    fun isPlaying(): Boolean
    fun release()
}

/** Platform'a uygun AudioPlayer oluşturur */
expect fun createAudioPlayer(): AudioPlayer

/**
 * Uygulama config değerleri
 * Android: BuildConfig'den okur
 * iOS: Info.plist veya Codemagic env vars'dan okur
 */
expect object AppConfig {
    val supabaseUrl: String
    val supabaseKey: String
    val cfAccountId: String
    val cfPublicUrl: String
}
