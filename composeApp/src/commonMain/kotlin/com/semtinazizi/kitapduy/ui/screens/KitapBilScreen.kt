package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.ui.components.PremiumUpgradeCard
import com.semtinazizi.kitapduy.ui.theme.SleekChampagne
import com.semtinazizi.kitapduy.ui.theme.PlayfairDisplayFamily
import com.semtinazizi.kitapduy.platform.BannerAdView

class KitapBilScreen(
    val isPremium: Boolean = false,
    val onIndependentPensClick: () -> Unit = {},
    val onMindLibraryClick: () -> Unit = {},
    val onQuizClick: () -> Unit = {},
    val onArticleClick: (String) -> Unit = {},
    val onVideoClick: () -> Unit = {},
    val onPodcastClick: () -> Unit = {}
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        
        // Tema Renkleri
        val softGold = Color(0xFFFFD700)
        val softRed = Color(0xFFFF2D55)
        val softViolet = Color(0xFFE040FB)
        val softBlue = Color(0xFF03A9F4)

        val softCardBackground = MaterialTheme.colorScheme.surface

        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // 🌟 MODERN DİNAMİK ARKA PLAN (BLOBLAR) 🌟
            val infiniteTransition = rememberInfiniteTransition(label = "bg_blobs")

            val blobOffset1 by infiniteTransition.animateValue(
                initialValue = IntOffset(-50, -50),
                targetValue = IntOffset(50, 150),
                typeConverter = IntOffset.VectorConverter,
                animationSpec = infiniteRepeatable(animation = tween(10000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
                label = "offset1"
            )

            val blobOffset2 by infiniteTransition.animateValue(
                initialValue = IntOffset(200, 300),
                targetValue = IntOffset(-100, 500),
                typeConverter = IntOffset.VectorConverter,
                animationSpec = infiniteRepeatable(animation = tween(15000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
                label = "offset2"
            )

            Box(
                modifier = Modifier
                    .offset(blobOffset1.x.dp, blobOffset1.y.dp)
                    .size(350.dp)
                    .blur(100.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 120.dp) // Bottom padding for navigation bar spacing
            ) {
                // 🌟 ÜST BAŞLIK 🌟
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp).statusBarsPadding()) {
                    Text(
                        text = "KitapDuy",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Eğlence & Keşfet",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        fontFamily = PlayfairDisplayFamily,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.5).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Zihnini dinlendir, bilgini sına!",
                        color = SleekChampagne.copy(alpha = 0.9f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // 🌟 BENTO KARTLARI (Gerçek Bento Hiyerarşisi) 🌟
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    
                    // Satır 1: Tam Genişlikte HERO Kartı (Arena)
                    ModernNeonBentoCard(
                        title = "Arena",
                        subtitle = "Edebi Bilgini Sına",
                        icon = Icons.Default.SportsEsports,
                        primaryColor = softGold,
                        badgeText = "YARIŞMA",
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    ) { onQuizClick() }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Satır 2: 2'li Grid (Kalemler ve Kitaplık)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ModernNeonBentoCard(
                            title = "Kalemler",
                            subtitle = "Senin Cümlelerin",
                            icon = Icons.Default.Draw,
                            primaryColor = softViolet,
                            badgeText = "YENİ",
                            modifier = Modifier.weight(1f).height(150.dp)
                        ) { onIndependentPensClick() }

                        ModernNeonBentoCard(
                            title = "Zihin Ktp.",
                            subtitle = "Edebi Makaleler",
                            icon = Icons.Default.LocalLibrary,
                            primaryColor = softBlue,
                            modifier = Modifier.weight(1f).height(150.dp)
                        ) { onMindLibraryClick() }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Satır 3: 2'li Grid (Odaklanma ve Radyo Tiyatrosu)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ModernNeonBentoCard(
                            title = "Odaklanma",
                            subtitle = "Zihnini Özgür Bırak",
                            icon = Icons.Default.SelfImprovement,
                            primaryColor = softRed,
                            modifier = Modifier.weight(1f).height(150.dp)
                        ) { onVideoClick() }

                        ModernNeonBentoCard(
                            title = "Radyo Tiyatrosu",
                            subtitle = "Podcast & Tiyatrolar",
                            icon = Icons.Default.Mic,
                            primaryColor = Color(0xFF1DB954), // Spotify Green
                            badgeText = "POPÜLER",
                            modifier = Modifier.weight(1f).height(150.dp)
                        ) {
                            navigator.push(PodcastListScreen())
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (!isPremium) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PremiumUpgradeCard(
                            onClick = { /* navigator.push(SubscriptionPaywallScreen()) */ }
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = softCardBackground.copy(alpha = 0.8f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "SPONSORLU",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                                Spacer(Modifier.height(8.dp))
                                BannerAdView()
                            }
                        }
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun ModernNeonBentoCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    primaryColor: Color,
    badgeText: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "neon_glow")

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "glowAlpha"
    )

    Card(
        modifier = modifier.clickable { onClick() }.shadow(12.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            if (badgeText != null) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd),
                    shape = RoundedCornerShape(10.dp),
                    color = primaryColor.copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, primaryColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = badgeText,
                        color = primaryColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        letterSpacing = 1.sp
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .blur(16.dp)
                            .background(primaryColor.copy(alpha = glowAlpha * 0.4f), CircleShape)
                    )
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.5).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
