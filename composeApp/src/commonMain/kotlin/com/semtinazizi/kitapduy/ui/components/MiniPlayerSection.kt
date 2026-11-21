package com.semtinazizi.kitapduy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiniPlayerSection(
    title: String,
    artist: String,
    coverUrl: String,
    isPlaying: Boolean,
    isPremium: Boolean = false,
    progress: Float? = null,
    isBuffering: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onCloseClick: (() -> Unit)? = null,
    onUpgradeClick: () -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current
    val buttonAlpha = 0.9f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .height(72.dp)
            .testTag("mini-player")
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kitap Kapağı
                Box(modifier = Modifier.padding(start = 12.dp)) {
                    SmartImage(
                        imageUrl = coverUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(0.05f)),
                        contentScale = ContentScale.Crop,
                        fallbackText = title
                    )
                }

                // Metin Alanı
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title.ifEmpty { "Seçilen Kitap" },
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = artist.ifEmpty { "Yazar" },
                        maxLines = 1,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.basicMarquee()
                    )
                }

                if (!isPremium) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFFFD700).copy(alpha = buttonAlpha),
                        border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.8f)),
                        modifier = Modifier.padding(end = 12.dp).clickable { 
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onUpgradeClick() 
                        }
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "💎", fontSize = 10.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(text = "Reklamsız", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 9.sp)
                        }
                    }
                }

                // Oynat/Durdur Butonu
                IconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPlayPauseClick()
                    },
                    modifier = Modifier
                        .padding(end = if (onCloseClick != null) 4.dp else 12.dp)
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape)
                        .testTag("mini-player-play-pause")
                        .semantics {
                            contentDescription = when {
                                isBuffering -> "Yükleniyor"
                                isPlaying -> "Duraklat"
                                else -> "Oynat"
                            }
                        }
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Kapat Butonu (Opsiyonel)
                if (onCloseClick != null) {
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCloseClick()
                        },
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(44.dp)
                            .testTag("mini-player-close")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // İlerleme Çubuğu (Opsiyonel)
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            }
        }
    }
}
