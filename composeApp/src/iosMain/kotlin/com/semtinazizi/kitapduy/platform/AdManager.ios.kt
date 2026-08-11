package com.semtinazizi.kitapduy.platform

/**
 * iOS AdManager Stub
 * Gerçek AdMob iOS SDK entegrasyonu ileride buraya eklenecek.
 * Şimdilik boş implementasyon — uygulama derlenebilir kalır.
 */
actual class AdManager {
    actual val bannerAdUnitId: String = ""

    actual fun loadInterstitial() {
        // iOS AdMob entegrasyonu ilerleyen aşamada eklenecek
    }

    actual fun showInterstitialIfReady() {
        // iOS AdMob entegrasyonu ilerleyen aşamada eklenecek
    }
}

actual fun createAdManager(): AdManager = AdManager()
