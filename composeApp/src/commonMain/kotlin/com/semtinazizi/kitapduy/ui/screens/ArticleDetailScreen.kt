package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.platform.BannerAdView
import com.semtinazizi.kitapduy.ui.viewmodel.AuthViewModel
import org.koin.compose.viewmodel.koinViewModel

data class ArticleDetailScreen(val articleId: String, val title: String, val articleUrl: String = "") : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val uriHandler = LocalUriHandler.current
        val authViewModel = koinViewModel<AuthViewModel>()
        val userProfile by authViewModel.userProfile.collectAsState()
        val isPremium = userProfile?.hasPremium() ?: false

        // Makale URL'si varsa hemen dis tarayicide ac, icerik placeholder gosterme
        LaunchedEffect(articleUrl) {
            if (articleUrl.isNotBlank()) {
                uriHandler.openUri(articleUrl)
                navigator.pop()
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Makale") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        ) { paddingValues ->
            // URL yoksa basit bir acilma ekrani goster (asil icerik dista acilir)
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (articleUrl.isNotBlank()) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Makale aciliyor...", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Text("Bu makale icin icerk mevcut degil.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { navigator.pop() }) { Text("Geri Don") }
                }

                if (!isPremium) {
                    Spacer(modifier = Modifier.height(24.dp))
                    BannerAdView()
                }
            }
        }
    }
}
