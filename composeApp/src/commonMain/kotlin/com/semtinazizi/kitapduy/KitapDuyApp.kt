package com.semtinazizi.kitapduy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import com.semtinazizi.kitapduy.platform.IosSmokeSimulationBridge
import com.semtinazizi.kitapduy.platform.isSimulatorCiSmokeTest


/**
 * KitapDuyApp - Ana Compose composable
 * Hem Android (MainActivity) hem iOS (MainViewController) tarafından çağrılır
 * Bu dosya commonMain'de olduğu için her iki platformda da çalışır
 */
import com.semtinazizi.kitapduy.ui.screens.MainScreen

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.semtinazizi.kitapduy.ui.screens.LoginScreen
import com.semtinazizi.kitapduy.ui.viewmodel.AuthViewModel
import org.koin.compose.viewmodel.koinViewModel
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
        Navigator(SplashScreen()) { navigator ->
            if (isSimulatorCiSmokeTest) {
                DisposableEffect(Unit) {
                    println("KITAPDUY_CI_UI_APPEARED")
                    onDispose {}
                }
                val targetScreen by IosSmokeSimulationBridge.targetScreen.collectAsState()
                val popTrigger by IosSmokeSimulationBridge.popTrigger.collectAsState()

                LaunchedEffect(targetScreen) {
                    targetScreen?.let {
                        navigator.push(it)
                        IosSmokeSimulationBridge.clearTargetScreen()
                    }
                }
                LaunchedEffect(popTrigger) {
                    if (popTrigger > 0 && navigator.canPop) {
                        try {
                            navigator.pop()
                        } catch (_: Throwable) {}
                    }
                }
            }
            CurrentScreen()
        }
    }
}
