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
import androidx.compose.foundation.lazy.LazyRow
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
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.util.root
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
import com.semtinazizi.kitapduy.platform.BillingManager
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject

data class BookDetailScreen(val book: Book) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scrollState = rememberScrollState()
        val authViewModel = koinViewModel<AuthViewModel>()
        val billingManager = koinInject<BillingManager>()
        val userProfile by authViewModel.userProfile.collectAsState()
        val isPremium = (billingManager.isPremium.collectAsState().value || userProfile?.hasPremium() == true)
        val userId = userProfile?.uid ?: ""
        val userName = userProfile?.name ?: "Kullanıcı"

        val viewModel = koinScreenModel<BookDetailScreenModel>()
        val catalog = koinInject<com.semtinazizi.kitapduy.domain.repository.BookRepository>()
        var similarBooks by remember(book.id) { mutableStateOf<List<Book>>(emptyList()) }
        LaunchedEffect(book.id) {
            try {
                similarBooks = catalog.getBooks().filter {
                    it.id != book.id && book.category.isNotBlank() && it.category == book.category
                }.take(10)
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (failure: Exception) { /* Recommendations do not block offline playback. */ }
        }

        val viewCount by viewModel.viewCount.collectAsState()
        val likeCount by viewModel.likeCount.collectAsState()
        val isLiked by viewModel.isLiked.collectAsState()
        val activeListeners by viewModel.activeListeners.collectAsState()
        val comments by viewModel.comments.collectAsState()
        val actionError by viewModel.error.collectAsState()
        actionError?.let { message ->
            AlertDialog(onDismissRequest = { viewModel.error.value = null }, text = { Text(message) },
                confirmButton = { TextButton(onClick = { viewModel.error.value = null }) { Text("Tamam") } })
        }

        var showShelfDialog by remember { mutableStateOf(false) }
        var addingToShelf by remember { mutableStateOf(false) }
        var shelfMessage by remember { mutableStateOf<String?>(null) }
        shelfMessage?.let { message ->
            AlertDialog(onDismissRequest = { shelfMessage = null }, text = { Text(message) },
                confirmButton = { TextButton(onClick = { shelfMessage = null }) { Text("Tamam") } })
        }
        if (showShelfDialog) {
            AlertDialog(onDismissRequest = { if (!addingToShelf) showShelfDialog = false },
                title = { Text("Kitaplığıma ekle") },
                text = {
                    Column {
                        listOf("READING" to "Şu an dinliyorum", "FINISHED" to "Bitirdiklerim",
                            "WISHLIST" to "Dinleyeceklerim").forEach { (type, label) ->
                            TextButton(enabled = !addingToShelf, onClick = {
                                addingToShelf = true
                                viewModel.addToShelf(book, type,
                                    onSuccess = { addingToShelf = false; showShelfDialog = false; shelfMessage = "Kitap rafınıza eklendi." },
                                    onError = { addingToShelf = false; showShelfDialog = false; shelfMessage = it })
                            }) { Text(label) }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(enabled = !addingToShelf, onClick = { showShelfDialog = false }) { Text("Vazgeç") } })
        }

        var commentText by remember { mutableStateOf("") }
        var isDescriptionExpanded by remember { mutableStateOf(false) }

        val audioController = koinInject<GlobalAudioController>()
        val fullPlayerToken = remember { Any() }
        DisposableEffect(fullPlayerToken) {
            audioController.setFullPlayerVisible(true, fullPlayerToken)
            onDispose { audioController.setFullPlayerVisible(false, fullPlayerToken) }
        }
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
        var downloadMessage by remember { mutableStateOf<String?>(null) }
        downloadMessage?.let { message ->
            AlertDialog(onDismissRequest = { downloadMessage = null }, text = { Text(message) },
                confirmButton = { TextButton(onClick = { downloadMessage = null }) { Text("Tamam") } })
        }

        LaunchedEffect(book.id) {
            viewModel.loadBookData(book, userId)
        }

        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            BookParallaxHeader(book = book, scrollState = scrollState)

            Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                Spacer(Modifier.statusBarsPadding().height(80.dp))
                SmartImage(
                    imageUrl = book.coverUrl,
                    contentDescription = book.title,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(220.dp)
                        .height(320.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop,
                    fallbackText = book.title
                )
                Spacer(Modifier.height(24.dp))

                Surface(
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
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

                        Text(book.author, Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center)
                        Text("${viewCount} dinleme · ${book.category}", Modifier.fillMaxWidth().padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

                        Spacer(Modifier.height(24.dp))

                        AudioPlayerControls(
                            audioController = audioController,
                            currentPosition = currentPosition,
                            isPlaying = isPlaying,
                            currentBook = currentBook,
                            book = book,
                            isPremium = isPremium
                        )

                        Spacer(Modifier.height(32.dp))

                        BookActionBar(
                            isLiked = isLiked,
                            likeCount = likeCount,
                            currentSpeed = currentSpeed,
                            isDownloading = isDownloading,
                            isDownloaded = audioController.isDownloaded(book.id),
                            onToggleLike = { viewModel.toggleLike(book.id, userId) },
                            onShowSpeedDialog = { showSpeedDialog = true },
                            onShowAtmosphereDialog = { showAtmosphereDialog = true },
                            onShowTimerDialog = { showTimerDialog = true },
                            onShowQuoteDialog = { showQuoteDialog = true },
                            onDownloadClick = {
                                if (isPremium) {
                                    if (!audioController.isDownloaded(book.id)) {
                                        audioController.downloadBook(book) { success ->
                                            downloadMessage = if (success) "Kitap indirildi." else "Kitap indirilemedi. Lütfen tekrar deneyin."
                                        }
                                    }
                                } else {
                                    navigator.root.push(SubscriptionPaywallScreen())
                                }
                            }
                        )

                        Spacer(Modifier.height(32.dp))

                        OutlinedButton(onClick = { showShelfDialog = true },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                            Text("Kitaplığıma ekle")
                        }
                        Spacer(Modifier.height(16.dp))

                        BookDescriptionSection(
                            description = book.description,
                            isExpanded = isDescriptionExpanded,
                            onToggleExpand = { isDescriptionExpanded = !isDescriptionExpanded }
                        )

                        Spacer(Modifier.height(40.dp))

                        if (similarBooks.isNotEmpty()) {
                            Text("Bunları da Sevebilirsin", Modifier.padding(horizontal = 24.dp),
                                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(20.dp))
                            LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(similarBooks, key = { it.id }) { suggestion ->
                                    com.semtinazizi.kitapduy.ui.components.SpotifySquareCard(suggestion, suggestion.author) {
                                        navigator.push(BookDetailScreen(suggestion))
                                    }
                                }
                            }
                            Spacer(Modifier.height(40.dp))
                        }
                        PremiumAndAdsSection(
                            isPremium = isPremium,
                            onPremiumClick = { navigator.root.push(SubscriptionPaywallScreen()) }
                        )

                        BookCommentsSection(
                            comments = comments,
                            commentText = commentText,
                            onCommentTextChange = { commentText = it },
                            onSendComment = {
                                if (commentText.isNotBlank()) {
                                    viewModel.addComment(
                                        book.id,
                                        book.title,
                                        book.coverUrl,
                                        commentText,
                                        userProfile?.photoUrl,
                                        isPremium,
                                        userId,
                                        userName,
                                        onSuccess = { commentText = "" }
                                    )
                                }
                            },
                            userId = userId,
                            bookId = book.id,
                            bookTitle = book.title
                        )
                    }
                }
            }

            BookDetailTopBar(onBackClick = { navigator.pop() })

            BookDetailDialogs(
                showAtmosphereDialog = showAtmosphereDialog,
                onDismissAtmosphere = { showAtmosphereDialog = false },
                currentAtmosphere = currentAtmosphere,
                onAtmosphereSelected = {
                    currentAtmosphere = it
                    if (it != null) audioController.playAtmosphere(it, atmosphereVolume)
                    else audioController.stopAtmosphere()
                },
                atmosphereVolume = atmosphereVolume,
                onAtmosphereVolumeChange = {
                    atmosphereVolume = it
                    audioController.setAtmosphereVolume(it)
                },
                isPremium = isPremium,
                onPremiumUpgrade = { navigator.root.push(SubscriptionPaywallScreen()) },
                showSpeedDialog = showSpeedDialog,
                onDismissSpeed = { showSpeedDialog = false },
                currentSpeed = currentSpeed,
                onSpeedSelected = { audioController.setPlaybackSpeed(it) },
                showTimerDialog = showTimerDialog,
                onDismissTimer = { showTimerDialog = false },
                timerRemaining = timerRemaining,
                onTimerSelected = { audioController.startSleepTimer(it) },
                onCancelTimer = { audioController.cancelSleepTimer() },
                showQuoteDialog = showQuoteDialog,
                onDismissQuote = { showQuoteDialog = false },
                bookTitle = book.title,
                bookAuthor = book.author
            )
        }
    }
}

