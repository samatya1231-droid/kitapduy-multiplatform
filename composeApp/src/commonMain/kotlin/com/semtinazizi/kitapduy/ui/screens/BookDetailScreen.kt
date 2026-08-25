package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
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
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.semtinazizi.kitapduy.model.Book
import com.semtinazizi.kitapduy.model.Comment
import com.semtinazizi.kitapduy.ui.components.CommentItem
import com.semtinazizi.kitapduy.ui.components.PremiumTrialBanner
import com.semtinazizi.kitapduy.ui.components.SmartImage
import com.semtinazizi.kitapduy.ui.theme.BrandGold
import com.semtinazizi.kitapduy.ui.theme.PremiumAccent
import com.semtinazizi.kitapduy.ui.viewmodel.AuthViewModel
import com.semtinazizi.kitapduy.ui.viewmodel.BookDetailScreenModel
import com.semtinazizi.kitapduy.ui.viewmodel.GlobalAudioController
import com.semtinazizi.kitapduy.ui.components.dialogs.*
import com.semtinazizi.kitapduy.platform.BannerAdView
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject

data class BookDetailScreen(val book: Book) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scrollState = rememberScrollState()
        val authViewModel = koinViewModel<AuthViewModel>()
        val userProfile by authViewModel.userProfile.collectAsState()
        val isPremium = userProfile?.hasPremium() == true
        val userId = userProfile?.uid ?: ""
        val userName = userProfile?.name ?: "Kullanıcı"

        val viewModel = koinScreenModel<BookDetailScreenModel>()
        
        val viewCount by viewModel.viewCount.collectAsState()
        val likeCount by viewModel.likeCount.collectAsState()
        val isLiked by viewModel.isLiked.collectAsState()
        val activeListeners by viewModel.activeListeners.collectAsState()
        val comments by viewModel.comments.collectAsState()

        var commentText by remember { mutableStateOf("") }
        var isDescriptionExpanded by remember { mutableStateOf(false) }

        val audioController = koinInject<GlobalAudioController>()
        val isPlaying by audioController.isPlaying.collectAsState()
        val currentPosition by audioController.currentPosition.collectAsState()
        val currentBook by audioController.currentBook.collectAsState()
        
        var showAtmosphereDialog by remember { mutableStateOf(false) }
        var showSpeedDialog by remember { mutableStateOf(false) }
        var showTimerDialog by remember { mutableStateOf(false) }
        var showQuoteDialog by remember { mutableStateOf(false) }
        
        val currentSpeed by audioController.playbackSpeed.collectAsState()
        var currentAtmosphere by remember { mutableStateOf<String?>(null) }
        var atmosphereVolume by remember { mutableStateOf(0.5f) }
        val timerRemaining by audioController.sleepTimerRemainingMinutes.collectAsState()
        val isDownloading by audioController.isDownloading.collectAsState()

        LaunchedEffect(book.id) {
            viewModel.loadBookData(book.id, userId)
            viewModel.incrementViewCount(book.id)
        }

        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Ambient Glow (Arkaplan Aurası)
            AsyncImage(
                model = book.coverUrl,
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
                    model = book.coverUrl,
                    contentDescription = book.title,
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
                            text = book.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                        )
                        Spacer(Modifier.height(16.dp))

                        // Bento Box İstatistikler
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Yazar
                            Surface(
                                modifier = Modifier.weight(1f).height(72.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            ) {
                                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Person, null, tint = BrandGold, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.height(4.dp))
                                    Text(text = book.author, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            // Kategori
                            Surface(
                                modifier = Modifier.weight(1f).height(72.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            ) {
                                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Category, null, tint = PremiumAccent, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.height(4.dp))
                                    Text(text = book.category.ifBlank { "Roman" }, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            // Dinleyici/Görüntüleme
                            Surface(
                                modifier = Modifier.weight(1f).height(72.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            ) {
                                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                    val infiniteTransition = rememberInfiniteTransition()
                                    val pulseAlpha by infiniteTransition.animateFloat(
                                        initialValue = 0.2f, targetValue = 1f,
                                        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Red.copy(alpha = pulseAlpha)))
                                        Spacer(Modifier.width(4.dp))
                                        Icon(Icons.Default.Headset, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(text = "$activeListeners", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Premium Banner
                        if (!isPremium) {
                            PremiumTrialBanner(
                                onClick = { navigator.push(SubscriptionPaywallScreen()) },
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            BannerAdView(modifier = Modifier.padding(horizontal = 24.dp))
                            Spacer(Modifier.height(24.dp))
                        }

                        // Player Kontrolleri
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                            // Slider
                            Slider(
                                value = currentPosition,
                                onValueChange = { audioController.seekTo(it) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = BrandGold,
                                    activeTrackColor = BrandGold,
                                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                )
                            )
                            // Butonlar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { audioController.seekTo((currentPosition - 0.05f).coerceAtLeast(0f)) }) {
                                    Icon(Icons.Default.FastRewind, contentDescription = "Geri")
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                FloatingActionButton(
                                    onClick = { 
                                        if (currentBook?.id == book.id) {
                                            audioController.togglePlayPause()
                                        } else {
                                            audioController.playTrack(book, isPremium)
                                        }
                                    },
                                    containerColor = BrandGold,
                                    shape = CircleShape,
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying && currentBook?.id == book.id) Icons.Default.Pause else Icons.Default.PlayArrow, 
                                        contentDescription = "Oynat/Duraklat", 
                                        tint = Color.Black,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                IconButton(onClick = { audioController.seekTo((currentPosition + 0.05f).coerceAtMost(1f)) }) {
                                    Icon(Icons.Default.FastForward, contentDescription = "İleri")
                                }
                            }
                        }

                        Spacer(Modifier.height(32.dp))

                        // Yeni Action Bar (Oynatıcı Özel İkonları)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { viewModel.toggleLike(book.id, userId) }) {
                                Icon(if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurface)
                                Text("$likeCount", fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showSpeedDialog = true }) {
                                Icon(Icons.Default.Speed, null, tint = MaterialTheme.colorScheme.onSurface)
                                Text("${currentSpeed}x", fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showAtmosphereDialog = true }) {
                                Icon(Icons.Default.Air, null, tint = MaterialTheme.colorScheme.onSurface)
                                Text("Atmosfer", fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showTimerDialog = true }) {
                                Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.onSurface)
                                Text("Uyku", fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                                showQuoteDialog = true
                            }) {
                                Icon(Icons.Default.FormatQuote, null, tint = MaterialTheme.colorScheme.onSurface)
                                Text("Alıntı", fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                                if (isPremium) {
                                    if (!audioController.isDownloaded(book.id)) {
                                        audioController.downloadCurrentBook {}
                                    }
                                } else {
                                    navigator.push(SubscriptionPaywallScreen())
                                }
                            }) {
                                Icon(if (audioController.isDownloaded(book.id)) Icons.Default.DownloadDone else Icons.Default.Download, null, tint = MaterialTheme.colorScheme.onSurface)
                                Text(if (isDownloading) "İndiriliyor" else "İndir", fontSize = 12.sp)
                            }
                        }

                        Spacer(Modifier.height(32.dp))

                        // Açıklama Bölümü
                        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                            Text("Hakkında", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = book.description.ifEmpty { "Bu kitap hakkında henüz bir açıklama eklenmemiş." },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                lineHeight = 24.sp,
                                maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 4,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable { isDescriptionExpanded = !isDescriptionExpanded }
                            )
                            if (book.description.length > 150) {
                                Text(
                                    text = if (isDescriptionExpanded) "Daha az göster" else "Devamını oku",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable { isDescriptionExpanded = !isDescriptionExpanded }.padding(top = 4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(40.dp))

                        // Yorumlar Bölümü
                        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                            Text(text = "Yorumlar (${comments.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            
                            // Yorum Yazma Alanı
                            OutlinedTextField(
                                value = commentText,
                                onValueChange = { commentText = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Ne düşünüyorsun?") },
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandGold,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(0.1f)
                                ),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        if (commentText.isNotBlank()) {
                                            viewModel.addComment(book.id, book.title, book.coverUrl, commentText, userProfile?.photoUrl, isPremium, userId, userName)
                                            commentText = ""
                                        }
                                    }) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gönder", tint = BrandGold)
                                    }
                                }
                            )
                            Spacer(Modifier.height(24.dp))

                            // Yorum Listesi
                            comments.forEach { comment ->
                                CommentItem(
                                    comment = comment,
                                    currentUserId = userId,
                                    bookId = book.id,
                                    bookTitle = book.title,
                                    isPremium = comment.isPremium,
                                    onEditComment = { _, _ -> },
                                    onDeleteComment = {},
                                    onToggleLike = {},
                                    onAddReply = {},
                                    onLoadReplies = {},
                                    replies = emptyList(),
                                    isLoadingReplies = false
                                )
                                Spacer(Modifier.height(16.dp))
                            }
                        }
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = Color.White)
                }
            }

            // Floating Play Buttons removed as they are now in the Action Bar

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
            if (showQuoteDialog) {
                QuoteDialog(
                    onDismiss = { showQuoteDialog = false },
                    bookTitle = book.title,
                    bookAuthor = book.author,
                    onShare = { /* Paylaşım logic */ }
                )
            }
        }
    }
}
