const fs = require('fs');

// 1. Update Platform.kt
let platformPath = 'C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/platform/Platform.kt';
let pCode = fs.readFileSync(platformPath, 'utf8');

if (!pCode.includes('fun showRewardedAd(')) {
    pCode = pCode.replace(
        'fun showInterstitialIfReady()',
        'fun showInterstitialIfReady()\n    /** Ödüllü reklam gösterir */\n    fun showRewardedAd(onRewarded: () -> Unit)'
    );
    
    // Add adFree methods to AdManager
    pCode = pCode.replace(
        'fun showRewardedAd(onRewarded: () -> Unit)',
        'fun showRewardedAd(onRewarded: () -> Unit)\n    /** 30 dakika reklamsýz hakký verir */\n    fun grantAdFreeTime()\n    /** Reklamsýz sürenin dolup dolmadýðýný döner */\n    fun isAdFreeActive(): Boolean'
    );
    fs.writeFileSync(platformPath, pCode, 'utf8');
}

// 2. Update AdManager.android.kt
let androidAdPath = 'C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/androidMain/kotlin/com/semtinazizi/kitapduy/platform/AdManager.android.kt';
let aCode = fs.readFileSync(androidAdPath, 'utf8');

if (!aCode.includes('fun showRewardedAd(')) {
    aCode = aCode.replace(
        'import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback',
        'import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback\nimport com.google.android.gms.ads.rewarded.RewardedAd\nimport com.google.android.gms.ads.rewarded.RewardedAdLoadCallback\nimport com.google.android.gms.ads.OnUserEarnedRewardListener\nimport android.content.Context\nimport android.widget.Toast\nimport android.content.SharedPreferences'
    );
    
    aCode = aCode.replace(
        'private val interstitialAdUnitId = "ca-app-pub-1399698754234680/3044640980"',
        'private val interstitialAdUnitId = "ca-app-pub-1399698754234680/3044640980"\n    private var rewardedAd: RewardedAd? = null\n    private val rewardedAdUnitId = "ca-app-pub-1399698754234680/1628173449"\n    \n    private val prefs: SharedPreferences = activity.getSharedPreferences("KitapDuyPrefs", Context.MODE_PRIVATE)'
    );
    
    const newAndroidMethods = `
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
            // Eðer reklam yüklenmemiþse, kullanýcýyý bekletmemek için direkt ödülü veriyoruz
            Toast.makeText(activity, "Reklam yüklenemedi. Joker hediye edildi!", Toast.LENGTH_SHORT).show()
            onRewarded()
            loadRewardedAd()
        }
    }

    actual fun grantAdFreeTime() {
        val newEndTime = System.currentTimeMillis() + (30 * 60 * 1000) // 30 mins
        prefs.edit().putLong("AdFreeEndTime", newEndTime).apply()
        Toast.makeText(activity, "30 Dakika Reklamsýz Süreniz Baþladý!", Toast.LENGTH_LONG).show()
    }

    actual fun isAdFreeActive(): Boolean {
        val endTime = prefs.getLong("AdFreeEndTime", 0L)
        return System.currentTimeMillis() < endTime
    }
`;

    // Replace the old init { } block
    aCode = aCode.replace(/init \{[\s\S]*?\}/, newAndroidMethods.trim());
    
    fs.writeFileSync(androidAdPath, aCode, 'utf8');
}

// 3. Update AdManager.ios.kt
let iosAdPath = 'C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/iosMain/kotlin/com/semtinazizi/kitapduy/platform/AdManager.ios.kt';
let iCode = fs.readFileSync(iosAdPath, 'utf8');

if (!iCode.includes('fun showRewardedAd(')) {
    iCode = iCode.replace(
        'actual fun showInterstitialIfReady() {}',
        'actual fun showInterstitialIfReady() {}\n    actual fun showRewardedAd(onRewarded: () -> Unit) { onRewarded() }\n    actual fun grantAdFreeTime() {}\n    actual fun isAdFreeActive(): Boolean = false'
    );
    fs.writeFileSync(iosAdPath, iCode, 'utf8');
}

console.log("AdManager updated");
