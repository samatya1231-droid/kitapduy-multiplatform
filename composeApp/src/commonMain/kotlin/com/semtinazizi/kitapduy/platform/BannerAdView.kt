package com.semtinazizi.kitapduy.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Banner reklam göstermek için ortak KMP Composable.
 * Android'de AdMob AndroidView çizer, iOS'ta şu anlık Spacer döner.
 */
@Composable
expect fun BannerAdView(modifier: Modifier = Modifier)
