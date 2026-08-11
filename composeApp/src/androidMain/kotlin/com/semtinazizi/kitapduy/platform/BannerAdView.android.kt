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
    
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = adManager.bannerAdUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
