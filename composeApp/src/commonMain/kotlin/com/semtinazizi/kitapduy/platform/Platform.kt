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

