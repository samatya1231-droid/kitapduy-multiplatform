package com.semtinazizi.kitapduy.platform

/**
 * iOS AdMob implementasyonu.
 * Xcode projesine GoogleMobileAds SDK eklendikten sonra
 * Swift tarafÄ±ndan atanacak fonksiyonlarla Ã§alÄ±ÅŸÄ±r.
 */
object AdMobSwiftBridge {
    var loadInterstitialAd: (() -> Unit)? = null
    var showInterstitialAdIfReady: (() -> Unit)? = null
}

actual class AdManager {
    // iOS banner reklam birim ID'si
    actual val bannerAdUnitId: String = "ca-app-pub-1229684690212629/9310593353" // Real ID

    // iOS Interstitial reklam birim ID'si (Swift tarafÄ±nda kullanÄ±lmak Ã¼zere referans)
    val interstitialAdUnitId: String = "ca-app-pub-1229684690212629/2226310253"

    actual fun loadInterstitial() {
        AdMobSwiftBridge.loadInterstitialAd?.invoke()
    }

    actual fun showInterstitialIfReady() {
        AdMobSwiftBridge.showInterstitialAdIfReady?.invoke()
    }

    actual fun showRewardedAd(onRewarded: () -> Unit) {
        // TODO: iOS Reward Ad implementasyonu Swift bridge uzerinden yapilacak
        onRewarded()
    }

    actual fun grantAdFreeTime() {
        // TODO: NSUserDefaults ile iOS kisminda saklanabilir
    }

    actual fun isAdFreeActive(): Boolean {
        // TODO: NSUserDefaults ile iOS kisminda kontrol edilecek
        return false
    }
}

actual fun createAdManager(): AdManager = AdManager()

