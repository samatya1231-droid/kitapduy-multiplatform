package com.semtinazizi.kitapduy.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.semtinazizi.kitapduy.ui.theme.PremiumBronze
import com.semtinazizi.kitapduy.ui.theme.PremiumCopper
import kotlin.math.sin

@Preview
@Composable
fun PreviewLibraryBackground() {
    DynamicAuraBackground()
}

@Composable
fun DynamicAuraBackground(
    modifier: Modifier = Modifier,
    genre: String? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LibraryTransition")

    // Mapping genre to color
    val targetLampColor = when (genre?.lowercase()) {
        "korku", "gerilim", "horror", "thriller" -> Color(0xFFD32F2F) // Deep Red
        "bilim kurgu", "sci-fi", "science fiction" -> Color(0xFF00B0FF) // Neon Blue
        "klasikler", "classics" -> Color(0xFF8D6E63) // Sepia/Classic Wood
        "romantik", "romance" -> Color(0xFFF06292) // Soft Rose
        "felsefe", "philosophy" -> Color(0xFF9575CD) // Deep Purple
        else -> Color(0xFFFFB300) // Default Amber Lamp
    }

    val animatedLampColor by animateColorAsState(
        targetValue = targetLampColor,
        animationSpec = tween(1500),
        label = "GenreColorTransition"
    )

    // Slow breathing animation for the lamp glow
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowPulse"
    )

    // Floating light flares
    val flare1Pos by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Flare1"
    )
    val backgroundColor = MaterialTheme.colorScheme.background
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. Solid Base
        drawRect(color = backgroundColor)

        // 3. Adaptive Reading Lamp Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(animatedLampColor.copy(alpha = 0.22f * glowPulse), Color.Transparent),
                center = Offset(width * 0.9f, height * 0.1f),
                radius = width * 1.6f
            ),
            center = Offset(width * 0.9f, height * 0.1f),
            radius = width * 1.6f
        )

        // 4. Subtle Accent Bloom (Reacts to genre too)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(animatedLampColor.copy(alpha = 0.1f * glowPulse), Color.Transparent),
                center = Offset(width * 0.1f, height * 0.8f),
                radius = width * 1.2f
            ),
            center = Offset(width * 0.1f, height * 0.8f),
            radius = width * 1.2f
        )

        // 5. Modern Floating Light Flares
        val f1X = (sin(flare1Pos * kotlin.math.PI.toFloat() * 2f) * 0.3f + 0.5f) * width
        val f1Y = (sin(flare1Pos * kotlin.math.PI.toFloat() * 1.5f) * 0.2f + 0.4f) * height
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(animatedLampColor.copy(alpha = 0.08f), Color.Transparent),
                center = Offset(f1X, f1Y),
                radius = 400f
            ),
            center = Offset(f1X, f1Y),
            radius = 400f
        )

        // 6. Dramatic Vignette
        val isLight = backgroundColor.red > 0.8f && backgroundColor.green > 0.8f && backgroundColor.blue > 0.8f
        val vignetteColor = if (isLight) Color.White else Color.Black
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, vignetteColor.copy(alpha = if (isLight) 0.3f else 0.6f)),
                startY = height * 0.5f
            ),
            topLeft = Offset.Zero,
            size = Size(width, height)
        )
    }
}
