package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import com.semtinazizi.kitapduy.ui.components.FeaturedBookCard
import com.semtinazizi.kitapduy.ui.components.SpotifySectionHeader
import com.semtinazizi.kitapduy.ui.viewmodel.AuthViewModel
import com.semtinazizi.kitapduy.ui.viewmodel.HomeViewModel
import org.koin.compose.viewmodel.koinViewModel
class HomeScreen : Screen {
    
    @Composable
    override fun Content() {
        val homeViewModel = koinViewModel<HomeViewModel>()
        val authViewModel = koinViewModel<AuthViewModel>()

        val books by homeViewModel.books.collectAsState()
        val isLoading by homeViewModel.isLoading.collectAsState()
        val userProfile by authViewModel.userProfile.collectAsState()

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Yükleniyor...")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // Hoşgeldin Kısmı
                item {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Merhaba ${userProfile?.name ?: "Ziyaretçi"} 👋",
                            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
                        )
                    }
                }

                // Bento Grid Menü
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        com.semtinazizi.kitapduy.ui.components.BentoGrid(
                            onQuizClick = { /* TODO */ },
                            onPoetryClick = { /* TODO */ },
                            onTvClick = { /* TODO */ },
                            onArticleClick = { /* TODO */ }
                        )
                    }
                }
                
                item { Spacer(modifier = Modifier.height(24.dp)) }

                // Trend Olanlar Bölümü
                item {
                    SpotifySectionHeader(title = "Trend Olanlar", onClick = {})
                    
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(books.take(5)) { index, book ->
                            FeaturedBookCard(
                                book = book,
                                index = index,
                                onClick = { /* TODO: Navigate to Detail */ }
                            )
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}
