package com.semtinazizi.kitapduy.platform

import android.app.Activity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Android AdMob implementasyonu.
 * Activity referansı interstitial göstermek için gereklidir.
 */
actual class AdManager(private val activity: Activity) {

    actual val bannerAdUnitId: String = "ca-app-pub-1399698754234680/6817219979"

    private var interstitialAd: InterstitialAd? = null
    private val interstitialAdUnitId = "ca-app-pub-1399698754234680/3044640980"

    init {
        MobileAds.initialize(activity) {}
    }

    actual fun loadInterstitial() {
        InterstitialAd.load(
            activity,
            interstitialAdUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            loadInterstitial() // Sonraki gösterim için önceden yükle
                        }
                    }
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    actual fun showInterstitialIfReady() {
        interstitialAd?.show(activity) ?: run {
            // Reklam hazır değilse yüklemeyi tekrar dene
            loadInterstitial()
        }
    }
}

actual fun createAdManager(): AdManager {
    // Android'de Activity gerekli; DI veya platform context ile sağlanacak.
    // Geçici olarak placeholder — gerçek çağrı MainActivity içinden yapılacak.
    error("createAdManager() Android'de Activity context gerektiriyor. Doğrudan AdManager(activity) kullanın.")
}
