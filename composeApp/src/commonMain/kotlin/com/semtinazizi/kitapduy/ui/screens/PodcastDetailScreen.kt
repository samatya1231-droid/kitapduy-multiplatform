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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.semtinazizi.kitapduy.model.PodcastEpisode
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
import com.semtinazizi.kitapduy.ui.theme.BrandGold
import com.semtinazizi.kitapduy.ui.theme.PremiumAccent

data class PodcastDetailScreen(val episode: PodcastEpisode, val podcastTitle: String) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scrollState = rememberScrollState()
        val authViewModel = koinViewModel<AuthViewModel>()
        val userProfile by authViewModel.userProfile.collectAsState()
        val isPremium = userProfile?.hasPremium() == true

        var isDescriptionExpanded by remember { mutableStateOf(false) }

        val audioController = koinInject<GlobalAudioController>()
        val isPlaying by audioController.isPlaying.collectAsState()
        val currentPosition by audioController.currentPosition.collectAsState()
        val durationMs by audioController.duration.collectAsState()
        val currentEpisode by audioController.currentEpisode.collectAsState()
        
        // Saniye ve Dakika formatlama fonksiyonu
        val formatTime: (Long) -> String = { ms ->
            val totalSeconds = ms / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        }

        val selectedEpisode = currentEpisode?.id == episode.id
        val displayPosition = if (selectedEpisode) currentPosition.coerceIn(0f, 1f) else 0f
        val displayDuration = if (selectedEpisode) durationMs else 0f
        val currentTimeMs = (displayPosition * displayDuration).toLong()
        
        var showAtmosphereDialog by remember { mutableStateOf(false) }
        var showSpeedDialog by remember { mutableStateOf(false) }
        var showTimerDialog by remember { mutableStateOf(false) }
        
        val currentSpeed by audioController.playbackSpeed.collectAsState()
        var currentAtmosphere by remember { mutableStateOf<String?>(null) }
        var atmosphereVolume by remember { mutableStateOf(0.5f) }
        val timerRemaining by audioController.sleepTimerRemainingMinutes.collectAsState()
        val isDownloading by audioController.isDownloading.collectAsState()

        val displayImageUrl = episode.imageUrl ?: "https://images.unsplash.com/photo-1593697821252-0c9137d9fc45?w=400&q=80"

        // Tarih ve Süre Formatlama Yardımcıları
        val formattedDate = remember(episode.pubDate) {
            val regex = "(\\d{1,2})\\s+([A-Za-z]{3})\\s+(\\d{4})".toRegex()
            val match = regex.find(episode.pubDate)
            if (match != null) {
                val day = match.groupValues[1]
                val month = match.groupValues[2]
                val year = match.groupValues[3]
                val turkishMonths = mapOf(
                    "Jan" to "Ocak", "Feb" to "Şubat", "Mar" to "Mart", "Apr" to "Nisan",
                    "May" to "Mayıs", "Jun" to "Haziran", "Jul" to "Temmuz", "Aug" to "Ağustos",
                    "Sep" to "Eylül", "Oct" to "Ekim", "Nov" to "Kasım", "Dec" to "Aralık"
                )
                "$day ${turkishMonths[month] ?: month} $year"
            } else episode.pubDate.take(16).ifBlank { "Tarih Yok" }
        }

        val formattedDuration = remember(episode.duration) {
            if (episode.duration.contains(":")) episode.duration
            else {
                val seconds = episode.duration.toLongOrNull()
                if (seconds != null) {
                    val mins = seconds / 60
                    if (mins > 0) "$mins dk" else "$seconds sn"
                } else episode.duration.ifBlank { "--:--" }
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Ambient Glow (Arkaplan Aurası)
            AsyncImage(
                model = displayImageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.2f)
                    .blur(120.dp)
                    .alpha(0.15f),
                contentScale = ContentScale.Crop
            )

            // Collapsing Parallax Header
            val parallaxOffset = -(scrollState.value.toFloat() * 0.4f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
                    .graphicsLayer { translationY = parallaxOffset }
            ) {
                AsyncImage(
                    model = displayImageUrl,
                    contentDescription = episode.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
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
                Spacer(Modifier.height(380.dp))

                Surface(
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxWidth().padding(top = 36.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 120.dp)) {
                        Spacer(Modifier.height(24.dp))
                        
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

                        Spacer(Modifier.height(24.dp))

                        // Player Kontrolleri
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                            // Slider
                            Slider(
                                value = displayPosition,
                                enabled = selectedEpisode && displayDuration > 0f,
                                onValueChange = { audioController.seekTo(it, expectedEpisodeId = episode.id) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = SleekChampagne,
                                    activeTrackColor = SleekChampagne,
                                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                )
                            )
                            
                            // Saniye Göstergesi (Yeni!)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatTime(currentTimeMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SleekGray
                                )
                                Text(
                                    text = formatTime(displayDuration.toLong()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SleekGray
                                )
                            }
                            
                            Spacer(Modifier.height(8.dp))

                            // Butonlar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(enabled = selectedEpisode && displayDuration > 0f, onClick = { audioController.skipBy(-15_000, expectedEpisodeId = episode.id) }) {
                                    Icon(Icons.Default.FastRewind, contentDescription = "Geri", tint = Color.White)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                FloatingActionButton(
                                    onClick = { 
                                        if (currentEpisode?.id == episode.id) {
                                            audioController.togglePlayPause()
                                        } else {
                                            audioController.playPodcastEpisode(episode)
                                        }
                                    },
                                    containerColor = SleekChampagne,
                                    shape = CircleShape,
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying && currentEpisode?.id == episode.id) Icons.Default.Pause else Icons.Default.PlayArrow, 
                                        contentDescription = "Oynat/Duraklat", 
                                        tint = Color.Black,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                IconButton(enabled = selectedEpisode && displayDuration > 0f, onClick = { audioController.skipBy(15_000, expectedEpisodeId = episode.id) }) {
                                    Icon(Icons.Default.FastForward, contentDescription = "İleri", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }

                        Spacer(Modifier.height(32.dp))

                        // Yeni Action Bar (Oynatıcı Özel İkonları)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showSpeedDialog = true }) {
                                Icon(Icons.Default.Speed, null, tint = MaterialTheme.colorScheme.onSurface)
                                Text("${currentSpeed}x", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showAtmosphereDialog = true }) {
                                Icon(Icons.Default.Air, null, tint = MaterialTheme.colorScheme.onSurface)
                                Text("Atmosfer", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showTimerDialog = true }) {
                                Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.onSurface)
                                Text("Uyku", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                                if (!isPremium) {
                                    navigator.push(SubscriptionPaywallScreen())
                                }
                            }) {
                                Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.onSurface)
                                Text("İndir", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        
                        Spacer(Modifier.height(32.dp))
                        
                        if (!isPremium) {
                            PremiumTrialBanner(
                                onClick = { navigator.push(SubscriptionPaywallScreen()) },
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            Spacer(Modifier.height(24.dp))
                        }

                        // Açıklama Bölümü
                        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                            Text("Bölüm Hakkında", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = episode.description.ifEmpty { "Bu bölüm hakkında henüz bir açıklama eklenmemiş." },
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
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navigator.pop() },
                    modifier = Modifier.background(Color.Black.copy(0.4f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            if (showAtmosphereDialog) {
                AtmosphereDialog(
                    onDismiss = { showAtmosphereDialog = false },
                    currentSound = currentAtmosphere,
                    onSoundSelected = { 
                        currentAtmosphere = it
                        if (it != null) audioController.playAtmosphere(it, atmosphereVolume)
                        else audioController.stopAtmosphere()
                    },
                    volume = atmosphereVolume,
                    onVolumeChange = { 
                        atmosphereVolume = it
                        audioController.setAtmosphereVolume(it)
                    },
                    isPremiumUser = isPremium,
                    onUpgradeClick = { navigator.push(SubscriptionPaywallScreen()) }
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
                    onTimerSelected = { audioController.startSleepTimer(it) },
                    onCancelTimer = { audioController.cancelSleepTimer() }
                )
            }
        }
    }
}
