package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.platform.BannerAdView
import com.semtinazizi.kitapduy.ui.components.SmartImage
import com.semtinazizi.kitapduy.ui.theme.PlayfairDisplayFamily
import com.semtinazizi.kitapduy.ui.theme.SleekChampagne
import com.semtinazizi.kitapduy.ui.viewmodel.AuthViewModel
import com.semtinazizi.kitapduy.util.PlaybackTimeFormatter
import com.semtinazizi.kitapduy.util.RssTextCleaner
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

        LaunchedEffect(rssUrl, podcastId) {
            viewModel.loadEpisodes(rssUrl, podcastId)
        }

        val headerImageUrl = imageUrl.ifBlank { channelImageUrl.orEmpty() }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = PlayfairDisplayFamily,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
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
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (error != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Headphones,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = error ?: "Bölümler yüklenirken bir hata oluştu.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.loadEpisodes(rssUrl, podcastId, forceRefresh = true) },
                            colors = ButtonDefaults.buttonColors(containerColor = SleekChampagne),
                            modifier = Modifier.height(48.dp).testTag("podcast-episodes-retry")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Yeniden Dene", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(key = "podcast-header") {
                            PodcastHeader(
                                title = title,
                                imageUrl = headerImageUrl,
                                episodeCount = episodes.size
                            )
                        }

                        if (episodes.isEmpty()) {
                            item(key = "episodes-empty") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp, horizontal = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Bölümler yüklenemedi veya bu podcast henüz bölüm içermiyor.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = { viewModel.loadEpisodes(rssUrl, podcastId, forceRefresh = true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SleekChampagne),
                                        modifier = Modifier.height(48.dp).testTag("podcast-episodes-retry")
                                    ) {
                                        Text("Yeniden Dene", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            item(key = "episodes-heading") {
                                Text(
                                    text = "Bölümler",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = PlayfairDisplayFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                                )
                            }
                        }

                        items(episodes.size) { index ->
                            val episode = episodes[index]
                            val isThisPlaying = currentEpisode?.id == episode.id
                            val cleanDesc = RssTextCleaner.cleanHtmlDescription(episode.description)
                                .replace('\n', ' ')
                                .trim()

                            Card(
                                modifier = Modifier
                                    .testTag("podcast-episode-$index")
                                    .fillMaxWidth()
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
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isThisPlaying) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isThisPlaying) SleekChampagne.copy(alpha = 0.65f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = episode.title,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (cleanDesc.isNotBlank()) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = cleanDesc,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        val formattedDuration = PlaybackTimeFormatter.formatRssDuration(
                                            episode.duration,
                                            emptyPlaceholder = ""
                                        )
                                        val formattedDate = PlaybackTimeFormatter.formatPubDateTr(
                                            episode.pubDate,
                                            fallback = ""
                                        )
                                        val meta = listOfNotNull(
                                            formattedDuration.takeIf { it.isNotBlank() },
                                            formattedDate.takeIf { it.isNotBlank() }
                                        ).joinToString(" • ")
                                        if (meta.isNotBlank()) {
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                text = meta,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Surface(
                                        modifier = Modifier.size(40.dp),
                                        shape = CircleShape,
                                        color = if (isThisPlaying) SleekChampagne else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isThisPlaying && isPlaying) {
                                                    Icons.Filled.Pause
                                                } else {
                                                    Icons.Filled.PlayArrow
                                                },
                                                contentDescription = "Oynat",
                                                tint = if (isThisPlaying) Color.Black else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (!isPremium) {
                            item {
                                BannerAdView(modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PodcastHeader(
    title: String,
    imageUrl: String,
    episodeCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl.isNotBlank()) {
                    SmartImage(
                        imageUrl = imageUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        fallbackText = title
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Headphones,
                        contentDescription = null,
                        tint = SleekChampagne,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = PlayfairDisplayFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = SleekChampagne.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "$episodeCount Bölüm • Sesli Yayın",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
