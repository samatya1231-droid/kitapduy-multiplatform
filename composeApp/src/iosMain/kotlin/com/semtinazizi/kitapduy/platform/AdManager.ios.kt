package com.semtinazizi.kitapduy.platform

/**
 * iOS AdMob implementasyonu.
 * Xcode projesine GoogleMobileAds SDK eklendikten sonra
 * Swift tarafÄ±ndan atanacak fonksiyonlarla Ã§alÄ±ÅŸÄ±r.
 */
object AdMobSwiftBridge {
    var loadInterstitialAd: (() -> Unit)? = null
    var showInterstitialAdIfReady: (() -> Unit)? = null
    var showRewardedAd: ((() -> Unit) -> Unit)? = null
}

actual class AdManager {
    // iOS banner reklam birim ID'si
    actual val bannerAdUnitId: String = "ca-app-pub-1229684690212629/9310593353" // Real ID

    // iOS Interstitial reklam birim ID'si (Swift tarafında kullanılmak üzere referans)
    val interstitialAdUnitId: String = "ca-app-pub-1229684690212629/2226310253"

    // iOS Rewarded reklam birim ID'si
    val rewardedAdUnitId: String = "ca-app-pub-1229684690212629/2810760779"

    actual fun loadInterstitial() {
        AdMobSwiftBridge.loadInterstitialAd?.invoke()
    }

    actual fun showInterstitialIfReady() {
        AdMobSwiftBridge.showInterstitialAdIfReady?.invoke()
    }

    actual fun showRewardedAd(onRewarded: () -> Unit) {
        if (AdMobSwiftBridge.showRewardedAd != null) {
            AdMobSwiftBridge.showRewardedAd?.invoke(onRewarded)
        } else {
            onRewarded()
        }
    }

    actual fun grantAdFreeTime() {
        val newEndTime = platform.Foundation.NSDate.date().timeIntervalSince1970() * 1000 + (30 * 60 * 1000)
        platform.Foundation.NSUserDefaults.standardUserDefaults.setDouble(newEndTime, "AdFreeEndTime")
    }

    actual fun isAdFreeActive(): Boolean {
        val endTime = platform.Foundation.NSUserDefaults.standardUserDefaults.doubleForKey("AdFreeEndTime")
        return (platform.Foundation.NSDate.date().timeIntervalSince1970() * 1000) < endTime
    }
}

actual fun createAdManager(): AdManager = AdManager()

