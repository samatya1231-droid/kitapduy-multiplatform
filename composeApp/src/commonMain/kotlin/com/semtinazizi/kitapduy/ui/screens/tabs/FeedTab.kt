package com.semtinazizi.kitapduy.ui.screens.tabs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.semtinazizi.kitapduy.ui.screens.SocialFeedScreen

object FeedTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val title = "Akış"
            val icon = rememberVectorPainter(Icons.AutoMirrored.Filled.List)
            
            return remember {
                TabOptions(
                    index = 1u,
                    title = title,
                    icon = icon
                )
            }
        }

    @Composable
    override fun Content() {
        TabRootNavigator(SocialFeedScreen())
    }
}