@Composable
private fun BookDetailTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding().padding(top = 8.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .background(Color.Black.copy(0.45f), CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = Color.White)
        }
    }
}

@Composable
private fun BookDetailDialogs(
    showAtmosphereDialog: Boolean,
    onDismissAtmosphere: () -> Unit,
    currentAtmosphere: String?,
    onAtmosphereSelected: (String?) -> Unit,
    atmosphereVolume: Float,
    onAtmosphereVolumeChange: (Float) -> Unit,
    isPremium: Boolean,
    onPremiumUpgrade: () -> Unit,
    showSpeedDialog: Boolean,
    onDismissSpeed: () -> Unit,
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    showTimerDialog: Boolean,
    onDismissTimer: () -> Unit,
    timerRemaining: Int,
    onTimerSelected: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    showQuoteDialog: Boolean,
    onDismissQuote: () -> Unit,
    bookTitle: String,
    bookAuthor: String
) {
    if (showAtmosphereDialog) {
        AtmosphereDialog(
            onDismiss = onDismissAtmosphere,
            currentSound = currentAtmosphere,
            onSoundSelected = onAtmosphereSelected,
            volume = atmosphereVolume,
            onVolumeChange = onAtmosphereVolumeChange,
            isPremiumUser = isPremium,
            onUpgradeClick = onPremiumUpgrade
        )
    }
    if (showSpeedDialog) {
        PlaybackSpeedDialog(
            onDismiss = onDismissSpeed,
            currentSpeed = currentSpeed,
            onSpeedSelected = onSpeedSelected
        )
    }
    if (showTimerDialog) {
        SleepTimerDialog(
            onDismiss = onDismissTimer,
            remainingMinutes = timerRemaining,
            onTimerSelected = onTimerSelected,
            onCancelTimer = onCancelTimer
        )
    }
    if (showQuoteDialog) {
        QuoteDialog(
            onDismiss = onDismissQuote,
            bookTitle = bookTitle,
            bookAuthor = bookAuthor
        )
    }
}

