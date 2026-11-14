package com.semtinazizi.kitapduy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.semtinazizi.kitapduy.model.FeedItem
import com.semtinazizi.kitapduy.ui.components.SmartImage

@Composable
fun ProfileBentoStats(
    followersCount: String,
    followingCount: String,
    booksFinished: String,
    streakDays: String = "0",
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val BrandGold = Color(0xFFFFD700)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(96.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BentoStatItem(
                label = "Takipçi",
                value = followersCount,
                icon = Icons.Default.Group,
                color = BrandGold,
                modifier = Modifier.weight(1f),
                onClick = onFollowersClick
            )
            BentoStatItem(
                label = "Takip",
                value = followingCount,
                icon = Icons.Default.PersonAdd,
                color = BrandGold,
                modifier = Modifier.weight(1f),
                onClick = onFollowingClick
            )
        }
        Row(modifier = Modifier.fillMaxWidth().height(96.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BentoStatItem(
                label = "Bitirilen",
                value = booksFinished,
                icon = Icons.Default.AutoStories,
                color = BrandGold,
                modifier = Modifier.weight(1f),
                onClick = null
            )
            BentoStatItem(
                label = "Günlük Seri",
                value = "$streakDays 🔥",
                icon = Icons.Default.LocalFireDepartment,
                color = BrandGold,
                modifier = Modifier.weight(1f),
                onClick = null
            )
        }
    }
}

@Composable
fun AnimatedStatNumber(target: Int, color: Color, fontSize: androidx.compose.ui.unit.TextUnit) {
    var displayed by remember(target) { mutableStateOf(0) }

    LaunchedEffect(target) {
        if (target == 0) { displayed = 0; return@LaunchedEffect }
        val steps = minOf(target, 30)
        val delayMs = (500L / steps.coerceAtLeast(1))
        val increment = (target.toFloat() / steps).coerceAtLeast(1f)
        var current = 0f
        repeat(steps) {
            current += increment
            displayed = current.toInt().coerceAtMost(target)
            delay(delayMs)
        }
        displayed = target
    }

    Text(
        text = displayed.toString(),
        color = color,
        fontSize = fontSize,
        fontWeight = FontWeight.Black
    )
}

@Composable
fun BentoStatItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, label = "scale")
    val numericValue = value.filter { it.isDigit() }.toIntOrNull() ?: 0

    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .scale(scale)
            .shadow(8.dp, RoundedCornerShape(22.dp))
            .semantics(mergeDescendants = true) {}
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(color.copy(alpha = 0.07f), Color.Transparent))
        )) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = (-20).dp)
                    .blur(30.dp)
                    .background(color.copy(alpha = 0.18f))
            )

            Column(
                modifier = Modifier.fillMaxSize().padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                    }
                    if (value.contains("🔥")) {
                        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    } else {
                        AnimatedStatNumber(target = numericValue, color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp)
                    }
                }
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun ReadingDNACard(categories: Map<String, Int>, title: String, modifier: Modifier = Modifier) {
    val totalBooks = categories.values.sum().coerceAtLeast(1)
    val colors = listOf(Color(0xFFFFD700), Color(0xFFE91E63), Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFF9800))

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(16.dp))
            
            if (categories.isEmpty()) {
                Text("Henüz bir okuma DNA'sı oluşmadı.", color = Color.Gray, fontSize = 13.sp)
            } else {
                // Bar
                Row(modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp))) {
                    categories.entries.forEachIndexed { index, entry ->
                        val weight = (entry.value.toFloat() / totalBooks).coerceAtLeast(0.01f)
                        Box(modifier = Modifier.weight(weight).fillMaxHeight().background(colors[index % colors.size]))
                    }
                }
                Spacer(Modifier.height(16.dp))
                // Legend
                categories.entries.forEachIndexed { index, entry ->
                    val percentage = (entry.value.toFloat() / totalBooks * 100).toInt()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(10.dp).background(colors[index % colors.size], CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(entry.key, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text("%$percentage", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFB300))),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("PREMIUM", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun ListeningStatsCard(
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(Color(0xFFFFD700).copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) { Text("🎧", fontSize = 20.sp) }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Dinleme İstatistikleri", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Text("Yolculuğunu detaylı incele", color = Color.Gray, fontSize = 12.sp)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun PremiumUpgradeCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Premium'a Geç", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Text("Reklamsız deneyim ve fazlası", color = Color.LightGray, fontSize = 12.sp)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun FavoriteQuotesGallery(quotes: List<FeedItem>, onQuoteClick: (FeedItem) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            text = "✨ Favori Alıntılar",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(quotes) { quote ->
                Card(
                    modifier = Modifier.width(260.dp).height(160.dp).clickable { onQuoteClick(quote) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Icon(Icons.Default.FormatQuote, null, tint = Color(0xFFFFD700).copy(0.3f), modifier = Modifier.size(40.dp))
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                            Text(
                                text = quote.content,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                maxLines = 4,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(text = "- ${quote.bookTitle}", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuthorCircleItem(name: String, imageUrl: String?, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp).clickable { onClick() }
    ) {
        SmartImage(
            imageUrl = imageUrl,
            modifier = Modifier.size(70.dp).clip(CircleShape).border(2.dp, Color(0xFFFFD700), CircleShape),
            contentScale = ContentScale.Crop,
            isAvatar = true
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = name,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
