package com.semtinazizi.kitapduy.platform

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import org.koin.compose.koinInject

@Composable
actual fun BannerAdView(modifier: Modifier) {
    val adManager = koinInject<AdManager>()

    // Emülatörde gerçek reklamlar yüklenmez.
    // Test modu: Google'ın resmi test banner unit ID'si (ca-app-pub-3940256099942544/6300978111)
    // Gerçek cihazda her zaman gerçek unit ID kullanılır.
    val testBannerUnitId = "ca-app-pub-3940256099942544/6300978111"
    val resolvedAdUnitId = if (isEmulator()) testBannerUnitId else adManager.bannerAdUnitId

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = resolvedAdUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

/** Emülatör ortamında çalışıp çalışmadığını kontrol eder */
private fun isEmulator(): Boolean {
    return (android.os.Build.FINGERPRINT.startsWith("generic")
            || android.os.Build.FINGERPRINT.startsWith("unknown")
            || android.os.Build.MODEL.contains("google_sdk")
            || android.os.Build.MODEL.contains("Emulator")
            || android.os.Build.MODEL.contains("Android SDK built for x86")
            || android.os.Build.MANUFACTURER.contains("Genymotion")
            || android.os.Build.BRAND.startsWith("generic")
            || android.os.Build.DEVICE.startsWith("generic"))
}
