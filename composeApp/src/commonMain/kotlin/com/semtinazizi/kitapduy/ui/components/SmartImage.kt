package com.semtinazizi.kitapduy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage

import androidx.compose.runtime.remember
import com.semtinazizi.kitapduy.platform.AppConfig

@Composable
fun SmartImage(
    imageUrl: String?,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    isAvatar: Boolean = false,
    fallbackText: String = ""
) {
    val finalUrl = remember(imageUrl) {
        var targetUrl = imageUrl ?: ""
        if (targetUrl.isNotBlank()) {
            val isR2Host = targetUrl.contains("r2.cloudflarestorage.com") || 
                           targetUrl.contains(".r2.dev") || 
                           targetUrl.contains("workers.dev")
            
            if (isR2Host) {
                // workers.dev, r2.dev veya r2.cloudflarestorage.com geldiğinde, path'i ayırıp AppConfig.cfWorkerUrl üzerinden geçirelim
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

                targetUrl = "${AppConfig.cfWorkerUrl.trimEnd('/')}/$cleanPath"
            }
        }
        targetUrl
    }

    if (finalUrl.isNotBlank()) {
        SubcomposeAsyncImage(
            model = finalUrl,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                }
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isAvatar) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surfaceVariant
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isAvatar) {
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
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = "Görsel Bulunamadı",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxSize(0.5f)
            )
        }
    }
}
