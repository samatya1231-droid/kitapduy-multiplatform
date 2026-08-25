package com.semtinazizi.kitapduy.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiniPlayerSection(
    title: String,
    artist: String,
    coverUrl: String,
    isPlaying: Boolean,
    isPremium: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onUpgradeClick: () -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current
    val buttonAlpha = 0.9f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .height(72.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Kitap Kapağı
            Box(modifier = Modifier.padding(start = 12.dp)) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(0.05f)),
                    contentScale = ContentScale.Crop
                )
            }

            // Metin Alanı
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title.ifEmpty { "Seçilen Kitap" },
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    fontSize = 14.sp,
                    color = Color.White,
                    modifier = Modifier.basicMarquee()
                )
                Text(
                    text = artist.ifEmpty { "Yazar" },
                    maxLines = 1,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
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
                    .padding(end = 12.dp)
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
