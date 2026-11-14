package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.util.root
import org.koin.compose.viewmodel.koinViewModel
import com.semtinazizi.kitapduy.platform.BillingManager
import com.semtinazizi.kitapduy.ui.viewmodel.HomeViewModel
import com.semtinazizi.kitapduy.ui.viewmodel.AuthViewModel
import com.semtinazizi.kitapduy.ui.components.*
import com.semtinazizi.kitapduy.ui.theme.SleekChampagne
import com.semtinazizi.kitapduy.ui.theme.SleekDarkBackground
import com.semtinazizi.kitapduy.model.Book
import com.semtinazizi.kitapduy.model.Podcast
import com.semtinazizi.kitapduy.model.ScienceArticle
import com.semtinazizi.kitapduy.model.getMoodList
import coil3.compose.AsyncImage
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class HomeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val tabs = cafe.adriel.voyager.navigator.tab.LocalTabNavigator.current
        val homeViewModel = koinViewModel<HomeViewModel>()
        val authViewModel = koinViewModel<AuthViewModel>()
        val billingManager = org.koin.compose.koinInject<BillingManager>()

        val userProfile by authViewModel.userProfile.collectAsState()
        val photoUrl = userProfile?.photoUrl
        val isPremium = (billingManager.isPremium.collectAsState().value || userProfile?.hasPremium() == true)
        
        val books by homeViewModel.books.collectAsState()
        val recommendedBooks by homeViewModel.recommendedBooks.collectAsState()
        val groupedBooks by homeViewModel.groupedBooks.collectAsState()
        val dailyQuote by homeViewModel.dailyQuote.collectAsState()
        val podcasts by homeViewModel.podcasts.collectAsState()
        val articles by homeViewModel.articles.collectAsState()
        val weeklyStars by homeViewModel.weeklyStars.collectAsState()
        val isLoading by homeViewModel.isLoading.collectAsState()
        
        var showQuoteDialog by remember { mutableStateOf(false) }
        var selectedQuote by remember { mutableStateOf<Triple<String, String, String>?>(null) }
        
        val progressManager = org.koin.compose.koinInject<com.semtinazizi.kitapduy.util.KmpBookProgressManager>()
        val audioController = org.koin.compose.koinInject<com.semtinazizi.kitapduy.ui.viewmodel.GlobalAudioController>()
        val playingBook by audioController.currentBook.collectAsState()
        val positionFraction by audioController.currentPosition.collectAsState()
        val durationMs by audioController.duration.collectAsState()
        val lastId = playingBook?.id ?: progressManager.getLastPlayedBook()
        val continueListeningBook = books.firstOrNull { it.id == lastId && progressManager.getProgress(it.id) > 0 }
        val displayBook = continueListeningBook ?: books.firstOrNull()
        val savedPosition = continueListeningBook?.let {
            if (playingBook?.id == it.id && durationMs > 0) (positionFraction * durationMs).toLong() else progressManager.getProgress(it.id)
        } ?: 0L

        val lazyListState = rememberLazyListState()
        
        val searchBarAlpha by remember {
            derivedStateOf {
                if (lazyListState.firstVisibleItemIndex > 0) 1f
                else {
                    val offset = lazyListState.firstVisibleItemScrollOffset
                    (offset / 300f).coerceIn(0f, 1f)
                }
            }
        }

        val moods = remember { getMoodList().take(6) }

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Blur background
                if (books.isNotEmpty() && !isLoading) {
                    val topBookUrl = books.firstOrNull { it.coverUrl.isNotEmpty() }?.coverUrl
                    if (topBookUrl != null) {
                        AsyncImage(
                            model = topBookUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(500.dp)
                                .blur(80.dp)
                                .graphicsLayer { 
                                    alpha = (0.15f - (searchBarAlpha * 0.1f)).coerceAtLeast(0.02f)
                                },
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(500.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                                    )
                                )
                        )
                    }
                }
                
                if (isLoading && books.isEmpty()) {
                    HomeShimmer()
                } else if (books.isEmpty() && !isLoading) {
                    EmptyHomeState()
                } else {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        item {
                            GreetingAndProfileBar(photoUrl, isPremium,
                                onProfileClick = { navigator.push(UserProfileScreen()) },
                                onPremiumClick = { navigator.root.push(SubscriptionPaywallScreen()) })
                            SearchBarSection(
                                Modifier
                                    .padding(horizontal = 16.dp)
                                    .graphicsLayer { alpha = (1f - searchBarAlpha).coerceIn(0f, 1f) }
                            ) { navigator.push(SearchScreen()) }
                        }
                        item {
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item { FilterChip(selected = true, onClick = {}, label = { Text("Tümü") }) }
                                item { FilterChip(selected = false, onClick = { tabs.current = com.semtinazizi.kitapduy.ui.screens.tabs.EntertainmentTab }, label = { Text("Eğlence") }) }
                                item { FilterChip(selected = false, onClick = { tabs.current = com.semtinazizi.kitapduy.ui.screens.tabs.AskidaTab }, label = { Text("Askıda") }) }
                                items(groupedBooks.keys.toList()) { category ->
                                    FilterChip(selected = false, onClick = { navigator.push(CategoryDetailScreen(category)) },
                                        label = { Text(category) })
                                }
                            }
                        }
                        displayBook?.let { book ->
                            item {
                                ContinueListeningCard(book, hasHistory = continueListeningBook != null,
                                    positionMs = savedPosition, onClick = { navigator.push(BookDetailScreen(book)) })
                            }
                        }
                        if (recommendedBooks.isNotEmpty()) item {
                            SpotifySectionHeader("Yeni hikâyeler keşfet")
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(recommendedBooks, key = { it.id }) { book ->
                                    SpotifySquareCard(book, book.author) { navigator.push(BookDetailScreen(book)) }
                                }
                            }
                        }
                        item {
                            SpotifySectionHeader("Bugün nasıl bir hikâye?")
                            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(moods, key = { it.id }) { mood ->
                                    MoodQuickAccessCard(mood, Modifier.width(156.dp)) { navigator.push(MoodDetailScreen(mood.id)) }
                                }
                            }
                        }
                        if (podcasts.isNotEmpty()) item {
                            SpotifySectionHeader("Podcast dünyası")
                            PodcastRow(podcasts.take(6)) {
                                navigator.push(PodcastEpisodesScreen(it.id, it.name, it.rssUrl, it.imageUrl))
                            }
                        }
                        if (!isPremium) item {
                            PremiumUpgradeCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 32.dp),
                                isPremium = false, onClick = { navigator.root.push(SubscriptionPaywallScreen()) })
                        }
                        groupedBooks.forEach { (category, categoryBooks) ->
                            if (categoryBooks.isNotEmpty()) item(key = "category_" + category) {
                                DynamicCategoryRow(category, categoryBooks,
                                    onSeeAllClick = { navigator.push(CategoryDetailScreen(category)) },
                                    onBookClick = { navigator.push(BookDetailScreen(it)) })
                            }
                        }
                        if (articles.isNotEmpty()) item {
                            SpotifySectionHeader("Bilim & Düşünce")
                            ArticlesRow(articles) { navigator.push(ArticleDetailScreen(it.link, it.title)) }
                        }
                    }
                }

                // Sticky Compact Search Bar
                if (searchBarAlpha > 0.05f) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .graphicsLayer { alpha = searchBarAlpha },
                        color = MaterialTheme.colorScheme.background,
                        shadowElevation = (8f * searchBarAlpha).dp
                    ) {
                        Box(modifier = Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
                            SearchBarSection(
                                modifier = Modifier,
                                onClick = { navigator.push(SearchScreen()) }
                            )
                        }
                    }
                }
            }
        }

        if (showQuoteDialog && selectedQuote != null) {
            FullQuoteDialog(
                quote = selectedQuote!!.first,
                author = selectedQuote!!.second,
                book = selectedQuote!!.third,
                onDismiss = { showQuoteDialog = false }
            )
        }
    }

    @Composable
    private fun GreetingAndProfileBar(
        photoUrl: String?,
        isPremium: Boolean,
        onProfileClick: () -> Unit,
        onPremiumClick: () -> Unit
    ) {
        val greeting = getGreeting()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Bugün ruh halin nasıl?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!isPremium) {
                    PremiumBadge(onClick = onPremiumClick)
                }
                ProfileAvatar(photoUrl = photoUrl, onClick = onProfileClick)
            }
        }
    }

    @Composable
    private fun PremiumBadge(onClick: () -> Unit) {
        Surface(
            modifier = Modifier.clickable { onClick() },
            shape = RoundedCornerShape(20.dp),
            color = SleekChampagne,
            shadowElevation = 4.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("PREMİUM", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 0.5.sp)
            }
        }
    }

    @Composable
    private fun ProfileAvatar(photoUrl: String?, onClick: () -> Unit) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .border(2.dp, SleekChampagne, CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            SmartImage(
                imageUrl = photoUrl,
                contentDescription = "Profile",
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop,
                isAvatar = true
            )
        }
    }

    @Composable
    private fun SearchBarSection(modifier: Modifier = Modifier, onClick: () -> Unit) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Filled.Search, contentDescription = "Ara", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(22.dp))
                Text("Kitap, yazar veya kategori ara...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 15.sp)
            }
        }
    }

    @Composable
    private fun ContinueListeningCard(book: Book, hasHistory: Boolean, positionMs: Long, onClick: () -> Unit) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
            SpotifySectionHeader(if (hasHistory) "Dinlemeye devam et" else "İlk hikâyeni keşfet")
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(100.dp)
                    .clickable { onClick() },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = book.coverUrl,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp).clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f).padding(vertical = 14.dp)) {
                        Text("SESLİ KİTAP", color = SleekChampagne, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text(book.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(book.author, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 13.sp, maxLines = 1)
                        Spacer(modifier = Modifier.weight(1f))
                        if (hasHistory) Text("${positionMs / 60_000} dakika dinlendi", style = MaterialTheme.typography.labelSmall)

                    }
                    Box(
                        modifier = Modifier.padding(end = 16.dp).size(40.dp).background(SleekChampagne, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Oynat", tint = Color.Black, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }

    @Composable
    private fun DailyQuoteCard(quote: Triple<String, String, String>?, onClick: () -> Unit) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable { onClick() },
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, SleekChampagne.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.FormatQuote, null, tint = SleekChampagne, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("GÜNÜN ALINTISI", style = MaterialTheme.typography.labelSmall, color = SleekChampagne, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = quote?.first ?: "Kitaplar, sessizce konuşan dostlardır.",
                    style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, lineHeight = 24.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "— ${quote?.second ?: "Anonim"}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = SleekChampagne,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }

    @Composable
    private fun WeeklyStarsSection(books: List<Book>, onBookClick: (Book) -> Unit) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(books) { index, book ->
                Box {
                    SpotifySquareCard(book = book, subtitle = book.author, onClick = { onBookClick(book) })
                    Surface(
                        modifier = Modifier.align(Alignment.TopStart).offset(x = 8.dp, y = 8.dp),
                        shape = CircleShape,
                        color = SleekChampagne
                    ) {
                        Text(
                            text = "#${index + 1}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun DynamicCategoryRow(
        title: String,
        books: List<Book>,
        onSeeAllClick: () -> Unit,
        onBookClick: (Book) -> Unit
    ) {
        Column(modifier = Modifier.padding(vertical = 18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold, 
                        letterSpacing = (-0.5).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = "Tümünü Gör",
                    style = MaterialTheme.typography.labelMedium,
                    color = SleekChampagne,
                    modifier = Modifier.clickable { onSeeAllClick() }
                )
            }
            Spacer(Modifier.height(16.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(books) { book ->
                    SpotifySquareCard(book = book, subtitle = book.author, onClick = { onBookClick(book) })
                }
            }
        }
    }

    @Composable
    private fun PodcastRow(podcasts: List<Podcast>, onPodcastClick: (Podcast) -> Unit) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(podcasts) { podcast ->
                Column(modifier = Modifier.width(140.dp).clickable { onPodcastClick(podcast) }) {
                    AsyncImage(
                        model = podcast.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(140.dp).clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = podcast.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = podcast.author,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
            }
        }
    }

    @Composable
    private fun ArticlesRow(articles: List<ScienceArticle>, onArticleClick: (ScienceArticle) -> Unit) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(articles) { article ->
                Surface(
                    modifier = Modifier.width(260.dp).height(130.dp).clickable { onArticleClick(article) },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = article.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.width(100.dp).fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(article.source.uppercase(), style = MaterialTheme.typography.labelSmall, color = SleekChampagne, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = article.title,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold, 
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun HomeShimmer() {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp).shimmerEffect())
            Spacer(Modifier.height(24.dp))
            Box(modifier = Modifier.fillMaxWidth().height(40.dp).shimmerEffect())
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f).height(160.dp).shimmerEffect())
                Box(modifier = Modifier.weight(1f).height(160.dp).shimmerEffect())
            }
            Spacer(Modifier.height(32.dp))
            Box(modifier = Modifier.width(200.dp).height(30.dp).shimmerEffect())
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(3) { Box(modifier = Modifier.size(120.dp, 180.dp).shimmerEffect()) }
            }
        }
    }

    @Composable
    private fun EmptyHomeState() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = SleekChampagne, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Henüz içerik bulunamadı", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text("Lütfen internet bağlantınızı kontrol edin", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 14.sp)
        }
    }

    private fun Modifier.shimmerEffect(): Modifier = composed {
        var size by remember { mutableStateOf(IntSize.Zero) }
        val transition = rememberInfiniteTransition()
        val startOffsets by transition.animateFloat(
            initialValue = -2 * size.width.toFloat(),
            targetValue = 2 * size.width.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1000)
            )
        )

        background(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFB8B5B5),
                    Color(0xFF8F8B8B),
                    Color(0xFFB8B5B5),
                ),
                start = Offset(startOffsets, 0f),
                end = Offset(startOffsets + size.width.toFloat(), size.height.toFloat())
            ),
            shape = RoundedCornerShape(16.dp)
        ).onGloballyPositioned { size = it.size }
    }

    @Composable
    private fun FullQuoteDialog(quote: String, author: String, book: String, onDismiss: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.FormatQuote, null, tint = SleekChampagne, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Günün İlhamı", fontWeight = FontWeight.Black)
                }
            },
            text = {
                Column {
                    Text(
                        text = "\"$quote\"",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontStyle = FontStyle.Italic,
                            lineHeight = 24.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "— $author",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = SleekChampagne,
                        modifier = Modifier.align(Alignment.End)
                    )
                    if (book.isNotBlank()) {
                        Text(
                            text = book,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Kapat", color = SleekChampagne, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    private fun getGreeting(): String {
        val hour = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
        return when (hour) {
            in 5..11 -> "Günaydın"
            in 12..17 -> "İyi Günler"
            in 18..21 -> "İyi Akşamlar"
            else -> "İyi Geceler"
        }
    }
}
