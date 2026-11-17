package com.semtinazizi.kitapduy.ui.screens.tabs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.semtinazizi.kitapduy.ui.screens.AskidaKitapScreen

object AskidaTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val title = "Askıda"
            val icon = rememberVectorPainter(Icons.Default.VolunteerActivism)

            return remember {
                TabOptions(
                    index = 4u,
                    title = title,
                    icon = icon
                )
            }
        }

    @Composable
    override fun Content() {
        // Tab içinde kendi navigasyon ağacını ve sekme kök dönüşünü barındırması için TabRootNavigator kullanıyoruz
        TabRootNavigator(AskidaKitapScreen())
    }
}
