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
 * iOS: AVFoundation/AVQueuePlayer (iosMain)
 */
data class AudioMediaItem(
    val id: String,
    val url: String,
    val title: String = "",
    val artist: String = "",
    val artworkUrl: String = ""
)

interface AudioPlayer {
    fun initialize()
    fun play(url: String)
    fun playQueue(items: List<AudioMediaItem>, startIndex: Int = 0, startPositionMs: Long = 0L)
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(positionMs: Long)
    fun getCurrentPosition(): Long
    fun getDuration(): Long
    fun isPlaying(): Boolean
    fun setPlaybackSpeed(speed: Float)
    fun playAtmosphere(url: String, volume: Float = 0.5f)
    fun stopAtmosphere()
    fun setAtmosphereVolume(volume: Float)
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
    val cfWorkerUrl: String
    val cfUploadToken: String
}

// ─────────────────────────────────────────────────────────────────────────────
// REKLAM (AD) YÖNETİCİSİ
// Android: Google AdMob   |   iOS: Stub (ileride AdMob iOS eklenebilir)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Reklam yönetimi için ortak arayüz.
 * Compose'da Banner reklam göstermek için [BannerAdView] kullanılır.
 */
expect class AdManager {
    /** Banner reklam unitId'si */
    val bannerAdUnitId: String
    /** Interstitial (tam ekran) reklam yükler */
    fun loadInterstitial()
    /** Yüklü interstitial reklamı gösterir. Yüklü değilse sessizce geçer. */
    fun showInterstitialIfReady()
}

/** Platform'a özgü AdManager oluşturur */
expect fun createAdManager(): AdManager

// ─────────────────────────────────────────────────────────────────────────────
// ÖDEME (BILLING) YÖNETİCİSİ
// Android: Google Play Billing   |   iOS: Stub (ileride StoreKit eklenebilir)
// ─────────────────────────────────────────────────────────────────────────────

/** Satın alma sonucu */
enum class PurchaseResult {
    SUCCESS, CANCELLED, ERROR
}

/**
 * Satın alma yönetimi için ortak arayüz.
 * [isPremium] akışını dinleyerek UI premium/free arasında otomatik güncellenir.
 */
expect class BillingManager {
    /** Kullanıcının aktif premium aboneliği var mı? */
    val isPremium: kotlinx.coroutines.flow.StateFlow<Boolean>
    /** Abonelik akışını başlatır (Google Play / App Store) */
    suspend fun startSubscription(productId: String): PurchaseResult
    /** Mevcut satın almaları doğrular ve isPremium'u günceller */
    suspend fun restorePurchases()
}

/** Platform'a özgü BillingManager oluşturur */
expect fun createBillingManager(): BillingManager

// ─────────────────────────────────────────────────────────────────────────────
// DOSYA İNDİRİCİ (FILE DOWNLOADER)
// Android/iOS: Ktor & Okio veya Platform'a özgü downloader
// ─────────────────────────────────────────────────────────────────────────────

interface FileDownloader {
    /** İndirme işlemini başlatır ve lokal dosya yolunu (URI) döner */
    suspend fun downloadFile(url: String, fileName: String, onProgress: (Float) -> Unit): String
    
    /** İndirilmiş dosya var mı kontrol eder, varsa yolunu döner */
    fun getDownloadedFilePath(fileName: String): String?
    
    /** İndirilmiş dosyayı siler */
    fun deleteFile(fileName: String): Boolean
}

expect fun createFileDownloader(): FileDownloader

