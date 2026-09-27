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

        val home = koinViewModel<com.semtinazizi.kitapduy.ui.viewmodel.HomeViewModel>()
        val articles by home.articles.collectAsState()
        val article = articles.find { it.id == articleId }
        var openError by remember { mutableStateOf(false) }

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
                modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                article?.let {
                    com.semtinazizi.kitapduy.ui.components.SmartImage(imageUrl = it.imageUrl,
                        modifier = Modifier.fillMaxWidth().height(200.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(it.source, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Text(it.description)
                }
                Spacer(Modifier.height(24.dp))
                val source = articleUrl.ifBlank { article?.link.orEmpty() }
                if (source.startsWith("https://") || source.startsWith("http://")) {
                    Button(onClick = {
                        try { uriHandler.openUri(source) } catch (failure: Exception) { openError = true }
                    }) { Text("Yazının tamamını kaynakta oku") }
                } else Text("Bu makalenin kaynak bağlantısı bulunamadı.")
                if (openError) Text("Kaynak açılamadı. Tekrar deneyin.", color = MaterialTheme.colorScheme.error)

                if (!isPremium) {
                    Spacer(modifier = Modifier.height(24.dp))
                    BannerAdView()
                }
            }
        }
    }
}
