package com.semtinazizi.kitapduy.platform

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIColor
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
    if (IOSBuildConfig.isCiSmokeTest) return
    val factory = BannerAdFactory.createBanner ?: return
    UIKitView(
        factory = {
            factory().apply {
                backgroundColor = UIColor.clearColor
                setOpaque(false)
                superview?.backgroundColor = UIColor.clearColor
                superview?.setOpaque(false)
            }
        },
        modifier = modifier.fillMaxWidth().height(60.dp),
        update = { view ->
            view.backgroundColor = UIColor.clearColor
            view.setOpaque(false)
            view.superview?.backgroundColor = UIColor.clearColor
            view.superview?.setOpaque(false)
        }
    )
}
