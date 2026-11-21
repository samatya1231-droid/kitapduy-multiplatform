package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.util.PlaybackTimeFormatter
import com.semtinazizi.kitapduy.util.RssTextCleaner
import com.semtinazizi.kitapduy.util.root
import com.semtinazizi.kitapduy.model.PodcastEpisode
import com.semtinazizi.kitapduy.ui.components.SmartImage
import com.semtinazizi.kitapduy.ui.theme.BrandGold
import com.semtinazizi.kitapduy.ui.theme.PremiumAccent
import com.semtinazizi.kitapduy.ui.viewmodel.AuthViewModel
import com.semtinazizi.kitapduy.ui.viewmodel.GlobalAudioController
import com.semtinazizi.kitapduy.ui.components.dialogs.*
import com.semtinazizi.kitapduy.ui.components.PremiumTrialBanner
import com.semtinazizi.kitapduy.platform.BannerAdView
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject

import com.semtinazizi.kitapduy.ui.theme.SleekChampagne
import com.semtinazizi.kitapduy.ui.theme.SleekDarkBackground
import com.semtinazizi.kitapduy.ui.theme.SleekGray

data class PodcastDetailScreen(val episode: PodcastEpisode, val podcastTitle: String) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scrollState = rememberScrollState()
        val authViewModel = koinViewModel<AuthViewModel>()
        val billingManager = koinInject<com.semtinazizi.kitapduy.platform.BillingManager>()
        val userProfile by authViewModel.userProfile.collectAsState()
        val isPremium = billingManager.isPremium.collectAsState().value || userProfile?.hasPremium() == true

        var isDescriptionExpanded by remember { mutableStateOf(false) }

        val audioController = koinInject<GlobalAudioController>()
        val fullPlayerToken = remember { Any() }
        DisposableEffect(fullPlayerToken) {
            audioController.setFullPlayerVisible(true, fullPlayerToken)
            onDispose { audioController.setFullPlayerVisible(false, fullPlayerToken) }
        }
        var showAtmosphereDialog by remember { mutableStateOf(false) }
        var showSpeedDialog by remember { mutableStateOf(false) }
        var showTimerDialog by remember { mutableStateOf(false) }
        var downloadMessage by remember { mutableStateOf<String?>(null) }
        downloadMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { downloadMessage = null },
                text = { Text(message) },
                confirmButton = { TextButton(onClick = { downloadMessage = null }) { Text("Tamam") } }
            )
        }
        
        val currentSpeed by audioController.playbackSpeed.collectAsState()
        val currentAtmosphere by audioController.currentAtmosphereId.collectAsState()
        val atmosphereVolume by audioController.atmosphereVolume.collectAsState()
        val timerRemaining by audioController.sleepTimerRemainingMinutes.collectAsState()
        val timerRemainingSeconds by audioController.sleepTimerRemainingSeconds.collectAsState()
        val timerAtBookEnd by audioController.sleepTimerAtBookEnd.collectAsState()
        val isDownloading by audioController.isDownloading.collectAsState()

        val displayImageUrl = episode.imageUrl?.takeIf { it.isNotBlank() }
            ?: "https://images.unsplash.com/photo-1593697821252-0c9137d9fc45?w=400&q=80"

        // Tarih ve Süre Formatlama Yardımcıları
        val currentEpisode by audioController.currentEpisode.collectAsState()
        val liveDurationMs by audioController.duration.collectAsState()
        val formattedDate = remember(episode.pubDate) {
            PlaybackTimeFormatter.formatPubDateTr(episode.pubDate, fallback = "Tarih Yok")
        }
        val rssDurationMs = remember(episode.duration) {
            PlaybackTimeFormatter.parseRssDurationToMs(episode.duration)
        }
        val resolvedTotalDurationMs = if (currentEpisode?.id == episode.id && liveDurationMs > 0f) {
            liveDurationMs.toLong()
        } else {
            rssDurationMs
        }
        val formattedDuration = if (resolvedTotalDurationMs > 0L) {
            PlaybackTimeFormatter.formatDurationMs(resolvedTotalDurationMs)
        } else {
            PlaybackTimeFormatter.formatRssDuration(episode.duration)
        }
        val cleanDescription = remember(episode.description) {
            RssTextCleaner.cleanHtmlDescription(episode.description)
        }

        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Ambient Glow (Arkaplan Aurası)
            SmartImage(
                imageUrl = displayImageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.2f)
                    .blur(120.dp)
                    .alpha(0.15f),
                contentScale = ContentScale.Crop,
                fallbackText = episode.title
            )

            // Collapsing Parallax Header
            val parallaxOffset = -(scrollState.value.toFloat() * 0.4f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .graphicsLayer { translationY = parallaxOffset }
            ) {
                SmartImage(
                    imageUrl = displayImageUrl,
                    contentDescription = episode.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    fallbackText = episode.title
                )
                // Vignette Karartma
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.2f),
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )
            }

            Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                Spacer(Modifier.height(310.dp))

                Surface(
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 120.dp)) {
                        Spacer(Modifier.height(16.dp))
                        
                        // Başlık
                        Text(
                            text = episode.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                        )
                        Spacer(Modifier.height(16.dp))

                        // Bento Box İstatistikler
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Podcast Adı
                            Surface(
                                modifier = Modifier.weight(1f).height(72.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            ) {
                                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Mic, null, tint = SleekChampagne, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.height(4.dp))
                                    Text(text = podcastTitle.ifBlank { "Podcast" }, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            // Yayın Tarihi
                            Surface(
                                modifier = Modifier.weight(1f).height(72.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            ) {
                                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.DateRange, null, tint = Color(0xFFFF2D55), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.height(4.dp))
                                    Text(text = formattedDate, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            // Süre (RSS'den gelen metin)
                            Surface(
                                modifier = Modifier.weight(1f).height(72.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            ) {
                                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Timer, null, tint = SleekChampagne, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.height(4.dp))
                                    Text(text = formattedDuration, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        PodcastPlaybackControls(audioController, episode, podcastTitle)

                        Spacer(Modifier.height(20.dp))

                        // Yeni Action Bar (Oynatıcı Özel İkonları)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showSpeedDialog = true }
                                    .padding(horizontal = 6.dp, vertical = 6.dp)
                                    .semantics(mergeDescendants = true) {}
                            ) {
                                Icon(Icons.Default.Speed, null, tint = MaterialTheme.colorScheme.onSurface)
                                Text("${currentSpeed}x", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showAtmosphereDialog = true }
                                    .padding(horizontal = 6.dp, vertical = 6.dp)
                                    .semantics(mergeDescendants = true) {}
                            ) {
                                Icon(
                                    Icons.Default.Air,
                                    null,
                                    tint = if (currentAtmosphere != null) BrandGold else MaterialTheme.colorScheme.onSurface
                                )
                                Text("Atmosfer", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showTimerDialog = true }
                                    .padding(horizontal = 6.dp, vertical = 6.dp)
                                    .semantics(mergeDescendants = true) {}
                            ) {
                                Icon(
                                    Icons.Default.Timer,
                                    null,
                                    tint = if (timerRemainingSeconds > 0 || timerAtBookEnd) BrandGold else MaterialTheme.colorScheme.onSurface
                                )
                                Text("Uyku", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (isPremium) {
                                            if (!audioController.isDownloaded(episode.id)) {
                                                audioController.downloadPodcastEpisode(episode, podcastTitle) { success ->
                                                    downloadMessage = if (success) "Bölüm indirildi." else "Bölüm indirilemedi. Lütfen tekrar deneyin."
                                                }
                                            } else {
                                                downloadMessage = "Bu bölüm zaten indirildi."
                                            }
                                        } else {
                                            navigator.root.push(SubscriptionPaywallScreen())
                                        }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 6.dp)
                                    .semantics(mergeDescendants = true) {}
                            ) {
                                val downloaded = audioController.isDownloaded(episode.id)
                                Icon(
                                    if (downloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Text(if (isDownloading) "İndiriliyor" else "İndir", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        
                        Spacer(Modifier.height(32.dp))
                        
                        if (!isPremium) {
                            PremiumTrialBanner(
                                onClick = { navigator.root.push(SubscriptionPaywallScreen()) },
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            Spacer(Modifier.height(24.dp))
                        }

                        // Açıklama Bölümü
                        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                            Text("Bölüm Hakkında", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = cleanDescription.ifEmpty { "Bu bölüm hakkında henüz bir açıklama eklenmemiş." },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                lineHeight = 24.sp,
                                maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 6,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable { isDescriptionExpanded = !isDescriptionExpanded }
                            )
                        }
                        
                        Spacer(Modifier.height(40.dp))
                    }
                }
            }

            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navigator.pop() },
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .background(Color.Black.copy(0.45f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = Color.White)
                }
            }

            if (showAtmosphereDialog) {
                AtmosphereDialog(
                    onDismiss = { showAtmosphereDialog = false },
                    currentSound = currentAtmosphere,
                    onSoundSelected = { soundId ->
                        val fallback = atmosphereSounds.firstOrNull { it.id == soundId }?.fallbackUrl
                        audioController.selectAtmosphere(soundId, fallback)
                    },
                    volume = atmosphereVolume,
                    onVolumeChange = { audioController.setAtmosphereVolume(it) },
                    isPremiumUser = isPremium,
                    onUpgradeClick = {
                        if (navigator.root.lastItem !is SubscriptionPaywallScreen) {
                            navigator.root.push(SubscriptionPaywallScreen())
                        }
                    }
                )
            }
            if (showSpeedDialog) {
                PlaybackSpeedDialog(
                    onDismiss = { showSpeedDialog = false },
                    currentSpeed = currentSpeed,
                    onSpeedSelected = { audioController.setPlaybackSpeed(it) }
                )
            }
            if (showTimerDialog) {
                SleepTimerDialog(
                    onDismiss = { showTimerDialog = false },
                    remainingMinutes = timerRemaining,
                    remainingSeconds = timerRemainingSeconds,
                    isAtBookEnd = timerAtBookEnd,
                    isPremiumUser = isPremium,
                    onTimerSelected = { audioController.startSleepTimer(it) },
                    onStopAtBookEnd = { audioController.startSleepTimerAtBookEnd() },
                    onCancelTimer = { audioController.cancelSleepTimer() },
                    onUpgradeClick = {
                        if (navigator.root.lastItem !is SubscriptionPaywallScreen) {
                            navigator.root.push(SubscriptionPaywallScreen())
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PodcastPlaybackControls(
    audioController: GlobalAudioController,
    episode: PodcastEpisode,
    podcastTitle: String
) {
    val isPlaying by audioController.isPlaying.collectAsState()
    val isBuffering by audioController.isBuffering.collectAsState()
    val playbackError by audioController.playbackError.collectAsState()
    val currentPosition by audioController.currentPosition.collectAsState()
    val currentPositionMs by audioController.currentPositionMs.collectAsState()
    val durationMs by audioController.duration.collectAsState()
    val currentEpisode by audioController.currentEpisode.collectAsState()

    val selectedEpisode = currentEpisode?.id == episode.id
    val savedPositionMs = remember(episode.id, selectedEpisode) {
        audioController.savedPositionMsFor(episode.id)
    }
    val rssDurationMs = remember(episode.duration) {
        PlaybackTimeFormatter.parseRssDurationToMs(episode.duration)
    }
    val loadedDurationMs = if (selectedEpisode && durationMs > 0f) durationMs.toLong() else 0L
    val effectiveTotalMs = if (loadedDurationMs > 0L) loadedDurationMs else rssDurationMs
    val currentTimeMs = if (selectedEpisode && (loadedDurationMs > 0L || currentPositionMs > 0L)) {
        currentPositionMs.coerceAtLeast(0L)
    } else {
        savedPositionMs
    }
    val displayPosition = when {
        selectedEpisode && loadedDurationMs > 0L -> currentPosition.coerceIn(0f, 1f)
        effectiveTotalMs > 0L && currentTimeMs > 0L ->
            (currentTimeMs.toDouble() / effectiveTotalMs.toDouble()).toFloat().coerceIn(0f, 1f)
        else -> 0f
    }
    val showBuffering = selectedEpisode && isBuffering
    val activeError = if (selectedEpisode) playbackError else null
    val canSeek = selectedEpisode && loadedDurationMs > 0L

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        if (showBuffering) {
            Text(
                text = "Bölüm yükleniyor...",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp).testTag("podcast-buffering")
            )
        }

        // Slider
        Slider(
            value = displayPosition,
            enabled = canSeek,
            onValueChange = { audioController.seekTo(it, expectedEpisodeId = episode.id) },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Oynatma konumu" },
            colors = SliderDefaults.colors(
                thumbColor = SleekChampagne,
                activeTrackColor = SleekChampagne,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
        )

        // Saniye Göstergesi
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = PlaybackTimeFormatter.formatDurationMs(currentTimeMs),
                modifier = Modifier.testTag("podcast-elapsed"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = PlaybackTimeFormatter.formatDurationMs(effectiveTotalMs, placeholderWhenZero = true),
                modifier = Modifier.testTag("podcast-duration"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!activeError.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth().testTag("podcast-playback-error")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = activeError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    TextButton(
                        onClick = {
                            audioController.clearPlaybackError()
                            audioController.playPodcastEpisode(episode, podcastTitle)
                        }
                    ) {
                        Text("Tekrar Dene", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Butonlar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                enabled = canSeek,
                onClick = { audioController.skipBy(-15_000, expectedEpisodeId = episode.id) }
            ) {
                Icon(Icons.Default.FastRewind, contentDescription = "15 saniye geri", tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.width(16.dp))
            FloatingActionButton(
                onClick = {
                    if (currentEpisode?.id == episode.id) {
                        audioController.togglePlayPause()
                    } else {
                        audioController.playPodcastEpisode(episode, podcastTitle)
                    }
                },
                containerColor = SleekChampagne,
                shape = CircleShape,
                modifier = Modifier
                    .size(64.dp)
                    .testTag("podcast-play-pause")
                    .semantics {
                        contentDescription = if (showBuffering) "Yükleniyor" else "Oynat/Duraklat"
                    }
            ) {
                if (showBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = Color.Black,
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isPlaying && selectedEpisode) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                enabled = canSeek,
                onClick = { audioController.skipBy(15_000, expectedEpisodeId = episode.id) }
            ) {
                Icon(Icons.Default.FastForward, contentDescription = "15 saniye ileri", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
