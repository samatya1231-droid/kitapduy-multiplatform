import os

path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\HomeScreen.kt'

content = '''package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.viewmodel.koinViewModel
import com.semtinazizi.kitapduy.ui.viewmodel.HomeViewModel
import com.semtinazizi.kitapduy.ui.viewmodel.AuthViewModel
import com.semtinazizi.kitapduy.ui.components.*
import coil3.compose.AsyncImage

class HomeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val homeViewModel = koinViewModel<HomeViewModel>()
        val authViewModel = koinViewModel<AuthViewModel>()

        val userProfile by authViewModel.userProfile.collectAsState()
        val photoUrl = userProfile?.photoUrl
        val isPremium = userProfile?.isPremium == true
        
        val recommendedBooks by homeViewModel.recommendedBooks.collectAsState()
        val allBooks by homeViewModel.books.collectAsState()
        
        val displayBook = allBooks.firstOrNull() // Continue Listening placeholder

        val lazyListState = rememberLazyListState()

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Kitapdinler Style Blurry Background Header
                if (allBooks.isNotEmpty()) {
                    val topBookUrl = allBooks.firstOrNull { it.coverUrl.isNotEmpty() }?.coverUrl
                    if (topBookUrl != null) {
                        AsyncImage(
                            model = topBookUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                                .blur(50.dp)
                                .graphicsLayer { alpha = 0.15f },
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                                    )
                                )
                        )
                    }
                }
                
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            // 1. ROW: Greeting & Profile
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Merhaba Okur 👋", // Will implement time-based later
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (!isPremium) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clickable { navigator.push(SubscriptionPaywallScreen()) }
                                                .background(
                                                    brush = Brush.linearGradient(colors = listOf(Color(0xFFFFD700), Color(0xFFFFA500))),
                                                    shape = RoundedCornerShape(16.dp)
                                                )
                                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Filled.Star, contentDescription = "Premium", tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("PREMİUM", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 0.5.sp)
                                        }
                                    }

                                    if (!photoUrl.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = photoUrl,
                                            contentDescription = "Profile",
                                            modifier = Modifier.size(38.dp).clip(CircleShape).clickable { navigator.push(UserProfileScreen()) },
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).clickable { navigator.push(UserProfileScreen()) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Filled.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(24.dp))

                            // 2. ROW: Search Bar (Sleek Pill)
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 8.dp,
                                tonalElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Filled.Search, contentDescription = "Ara", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(22.dp))
                                    Text("Kitap, yazar veya kategori ara...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    // 3. CONTINUE LISTENING
                    if (displayBook != null) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                                SpotifySectionHeader("Dinlemeye Devam Et")
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .height(80.dp)
                                        .clickable { navigator.push(BookDetailScreen(displayBook)) },
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                                        AsyncImage(
                                            model = displayBook.coverUrl,
                                            contentDescription = null,
                                            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
                                            Text("SESLİ KİTAP", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            Text(displayBook.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Spacer(modifier = Modifier.weight(1f))
                                            LinearProgressIndicator(
                                                progress = { 0.35f },
                                                modifier = Modifier.fillMaxWidth().padding(end = 16.dp).height(4.dp).clip(CircleShape),
                                                color = Color(0xFFFFD700),
                                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                            )
                                        }
                                        Icon(Icons.Filled.PlayArrow, contentDescription = "Oynat", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(end = 16.dp).size(28.dp))
                                    }
                                }
                            }
                        }
                    }

                    // 4. RECOMMENDED
                    if (recommendedBooks.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 32.dp)) {
                                SpotifySectionHeader("Senin İçin Seçildi")
                                TrendingTop10List(
                                    topBooks = recommendedBooks.take(10),
                                    onBookClick = { navigator.push(BookDetailScreen(it)) }
                                )
                            }
                        }
                    }

                    // 5. Featured
                    if (allBooks.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(32.dp))
                            val featuredBook = allBooks.random()
                            HeroBookCard(
                                book = featuredBook,
                                onClick = { navigator.push(BookDetailScreen(featuredBook)) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
'''
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Created perfect replica of kitapduy3 HomeScreen.")
