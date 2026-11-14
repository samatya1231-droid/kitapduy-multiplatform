package com.semtinazizi.kitapduy.platform

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.UIKit.UIColor
import platform.UIKit.UIView

/**
 * iOS BannerAdView için Swift tarafında atanacak factory.
 * Xcode'da GoogleMobileAds SDK import edildikten sonra
 * ComposeApp.BannerAdFactory.createBanner = { ... } şeklinde atanacak.
 */
object BannerAdFactory {
    var createBanner: (() -> UIView)? = null
    private val canRequestAdsFlow = MutableStateFlow(true)
    val canRequestAds: StateFlow<Boolean> = canRequestAdsFlow.asStateFlow()

    fun setCanRequestAds(enabled: Boolean) {
        canRequestAdsFlow.value = enabled
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun BannerAdView(modifier: Modifier) {
    if (IOSBuildConfig.isCiSmokeTest) return
    val canRequestAds by BannerAdFactory.canRequestAds.collectAsState()
    if (!canRequestAds) return
    val factory = BannerAdFactory.createBanner ?: return
    val bgColor = MaterialTheme.colorScheme.background
    val containerUIColor = remember(bgColor) {
        UIColor.colorWithRed(
            red = bgColor.red.toDouble(),
            green = bgColor.green.toDouble(),
            blue = bgColor.blue.toDouble(),
            alpha = bgColor.alpha.toDouble()
        )
    }
    val isOpaqueBg = bgColor.alpha >= 1f
    UIKitView(
        factory = {
            factory().apply {
                backgroundColor = containerUIColor
                setOpaque(isOpaqueBg)
                superview?.backgroundColor = containerUIColor
                superview?.setOpaque(isOpaqueBg)
            }
        },
        modifier = modifier.fillMaxWidth().height(60.dp),
        update = { view ->
            view.backgroundColor = containerUIColor
            view.setOpaque(isOpaqueBg)
            view.superview?.backgroundColor = containerUIColor
            view.superview?.setOpaque(isOpaqueBg)
        }
    )
}
