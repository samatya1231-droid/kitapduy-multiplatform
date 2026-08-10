package com.semtinazizi.kitapduy.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SleekChampagne,
    onPrimary = Color.Black,
    primaryContainer = SleekChampagne.copy(alpha = 0.12f),
    onPrimaryContainer = SleekChampagne,
    secondary = SleekBronze,
    onSecondary = Color.White,
    tertiary = PremiumBronze,
    background = SleekDarkBackground, 
    onBackground = SleekOnSurface,    
    surface = SleekSurface,           
    onSurface = SleekOnSurface,
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = SleekGray
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF7D5233), // Deep Bronze for literary feel
    onPrimary = Color.White,
    primaryContainer = Color(0xFF7D5233).copy(alpha = 0.1f),
    onPrimaryContainer = Color(0xFF7D5233),
    secondary = Color(0xFFB08D6A),
    onSecondary = Color.White,
    tertiary = PremiumBronze,
    background = Color(0xFFF5F0EB), // Sıcak, krem/kağıt tonu – göz yormuyor
    onBackground = PremiumNavy,
    surface = Color(0xFFFAF7F4),   // Neredeyse beyaz ama hafif sıcak krem
    onSurface = PremiumNavy,
    surfaceVariant = Color(0xFFEDE8E1),
    onSurfaceVariant = Color(0xFF5A5A5C)
)

@Composable
fun KitapduyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We disable dynamic color to strictly maintain our new literary brand identity
    dynamicColor: Boolean = false, 
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}