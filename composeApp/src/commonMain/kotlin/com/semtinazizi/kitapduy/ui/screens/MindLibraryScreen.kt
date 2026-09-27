package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.ui.components.SmartImage
import com.semtinazizi.kitapduy.ui.components.PremiumUpgradeCard
import com.semtinazizi.kitapduy.ui.viewmodel.HomeViewModel
import com.semtinazizi.kitapduy.ui.viewmodel.AuthViewModel
import org.koin.compose.viewmodel.koinViewModel

class MindLibraryScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = koinViewModel<HomeViewModel>()
        val auth = koinViewModel<AuthViewModel>()
        val profile by auth.userProfile.collectAsState()
        val articles by model.articles.collectAsState()
        val loading by model.articlesLoading.collectAsState()
        val error by model.articlesError.collectAsState()
        var category by remember { mutableStateOf("Tümü") }
        val scienceSources = listOf("Düşünbil", "Arkeofili", "Sarkaç", "Khosann")
        val literary = articles.filter { it.source !in scienceSources }
        val filtered = when (category) {
            "Tümü" -> literary
            "Bilim" -> articles.filter { it.source in scienceSources }
            "Haberler" -> literary
            else -> literary.filter { it.source == category }
        }
        Scaffold(topBar = {
            CenterAlignedTopAppBar(title = { Text("Zihin Kütüphanesi") },
                navigationIcon = { IconButton(onClick = { navigator.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") } },
                actions = { IconButton(enabled = filtered.isNotEmpty(), onClick = {
                    filtered.randomOrNull()?.let { navigator.push(ArticleDetailScreen(it.id, it.title, it.link)) }
                }) { Icon(Icons.Default.AutoAwesome, "Keşfet") } })
        }) { padding ->
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (category == "Tümü" && literary.isNotEmpty()) item("featured") {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(literary.take(5), key = { it.id }) { article ->
                            Card(Modifier.width(280.dp).clickable { navigator.push(ArticleDetailScreen(article.id, article.title, article.link)) }, shape = RoundedCornerShape(20.dp)) {
                                SmartImage(imageUrl = article.imageUrl, modifier = Modifier.fillMaxWidth().height(160.dp), contentScale = ContentScale.Crop)
                                Text(article.title, Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium, maxLines = 3)
                            }
                        }
                    }
                }
                if (profile?.hasPremium() != true && category == "Tümü") item("premium") {
                    PremiumUpgradeCard(Modifier.padding(horizontal = 16.dp), false) { navigator.push(SubscriptionPaywallScreen()) }
                }
                item("categories") {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("Tümü", "Kayıp Rıhtım", "Edebiyat Haber", "Kitap Haber", "Sanat Kritik", "Haberler", "Bilim")) { name ->
                            FilterChip(selected = category == name, onClick = { category = name }, label = { Text(name) })
                        }
                    }
                }
                if (loading) item("loading") { LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) }
                if (!loading && filtered.isEmpty()) item("empty") {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(error ?: "Henüz makale bulunmadı.")
                        TextButton(onClick = { model.reloadArticles() }) { Text("Tekrar dene") }
                    }
                }
                items(filtered, key = { it.id }) { article ->
                    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { navigator.push(ArticleDetailScreen(article.id, article.title, article.link)) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SmartImage(imageUrl = article.imageUrl, modifier = Modifier.size(88.dp), contentScale = ContentScale.Crop)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(article.title, style = MaterialTheme.typography.titleSmall, maxLines = 3)
                                Text(article.source, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
