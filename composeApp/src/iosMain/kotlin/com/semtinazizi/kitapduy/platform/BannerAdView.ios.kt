package com.semtinazizi.kitapduy.platform

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
actual fun BannerAdView(modifier: Modifier) {
    // iOS için henüz AdMob SDK yapılandırılmadığından boş bir Spacer döndürüyoruz.
    Spacer(modifier = modifier.fillMaxWidth().height(50.dp))
}
