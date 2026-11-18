package com.semtinazizi.kitapduy.ui.screens.tabs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.compositionLocalOf
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator

internal val LocalTabRootRequest = compositionLocalOf { 0 }

@Composable
internal fun TabRootNavigator(
    screen: Screen,
    content: @Composable (Navigator) -> Unit = { CurrentScreen() }
) {
    Navigator(screen) { navigator ->
        val request = LocalTabRootRequest.current
        val handledRequest = rememberSaveable { mutableIntStateOf(request) }
        LaunchedEffect(request, navigator) {
            if (handledRequest.intValue != request) {
                val firstScreen = navigator.items.firstOrNull()
                if (firstScreen != null && navigator.size > 1) {
                    navigator.popUntil { it == firstScreen }
                }
                handledRequest.intValue = request
            }
        }
        content(navigator)
    }
}
