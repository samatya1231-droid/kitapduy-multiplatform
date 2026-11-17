package com.semtinazizi.kitapduy.ui.screens.tabs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.semtinazizi.kitapduy.ui.screens.HomeScreen

object HomeTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val title = "Ana Sayfa"
            val icon = rememberVectorPainter(Icons.Default.Home)
            
            return remember {
                TabOptions(
                    index = 0u,
                    title = title,
                    icon = icon
                )
            }
        }

    @Composable
    override fun Content() {
        TabRootNavigator(HomeScreen())
    }
}
