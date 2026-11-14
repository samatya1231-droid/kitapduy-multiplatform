package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.platform.BannerAdView
import com.semtinazizi.kitapduy.ui.viewmodel.AuthViewModel
import org.koin.compose.viewmodel.koinViewModel

data class PodcastEpisodesScreen(
    val podcastId: String,
    val title: String,
    val rssUrl: String,
    val imageUrl: String = ""
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val authViewModel = koinViewModel<AuthViewModel>()
        val userProfile by authViewModel.userProfile.collectAsState()
        val isPremium = userProfile?.hasPremium() ?: false
        
        val viewModel = koinViewModel<com.semtinazizi.kitapduy.ui.viewmodel.PodcastEpisodesViewModel>()
        val audioController = org.koin.compose.koinInject<com.semtinazizi.kitapduy.ui.viewmodel.GlobalAudioController>()
        
        val episodes by viewModel.episodes.collectAsState()
        val channelImageUrl by viewModel.channelImageUrl.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val error by viewModel.error.collectAsState()

        val currentEpisode by audioController.currentEpisode.collectAsState()
        val isPlaying by audioController.isPlaying.collectAsState()

        LaunchedEffect(rssUrl) {
            viewModel.loadEpisodes(rssUrl)
        }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title, maxLines = 1) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
                } else if (error != null) {
                    Text(
                        text = error ?: "Bir hata oluştu.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(androidx.compose.ui.Alignment.Center).padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        item {
                            Text(
                                text = "Bölümler",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                        
                        items(episodes.size) { index ->
                            val episode = episodes[index]
                            val isThisPlaying = currentEpisode?.id == episode.id
                            
                            Card(
                                modifier = Modifier.testTag("podcast-episode-$index")
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable {
                                        val resolvedImage = episode.imageUrl?.takeIf { it.isNotBlank() }
                                            ?: imageUrl.takeIf { it.isNotBlank() }
                                            ?: channelImageUrl?.takeIf { it.isNotBlank() }
                                        val resolvedEpisode = if (resolvedImage != episode.imageUrl) {
                                            episode.copy(imageUrl = resolvedImage)
                                        } else {
                                            episode
                                        }
                                        if (!isThisPlaying) {
                                            audioController.playPodcastEpisode(resolvedEpisode, title)
                                        }
                                        navigator.push(PodcastDetailScreen(resolvedEpisode, title))
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isThisPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = episode.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (episode.pubDate.isNotBlank()) {
                                            Text(
                                                text = episode.pubDate,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                    
                                    Icon(
                                        imageVector = if (isThisPlaying && isPlaying) 
                                            Icons.Filled.Pause 
                                        else 
                                            Icons.Filled.PlayArrow,
                                        contentDescription = "Oynat",
                                        tint = if (isThisPlaying) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp).padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                        
                        if (!isPremium) {
                            item {
                                BannerAdView(modifier = Modifier.padding(top = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
