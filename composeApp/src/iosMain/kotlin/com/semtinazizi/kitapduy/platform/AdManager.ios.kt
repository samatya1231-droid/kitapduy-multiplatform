package com.semtinazizi.kitapduy.platform

/**
 * iOS AdMob implementasyonu.
 * Xcode projesine GoogleMobileAds SDK eklendikten sonra
 * Swift tarafından atanacak fonksiyonlarla çalışır.
 */
object AdMobSwiftBridge {
    var loadInterstitialAd: (() -> Unit)? = null
    var showInterstitialAdIfReady: (() -> Unit)? = null
}

actual class AdManager {
    // iOS banner reklam birim ID'si
    actual val bannerAdUnitId: String = "ca-app-pub-1229684690212629/9310593353" // Real ID

    // iOS Interstitial reklam birim ID'si (Swift tarafında kullanılmak üzere referans)
    val interstitialAdUnitId: String = "ca-app-pub-1229684690212629/2226310253"

    actual fun loadInterstitial() {
        AdMobSwiftBridge.loadInterstitialAd?.invoke()
    }

    actual fun showInterstitialIfReady() {
        AdMobSwiftBridge.showInterstitialAdIfReady?.invoke()
    }
}

actual fun createAdManager(): AdManager = AdManager()
