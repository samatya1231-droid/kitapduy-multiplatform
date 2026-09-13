package com.semtinazizi.kitapduy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.semtinazizi.kitapduy.util.KmpThemeManager

@Composable
fun KitapduyTheme(
    content: @Composable () -> Unit
) {
    val systemDefaultDark = isSystemInDarkTheme()
    val isDark = KmpThemeManager.isDarkMode(systemDefaultDark)
    
    // We build the ColorScheme dynamically inside the @Composable so it observes
    // the mutableStateOf variables from Color.kt (SleekChampagne, SleekDarkBackground etc.)
    val DarkColorScheme = darkColorScheme(
        primary = SleekChampagne,
        onPrimary = Color.Black,
        primaryContainer = SleekChampagne.copy(alpha = 0.2f),
        onPrimaryContainer = SleekChampagne,
        secondary = SleekBronze,
        onSecondary = Color.Black,
        tertiary = SleekBronze,
        background = SleekDarkBackground,
        onBackground = SleekOnSurface,
        surface = SleekSurface,
        onSurface = SleekOnSurface,
        surfaceVariant = SleekSurface.copy(alpha = 0.7f),
        onSurfaceVariant = SleekGray
    )

    val LightColorScheme = lightColorScheme(
        primary = Color(0xFF8B6B00), // Gold-ish brown for light mode
        secondary = Color(0xFF625b71),
        tertiary = Color(0xFF7D5260),
        background = PremiumPaper,
        onBackground = PremiumDarkBackground,
        surface = Color.White,
        onSurface = PremiumDarkBackground
    )

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
