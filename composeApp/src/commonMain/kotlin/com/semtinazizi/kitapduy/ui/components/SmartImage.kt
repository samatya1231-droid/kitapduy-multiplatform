package com.semtinazizi.kitapduy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.semtinazizi.kitapduy.platform.AppConfig
import com.semtinazizi.kitapduy.ui.theme.BrandGold
import com.semtinazizi.kitapduy.util.UrlSanitizer
import kotlin.math.abs

private val BookCoverFallbackPalettes = listOf(
    listOf(Color(0xFF5D2A2C), Color(0xFF2D1114)),
    listOf(Color(0xFF243B55), Color(0xFF141E30)),
    listOf(Color(0xFF23443C), Color(0xFF102520)),
    listOf(Color(0xFF4B3626), Color(0xFF261A11)),
    listOf(Color(0xFF3E2942), Color(0xFF1F1223)),
    listOf(Color(0xFF36454F), Color(0xFF1C252B))
)

internal fun resolveSmartImageUrl(imageUrl: String?): String {
    var targetUrl = (imageUrl ?: "").trim()
    if (targetUrl.isBlank()) return ""

    val isLoopback = targetUrl.startsWith("http://localhost", ignoreCase = true) ||
        targetUrl.startsWith("http://127.0.0.1", ignoreCase = true)
    if (!isLoopback && targetUrl.startsWith("http://", ignoreCase = true)) {
        targetUrl = "https://" + targetUrl.removePrefix("http://")
    }

    val workerBase = AppConfig.cfWorkerUrl.trimEnd('/')
    val isR2Host = targetUrl.contains("r2.cloudflarestorage.com") ||
        targetUrl.contains(".r2.dev") ||
        targetUrl.contains("workers.dev")

    if (isR2Host && workerBase.isNotBlank() && !targetUrl.startsWith(workerBase)) {
        var cleanPath = targetUrl
            .substringAfter("r2.cloudflarestorage.com/")
            .substringAfter(".r2.dev/")
            .substringAfter("workers.dev/")
            .substringBefore("?t=")
            .substringBefore("?")

        val bucket = "kitapduyprofiller"
        if (cleanPath.startsWith("$bucket/")) {
            cleanPath = cleanPath.removePrefix("$bucket/")
        } else if (cleanPath.startsWith("/$bucket/")) {
            cleanPath = cleanPath.removePrefix("/$bucket/")
        }
        cleanPath = cleanPath.trimStart('/')

        targetUrl = "$workerBase/$cleanPath"
    }

    return UrlSanitizer.sanitizeUrl(targetUrl)
}

@Composable
fun SmartImage(
    imageUrl: String?,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    isAvatar: Boolean = false,
    fallbackText: String = ""
) {
    val finalUrl = remember(imageUrl) { resolveSmartImageUrl(imageUrl) }

    if (finalUrl.isNotBlank()) {
        SubcomposeAsyncImage(
            model = finalUrl,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
            loading = {
                ImagePlaceholder(isAvatar = isAvatar, fallbackText = fallbackText)
            },
            error = {
                ImagePlaceholder(isAvatar = isAvatar, fallbackText = fallbackText)
            }
        )
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            ImagePlaceholder(isAvatar = isAvatar, fallbackText = fallbackText)
        }
    }
}

@Composable
private fun ImagePlaceholder(isAvatar: Boolean, fallbackText: String) {
    if (isAvatar) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            if (fallbackText.isNotBlank()) {
                Text(
                    text = fallbackText.trim().take(1).uppercase(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Profil",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxSize(0.5f)
                )
            }
        }
        return
    }

    if (fallbackText.isNotBlank()) {
        val palette = BookCoverFallbackPalettes[abs(fallbackText.hashCode()) % BookCoverFallbackPalettes.size]
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(palette))
        ) {
            // Subtle book spine edge
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.14f))
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .border(0.6.dp, BrandGold.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(1.dp)
                        .background(BrandGold.copy(alpha = 0.6f))
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = fallbackText,
                    color = Color(0xFFFDF8EE),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(1.dp)
                        .background(BrandGold.copy(alpha = 0.6f))
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = "Görsel Bulunamadı",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxSize(0.42f)
            )
        }
    }
}
