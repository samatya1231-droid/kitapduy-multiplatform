package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.ui.components.PremiumUpgradeCard
import com.semtinazizi.kitapduy.ui.screens.SubscriptionPaywallScreen
import com.semtinazizi.kitapduy.ui.viewmodel.AuthViewModel
import com.semtinazizi.kitapduy.platform.BannerAdView
import org.koin.compose.viewmodel.koinViewModel

class MindLibraryScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val authViewModel = koinViewModel<AuthViewModel>()
        val userProfile by authViewModel.userProfile.collectAsState()
        val isPremium = userProfile?.hasPremium() ?: false

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Zihin Kütüphanesi") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Burada rastgele kullanıcı baloncukları / kütüphane ambiyansı olacak.
                // Şimdilik ortada bir yazı ve buton koyuyoruz.
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    PremiumUpgradeCard(
                        modifier = Modifier.padding(bottom = 24.dp),
                        isPremium = isPremium,
                        onClick = { navigator.push(SubscriptionPaywallScreen()) }
                    )
                    Text(
                        text = "Şu an 142 kişi seninle birlikte okuyor / çalışıyor.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Sahte avatar baloncukları
                    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                        listOf("A", "K", "M", "E", "Z").forEach { letter ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = letter, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Button(
                        onClick = { navigator.push(FocusScreen()) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Odak Moduna Başla", fontWeight = FontWeight.Bold)
                    }

                    if (!isPremium) {
                        Spacer(modifier = Modifier.height(16.dp))
                        BannerAdView()
                    }
                }
            }
        }
    }
}
