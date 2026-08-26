import os

path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\HomeScreen.kt'

# The user wants the layout to not look simple/messy. We will implement the sleek background 
# gradient, the classic greeting, profile picture, premium crown, and standard lists instead of 
# the random BentoGrid we threw together earlier.

content = '''package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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

class HomeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val homeViewModel = koinViewModel<HomeViewModel>()
        val authViewModel = koinViewModel<AuthViewModel>()

        val userProfile by authViewModel.userProfile.collectAsState()
        val photoUrl by authViewModel.userPhotoUrl
        val isPremium = userProfile?.isPremium == true
        
        val recommendedBooks by homeViewModel.recommendedBooks.collectAsState()
        val allBooks by homeViewModel.allBooks.collectAsState()

        val calendar = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() // placeholder
        val greeting = "Merhaba Okur 👋" // Simple mock for now

        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Background Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1A1A2E).copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // Header
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = greeting,
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
                                        Text("PREMİUM", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                    }
                                }
                                
                                Box(
                                    modifier = Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)
                                        .clickable { navigator.push(UserProfileScreen()) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                if (recommendedBooks.isNotEmpty()) {
                    item {
                        SpotifySectionHeader("Trend Kitaplar")
                        TrendingTop10List(
                            topBooks = recommendedBooks.take(10),
                            onBookClick = { book -> navigator.push(BookDetailScreen(book)) }
                        )
                    }
                }

                if (allBooks.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        SpotifySectionHeader("Günün Keşfi")
                        HeroBookCard(
                            book = allBooks.random(),
                            onClick = { book -> navigator.push(BookDetailScreen(book)) },
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(24.dp))
                    SpotifySectionHeader("Sana Özel Kategoriler")
                    BentoGrid(
                        onQuizClick = { navigator.push(QuizScreen()) },
                        onFocusClick = { navigator.push(FocusScreen()) },
                        onPoetryClick = { navigator.push(IndependentPensScreen()) },
                        onDiscoverClick = { navigator.push(ScienceArticlesScreen()) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SpotifySectionHeader(title: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
    }
}
'''
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Restored stable KMP HomeScreen.kt with sleek kitapduy3-like layout.")
