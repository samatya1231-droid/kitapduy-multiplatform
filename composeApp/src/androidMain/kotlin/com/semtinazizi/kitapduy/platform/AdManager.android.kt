package com.semtinazizi.kitapduy.platform

import android.app.Activity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.OnUserEarnedRewardListener
import android.content.Context
import android.widget.Toast
import android.content.SharedPreferences

/**
 * Android AdMob implementasyonu.
 * Activity referansı interstitial göstermek için gereklidir.
 */
actual class AdManager(private val activity: Activity) {

    actual val bannerAdUnitId: String = "ca-app-pub-1399698754234680/6817219979"

    private var interstitialAd: InterstitialAd? = null
    private val interstitialAdUnitId = "ca-app-pub-1399698754234680/3044640980"
    private var rewardedAd: RewardedAd? = null
    private val rewardedAdUnitId = "ca-app-pub-1399698754234680/1628173449"
    
    private val prefs: SharedPreferences = activity.getSharedPreferences("KitapDuyPrefs", Context.MODE_PRIVATE)

    init {
        MobileAds.initialize(activity) {}
        loadRewardedAd()
    }

    private fun loadRewardedAd() {
        RewardedAd.load(
            activity,
            rewardedAdUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) { rewardedAd = null }
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            rewardedAd = null
                            loadRewardedAd()
                        }
                    }
                }
            }
        )
    }

    actual fun showRewardedAd(onRewarded: () -> Unit) {
        rewardedAd?.show(activity, OnUserEarnedRewardListener {
            onRewarded()
        }) ?: run {
            // E�er reklam y�klenmemi�se, kullan�c�y� bekletmemek i�in direkt �d�l� veriyoruz
            Toast.makeText(activity, "Reklam y�klenemedi. Joker hediye edildi!", Toast.LENGTH_SHORT).show()
            onRewarded()
            loadRewardedAd()
        }
    }

    actual fun grantAdFreeTime() {
        val newEndTime = System.currentTimeMillis() + (30 * 60 * 1000) // 30 mins
        prefs.edit().putLong("AdFreeEndTime", newEndTime).apply()
        Toast.makeText(activity, "30 Dakika Reklams�z S�reniz Ba�lad�!", Toast.LENGTH_LONG).show()
    }

    actual fun isAdFreeActive(): Boolean {
        val endTime = prefs.getLong("AdFreeEndTime", 0L)
        return System.currentTimeMillis() < endTime
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
