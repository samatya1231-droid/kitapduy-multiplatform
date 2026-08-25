package com.semtinazizi.kitapduy.ui.screens.tabs

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.semtinazizi.kitapduy.ui.components.PremiumUpgradeCard
import com.semtinazizi.kitapduy.ui.screens.MindLibraryScreen
import com.semtinazizi.kitapduy.ui.screens.QuizScreen
import com.semtinazizi.kitapduy.ui.screens.SubscriptionPaywallScreen
import com.semtinazizi.kitapduy.ui.theme.PlayfairDisplayFamily
import com.semtinazizi.kitapduy.ui.theme.SleekChampagne
import com.semtinazizi.kitapduy.ui.viewmodel.AuthViewModel
import com.semtinazizi.kitapduy.platform.BannerAdView
import org.koin.compose.viewmodel.koinViewModel

import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

object EntertainmentTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val title = "Eğlence"
            val icon = rememberVectorPainter(Icons.Default.Extension)
            
            return remember {
                TabOptions(
                    index = 3u,
                    title = title,
                    icon = icon
                )
            }
        }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val parentNavigator = navigator.parent ?: navigator
        val authViewModel = koinViewModel<AuthViewModel>()
        val homeViewModel = koinViewModel<com.semtinazizi.kitapduy.ui.viewmodel.HomeViewModel>()
        
        val userProfile by authViewModel.userProfile.collectAsState()
        val isPremium = userProfile?.hasPremium() ?: false
        val podcasts by homeViewModel.podcasts.collectAsState()

        // Tema Renkleri
        val softGold = Color(0xFFFFD700)
        val softRed = Color(0xFFFF2D55)
        val softViolet = Color(0xFFE040FB)
        val softBlue = Color(0xFF03A9F4)

        val softCardBackground = MaterialTheme.colorScheme.surface

        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // ?? MODERN DİNAMİK ARKA PLAN (BLOBLAR) ??
            val infiniteTransition = rememberInfiniteTransition(label = "bg_blobs")

            val blobOffset1X by infiniteTransition.animateFloat(
                initialValue = -50f, targetValue = 50f,
                animationSpec = infiniteRepeatable(animation = tween(10000, easing = LinearEasing), repeatMode = RepeatMode.Reverse)
            )
            val blobOffset1Y by infiniteTransition.animateFloat(
                initialValue = -50f, targetValue = 150f,
                animationSpec = infiniteRepeatable(animation = tween(10000, easing = LinearEasing), repeatMode = RepeatMode.Reverse)
            )

            val blobOffset2X by infiniteTransition.animateFloat(
                initialValue = 200f, targetValue = -100f,
                animationSpec = infiniteRepeatable(animation = tween(15000, easing = LinearEasing), repeatMode = RepeatMode.Reverse)
            )
            val blobOffset2Y by infiniteTransition.animateFloat(
                initialValue = 300f, targetValue = 500f,
                animationSpec = infiniteRepeatable(animation = tween(15000, easing = LinearEasing), repeatMode = RepeatMode.Reverse)
            )

            Box(
                modifier = Modifier
                    .offset(x = blobOffset1X.dp, y = blobOffset1Y.dp)
                    .size(350.dp)
                    .blur(100.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 120.dp) // Bottom padding for navigation bar spacing
            ) {
                // ?? ÜST BAŞLIK ??
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

                // ?? BENTO KARTLARI (Gerçek Bento Hiyerarşisi) ??
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    
                    // Satır 1: Tam Genişlikte HERO Kartı (Arena)
                    ModernNeonBentoCard(
                        title = "Arena",
                        subtitle = "Edebi Bilgini Sına",
                        icon = Icons.Default.SportsEsports,
                        primaryColor = softGold,
                        badgeText = "YARIŞMA",
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    ) { parentNavigator.push(QuizScreen()) }

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
                        ) { parentNavigator.push(com.semtinazizi.kitapduy.ui.screens.IndependentPensScreen()) }

                        ModernNeonBentoCard(
                            title = "Zihin Ktp.",
                            subtitle = "Edebi Makaleler",
                            icon = Icons.Default.LocalLibrary,
                            primaryColor = softBlue,
                            modifier = Modifier.weight(1f).height(150.dp)
                        ) { parentNavigator.push(com.semtinazizi.kitapduy.ui.screens.ScienceArticlesScreen()) }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Satır 3: 2'li Grid (Yazar Tahmin ve Radyo Tiyatrosu)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ModernNeonBentoCard(
                            title = "Yazar Tahmin",
                            subtitle = "Kimin Kalemi?",
                            icon = Icons.Default.SelfImprovement,
                            primaryColor = softRed,
                            badgeText = "OYUN",
                            modifier = Modifier.weight(1f).height(150.dp)
                        ) { parentNavigator.push(com.semtinazizi.kitapduy.ui.screens.GuessAuthorScreen()) }

                        ModernNeonBentoCard(
                            title = "Radyo Tiyatrosu",
                            subtitle = "Podcast & Tiyatrolar",
                            icon = Icons.Default.Mic,
                            primaryColor = Color(0xFF1DB954), // Spotify Green
                            badgeText = "POPÜLER",
                            modifier = Modifier.weight(1f).height(150.dp)
                        ) { parentNavigator.push(com.semtinazizi.kitapduy.ui.screens.PodcastListScreen()) }
                    }
                }

                if (podcasts.isNotEmpty()) {
                    Column(modifier = Modifier.padding(top = 32.dp)) {
                        Text(
                            text = "Öne Çıkan Podcastler",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = SleekChampagne),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(podcasts) { pod ->
                                Column(
                                    modifier = Modifier.width(120.dp).clickable {
                                        parentNavigator.push(com.semtinazizi.kitapduy.ui.screens.PodcastEpisodesScreen(pod.id, pod.name, pod.rssUrl))
                                    }
                                ) {
                                    coil3.compose.AsyncImage(
                                        model = pod.imageUrl,
                                        contentDescription = pod.name,
                                        modifier = Modifier.size(120.dp).clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = pod.name,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.White),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
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
                            onClick = { parentNavigator.push(SubscriptionPaywallScreen()) }
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
                    // Neon Aura
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .blur(16.dp)
                            .background(primaryColor.copy(alpha = glowAlpha * 0.4f), CircleShape)
                    )
                    // Ana İkon
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
