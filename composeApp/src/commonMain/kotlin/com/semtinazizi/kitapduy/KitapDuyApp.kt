package com.semtinazizi.kitapduy

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator


/**
 * KitapDuyApp - Ana Compose composable
 * Hem Android (MainActivity) hem iOS (MainViewController) tarafından çağrılır
 * Bu dosya commonMain'de olduğu için her iki platformda da çalışır
 */
import com.semtinazizi.kitapduy.ui.screens.HomeScreen

@Composable
fun App() {
    Navigator(HomeScreen())
}
