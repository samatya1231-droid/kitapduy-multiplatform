package com.semtinazizi.kitapduy

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator


/**
 * KitapDuyApp - Ana Compose composable
 * Hem Android (MainActivity) hem iOS (MainViewController) tarafından çağrılır
 * Bu dosya commonMain'de olduğu için her iki platformda da çalışır
 */

import com.semtinazizi.kitapduy.ui.screens.SplashScreen

import com.semtinazizi.kitapduy.ui.theme.KitapduyTheme
import com.semtinazizi.kitapduy.util.KmpThemeManager

import coil3.compose.setSingletonImageLoaderFactory
import coil3.ImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade

@Composable
fun App() {
    KmpThemeManager.applyTheme()
    
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
            }
            .crossfade(true)
            .build()
    }
    
    KitapduyTheme {
        Navigator(SplashScreen()) {
            CurrentScreen()
        }
    }
}
