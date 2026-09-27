package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen

class PremiumWrappedScreen : Screen {
    @Composable override fun Content() { ReadingSummaryContent(true) }
}
