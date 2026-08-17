package com.semtinazizi.kitapduy.platform

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIView

/**
 * iOS BannerAdView için Swift tarafında atanacak factory.
 * Xcode'da GoogleMobileAds SDK import edildikten sonra
 * ComposeApp.BannerAdFactory.createBanner = { ... } şeklinde atanacak.
 */
object BannerAdFactory {
    var createBanner: (() -> UIView)? = null
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun BannerAdView(modifier: Modifier) {
    val factory = BannerAdFactory.createBanner
    if (factory != null) {
        UIKitView(
            factory = { factory() },
            modifier = modifier.fillMaxWidth().height(60.dp),
            update = { view ->
                // Görünüm güncellendiğinde yapılacaklar (şimdilik boş)
            }
        )
    } else {
        Spacer(modifier = modifier.fillMaxWidth().height(60.dp))
    }
}
