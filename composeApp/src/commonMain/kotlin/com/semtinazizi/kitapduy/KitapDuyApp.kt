package com.semtinazizi.kitapduy

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import com.semtinazizi.kitapduy.ui.SplashScreen

/**
 * KitapDuyApp - Ana Compose composable
 * Hem Android (MainActivity) hem iOS (MainViewController) tarafından çağrılır
 * Bu dosya commonMain'de olduğu için her iki platformda da çalışır
 */
@Composable
fun KitapDuyApp() {
    // Voyager Navigator ile uygulama başlar
    Navigator(SplashScreen())
}