@Composable
private fun BookParallaxHeader(book: Book, scrollState: ScrollState) {
    // Ambient Glow (Arkaplan Aurası)
    SmartImage(
        imageUrl = book.coverUrl,
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .scale(1.2f)
            .blur(120.dp)
            .alpha(0.15f),
        contentScale = ContentScale.Crop,
        fallbackText = book.title
    )
}

@Composable
private fun BookInfoSection(book: Book, activeListeners: Int?) {
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
                Text(text = activeListeners?.toString() ?: "—", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PremiumAndAdsSection(isPremium: Boolean, onPremiumClick: () -> Unit) {
    if (!isPremium) {
        PremiumTrialBanner(
            onClick = onPremiumClick,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(8.dp))
        BannerAdView(modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AudioPlayerControls(
    audioController: GlobalAudioController,
    currentPosition: Float,
    isPlaying: Boolean,
    currentBook: Book?,
    book: Book,
    isPremium: Boolean
) {
    val duration by audioController.duration.collectAsState()
    val selected = currentBook?.id == book.id
    val position = if (selected) currentPosition.coerceIn(0f, 1f) else 0f
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(36.dp)).padding(24.dp)) {
        // Slider
        Slider(
            value = position,
            enabled = selected && duration > 0f,
            onValueChange = { audioController.seekTo(it, expectedBookId = book.id) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .semantics { contentDescription = "Oynatma konumu" },
            colors = SliderDefaults.colors(
                thumbColor = BrandGold,
                activeTrackColor = BrandGold,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(playbackTimeLabel((position * duration).toLong()), Modifier.testTag("book-elapsed"),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(playbackTimeLabel(if (selected) duration.toLong() else 0L),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // Butonlar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("book-skip-back"),
                enabled = selected && duration > 0f,
                onClick = { audioController.skipBy(-15_000, expectedBookId = book.id) }
            ) {
                Icon(Icons.Default.FastRewind, contentDescription = "15 saniye geri")
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
                modifier = Modifier.size(64.dp).testTag("book-play-pause")
            ) {
                Icon(
                    imageVector = if (isPlaying && currentBook?.id == book.id) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Oynat/Duraklat",
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("book-skip-forward"),
                enabled = selected && duration > 0f,
                onClick = { audioController.skipBy(15_000, expectedBookId = book.id) }
            ) {
                Icon(Icons.Default.FastForward, contentDescription = "15 saniye ileri")
            }
        }
    }
}

private fun playbackTimeLabel(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0) / 1000
    return "${(seconds / 60).toString().padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}"
}

@Composable
private fun BookActionBar(
    isLiked: Boolean,
    likeCount: Int,
    currentSpeed: Float,
    isDownloading: Boolean,
    isDownloaded: Boolean,
    onToggleLike: () -> Unit,
    onShowSpeedDialog: () -> Unit,
    onShowAtmosphereDialog: () -> Unit,
    onShowTimerDialog: () -> Unit,
    onShowQuoteDialog: () -> Unit,
    onDownloadClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BookActionBarItem(
            icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            label = "$likeCount",
            tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurface,
            onClick = onToggleLike
        )
        BookActionBarItem(
            icon = Icons.Default.Speed,
            label = "${currentSpeed}x",
            tint = MaterialTheme.colorScheme.onSurface,
            onClick = onShowSpeedDialog
        )
        BookActionBarItem(
            icon = Icons.Default.Air,
            label = "Atmosfer",
            tint = MaterialTheme.colorScheme.onSurface,
            onClick = onShowAtmosphereDialog
        )
        BookActionBarItem(
            icon = Icons.Default.Timer,
            label = "Uyku",
            tint = MaterialTheme.colorScheme.onSurface,
            onClick = onShowTimerDialog
        )
        BookActionBarItem(
            icon = Icons.Default.FormatQuote,
            label = "Alıntı",
            tint = MaterialTheme.colorScheme.onSurface,
            onClick = onShowQuoteDialog
        )
        BookActionBarItem(
            icon = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
            label = if (isDownloading) "İndiriliyor" else "İndir",
            tint = MaterialTheme.colorScheme.onSurface,
            onClick = onDownloadClick
        )
    }
}

@Composable
private fun BookActionBarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .semantics(mergeDescendants = true) {}
    ) {
        Icon(icon, null, tint = tint)
        Text(label, fontSize = 12.sp)
    }
}

@Composable
private fun BookDescriptionSection(
    description: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text("Hakkında", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            text = description.ifEmpty { "Bu kitap hakkında henüz bir açıklama eklenmemiş." },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            lineHeight = 24.sp,
            maxLines = if (isExpanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable { onToggleExpand() }
        )
        if (description.length > 150) {
            Text(
                text = if (isExpanded) "Daha az göster" else "Devamını oku",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onToggleExpand() }.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun BookCommentsSection(
    comments: List<Comment>,
    commentText: String,
    onCommentTextChange: (String) -> Unit,
    onSendComment: () -> Unit,
    userId: String,
    bookId: String,
    bookTitle: String
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(text = "Yorumlar (${comments.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        // Yorum Yazma Alanı
        OutlinedTextField(
            value = commentText,
            onValueChange = onCommentTextChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ne düşünüyorsun?") },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandGold,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(0.1f)
            ),
            trailingIcon = {
                IconButton(
                    onClick = onSendComment,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                ) {
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
                bookId = bookId,
                bookTitle = bookTitle,
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
