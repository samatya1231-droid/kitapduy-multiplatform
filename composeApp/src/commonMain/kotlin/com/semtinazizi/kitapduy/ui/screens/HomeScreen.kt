package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.model.Book
import com.semtinazizi.kitapduy.model.MoodItem
import com.semtinazizi.kitapduy.model.Podcast
import com.semtinazizi.kitapduy.model.getMoodList
import com.semtinazizi.kitapduy.ui.components.CategoryPill
import com.semtinazizi.kitapduy.ui.components.SmartImage
import com.semtinazizi.kitapduy.ui.components.PremiumUpgradeCard
import com.semtinazizi.kitapduy.ui.viewmodel.AuthViewModel
import com.semtinazizi.kitapduy.ui.viewmodel.HomeViewModel
import com.semtinazizi.kitapduy.platform.BannerAdView
import org.koin.compose.viewmodel.koinViewModel

class HomeScreen : Screen {
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val homeViewModel = koinViewModel<HomeViewModel>()
        val authViewModel = koinViewModel<AuthViewModel>()

        val allBooks by homeViewModel.books.collectAsState()
        val recommendedBooks by homeViewModel.recommendedBooks.collectAsState()
        val podcasts by homeViewModel.podcasts.collectAsState()
        val isLoading by homeViewModel.isLoading.collectAsState()
        val userProfile by authViewModel.userProfile.collectAsState()
        val groupedBooks by homeViewModel.groupedBooks.collectAsState()
        val selectedCat by homeViewModel.selectedCategory.collectAsState()
        
        val progressManager = org.koin.compose.koinInject<com.semtinazizi.kitapduy.util.KmpBookProgressManager>()

        val dailyQuote by homeViewModel.dailyQuote.collectAsState()
        val isTrailerPlaying by homeViewModel.isTrailerPlaying.collectAsState()
        val trailerBook by homeViewModel.trailerBook.collectAsState()
        val showTrailerCTA by homeViewModel.showTrailerCTA.collectAsState()

        val isPremium = userProfile?.hasPremium() ?: false
        val photoUrl = userProfile?.photoUrl

        val activeCategories = remember(groupedBooks) {
            val preferred = listOf("Türk Edebiyatı", "Polisiye", "Roman", "Dünya Edebiyatı")
            val available = groupedBooks.keys.filter {
                it.isNotBlank() && it != "Tümü" && !it.equals("Genel", ignoreCase = true)
            }
            (preferred.filter { available.contains(it) } + available.filter { !preferred.contains(it) }).distinct()
        }
        val displayCategories = remember(activeCategories) { listOf("Tümü") + activeCategories }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (isLoading && allBooks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                val lazyListState = rememberLazyListState()

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = if (isPremium) 24.dp else 84.dp)
                ) {
                    // --- 1. SPOTIFY STYLE TOP BAR (Profile + Filters) ---
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Profile Picture
                            if (!photoUrl.isNullOrEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .clickable { navigator.push(UserProfileScreen()) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    SmartImage(
                                        imageUrl = photoUrl,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        isAvatar = true
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .clickable { navigator.push(UserProfileScreen()) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                                }
                            }
                            
                            Spacer(Modifier.width(8.dp))

                            // Category Pills
                            if (activeCategories.isNotEmpty()) {
                                LazyRow(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(displayCategories, key = { it }) { cat ->
                                        CategoryPill(
                                            text = cat,
                                            isSelected = selectedCat == cat,
                                            onClick = {
                                                if (cat == "Tümü") {
                                                    homeViewModel.filterByCategory("Tümü")
                                                } else {
                                                    homeViewModel.filterByCategory(cat)
                                                }
                                            }
                                        )
                                    }
                                }
                            } else {
                                Spacer(Modifier.weight(1f))
                            }

                            // Premium Crown Button
                            if (!isPremium) {
                                val crownInteractionSource = remember { MutableInteractionSource() }
                                val crownPressed by crownInteractionSource.collectIsPressedAsState()
                                val crownScale by animateFloatAsState(
                                    targetValue = if (crownPressed) 0.88f else 1f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    label = "crownScale"
                                )
                                
                                val infiniteTransition = rememberInfiniteTransition(label = "starPulse")
                                val pulseScale by infiniteTransition.animateFloat(
                                    initialValue = 0.95f,
                                    targetValue = 1.05f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1200, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "starPulseScale"
                                )
                                val pulseGlow by infiniteTransition.animateFloat(
                                    initialValue = 0.3f,
                                    targetValue = 0.7f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1200, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "starPulseGlow"
                                )

                                Box(
                                    modifier = Modifier
                                        .graphicsLayer { 
                                            scaleX = crownScale * pulseScale 
                                            scaleY = crownScale * pulseScale 
                                        }
                                        .clickable(
                                            interactionSource = crownInteractionSource,
                                            indication = null
                                        ) { /* TODO: Navigate to Paywall */ },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(Color(0xFFFFD700).copy(alpha = pulseGlow), CircleShape)
                                            .blur(12.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(
                                                        Color(0xFFFFD700),
                                                        Color(0xFFFFA500)
                                                    )
                                                ),
                                                shape = CircleShape
                                            )
                                            .shadow(4.dp, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = "Premium'a Geç",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (selectedCat == "Tümü") {
                        // --- COLLAPSING SEARCH BAR ---
                        item {
                            val searchBarAlpha = if (lazyListState.firstVisibleItemIndex > 0) 0f else 1f
                            val searchBarOffset = if (lazyListState.firstVisibleItemIndex > 0) (-20).dp else 0.dp
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        alpha = searchBarAlpha
                                        translationY = searchBarOffset.toPx()
                                    }
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { navigator.push(SearchScreen()) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Search,
                                            contentDescription = "Ara",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Kitap, yazar veya tür ara...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }
                        }

                        // --- PREMIUM UPGRADE CARD ---
                        item {
                            PremiumUpgradeCard(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                isPremium = isPremium,
                                onClick = { navigator.push(SubscriptionPaywallScreen()) }
                            )
                        }

                        // --- QUICK ACCESS GRID (Moods) ---
                        val quickAccessMoods = getMoodList().take(6)
                        if (quickAccessMoods.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    quickAccessMoods.chunked(2).forEach { rowMoods ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowMoods.forEach { mood ->
                                                MoodQuickAccessCard(
                                                    mood = mood,
                                                    modifier = Modifier.weight(1f),
                                                    onClick = { /* TODO: Mood Navigation */ }
                                                )
                                            }
                                            if (rowMoods.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // --- CONTINUE LISTENING ---
                        val lastPlayedBookId = progressManager.getLastPlayedBook()
                        val lastBook = allBooks.find { it.id == lastPlayedBookId }
                        if (lastBook != null) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                                    Text(
                                        text = "Kaldığın yerden devam et",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        ),
                                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)
                                    )
                                    
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        item {
                                            com.semtinazizi.kitapduy.ui.components.SpotifySquareCard(
                                                book = lastBook,
                                                subtitle = "Çalınıyor",
                                                onClick = { navigator.push(BookDetailScreen(lastBook)) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // --- RECOMMENDED STATIONS / DISCOVER ---
                        if (recommendedBooks.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth().padding(top = 32.dp)) {
                                    Text(
                                        text = "Senin İçin Seçildi",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        ),
                                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)
                                    )
                                    
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(items = recommendedBooks.take(10), key = { it.id }) { book ->
                                            SpotifySquareCard(
                                                book = book,
                                                subtitle = "Önerilen Kitap",
                                                onClick = { navigator.push(BookDetailScreen(book)) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        // --- PODCASTS (Sesli Sahneler) ---
                        if (podcasts.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth().padding(top = 32.dp)) {
                                    Text(
                                        text = "Popüler Podcast'ler",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        ),
                                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)
                                    )
                                    
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(items = podcasts.take(6), key = { "pod_${it.id}" }) { podcast ->
                                            SpotifySquarePodcastCard(
                                                podcast = podcast,
                                                onClick = { /* TODO: Podcast Detail */ }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // --- BANNER AD (non-premium) ---
                        if (!isPremium) {
                            item {
                                BannerAdView(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }

                    } else {
                        // Category Header
                        item {
                            CategoryHeroHeader(category = selectedCat)
                        }
                    }

                    // --- DYNAMIC CATEGORIES ---
                    if (activeCategories.isNotEmpty()) {
                        itemsIndexed(activeCategories, key = { _, category -> "category_$category" }) { index, category ->
                            val booksInThisCategory = groupedBooks[category] ?: emptyList()

                            if (booksInThisCategory.isNotEmpty() && (selectedCat == "Tümü" || selectedCat == category)) {
                                Column(modifier = Modifier.padding(top = 32.dp)) {
                                    if (selectedCat == "Tümü") {
                                        SpotifySectionHeader(
                                            title = category,
                                            onClick = { navigator.push(CategoryDetailScreen(category)) }
                                        )

                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            items(booksInThisCategory, key = { it.id }) { book ->
                                                SpotifySquareCard(
                                                    book = book,
                                                    subtitle = book.author,
                                                    onClick = { navigator.push(BookDetailScreen(book)) }
                                                )
                                            }
                                        }
                                    }
                                    else {
                                        // --- CATEGORY HUB ---
                                        
                                        // 1. Featured Book
                                        val featuredBook = booksInThisCategory.maxByOrNull { it.viewCount } ?: booksInThisCategory.firstOrNull()
                                        if (featuredBook != null) {
                                            FeaturedCategoryBook(
                                                book = featuredBook,
                                                onClick = { navigator.push(BookDetailScreen(featuredBook)) }
                                            )
                                            Spacer(Modifier.height(32.dp))
                                        }

                                        // 2. Popular Books
                                        val popularBooks = booksInThisCategory.filter { it.id != featuredBook?.id }.sortedByDescending { it.listenCount }
                                        if (popularBooks.isNotEmpty()) {
                                            SpotifySectionHeader(title = "Popüler $category Kitapları", onClick = null)
                                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                                items(popularBooks.take(10), key = { it.id }) { book ->
                                                    SpotifySquareCard(
                                                        book = book,
                                                        subtitle = book.author,
                                                        onClick = { navigator.push(BookDetailScreen(book)) }
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(32.dp))
                                        }

                                        // 3. Related Podcasts
                                        val relatedPodcasts = podcasts.filter { it.category.equals(category, ignoreCase = true) }
                                        if (relatedPodcasts.isNotEmpty()) {
                                            SpotifySectionHeader(title = "Bu Kategorideki Podcastler", onClick = null)
                                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                                items(relatedPodcasts.take(6), key = { "pod_${it.id}" }) { podcast ->
                                                    SpotifySquarePodcastCard(
                                                        podcast = podcast,
                                                        onClick = { /* TODO: Podcast Detail */ }
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(32.dp))
                                        }

                                        // 4. Discover By Author
                                        val booksByAuthor = booksInThisCategory.groupBy { it.author }
                                        if (booksByAuthor.isNotEmpty()) {
                                            Text(
                                                text = "Yazarlara Göre Keşfet",
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.onBackground
                                                ),
                                                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)
                                            )
                                            
                                            booksByAuthor.forEach { (author, authorBooks) ->
                                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Text(author, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                                }
                                                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                                    items(authorBooks, key = { it.id }) { book ->
                                                        SpotifySquareCard(
                                                            book = book,
                                                            subtitle = book.author,
                                                            onClick = { navigator.push(BookDetailScreen(book)) }
                                                        )
                                                    }
                                                }
                                                Spacer(Modifier.height(32.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Overlay Dialogs & CTAs
            if (showTrailerCTA && trailerBook != null) {
                TrailerCTA(
                    book = trailerBook!!,
                    onDismiss = { homeViewModel.dismissTrailerCTA() },
                    onListenFull = {
                        homeViewModel.dismissTrailerCTA()
                        navigator.push(BookDetailScreen(trailerBook!!))
                    }
                )
            }

        }
    }
}

// --- REUSABLE COMPONENTS ---

@Composable
fun SpotifySectionHeader(title: String, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
        if (onClick != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Tümünü Gör",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Tümünü Gör",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun SpotifySquareCard(book: Book, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            SmartImage(
                imageUrl = book.coverUrl,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = book.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SpotifySquarePodcastCard(podcast: Podcast, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(12.dp)) // Podcasts have slightly more rounded corners
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            SmartImage(
                imageUrl = podcast.imageUrl,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Podcast Badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(Color(0xFFE5B94E).copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("PODCAST", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Black)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = podcast.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = podcast.author,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MoodQuickAccessCard(mood: MoodItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .height(56.dp)
            .clickable { onClick() },
        color = mood.color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(56.dp)
                    .background(mood.color.copy(alpha = 0.2f))
            ) {
                SmartImage(
                    imageUrl = mood.imageUrl,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = mood.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
        }
    }
}

@Composable
fun CategoryHeroHeader(category: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.BottomStart
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Kategori",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Text(
                text = category,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 40.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun FeaturedCategoryBook(book: Book, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(300.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        SmartImage(
            imageUrl = book.coverUrl,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                        startY = 300f
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xFFE5B94E), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("ÖNE ÇIKAN", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = book.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = book.author,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun DailyQuoteCard(quote: Triple<String, String, String>, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Günün Sözü",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "\"${quote.first}\"",
                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "- ${quote.second}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

// --- MODALS ---

@Composable
fun TrailerCTA(book: Book, onDismiss: () -> Unit, onListenFull: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .padding(32.dp)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Fragmanı Dinledin",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier.size(120.dp).clip(RoundedCornerShape(12.dp))
                ) {
                    SmartImage(imageUrl = book.coverUrl, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Spacer(Modifier.height(16.dp))
                Text(book.title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onListenFull,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5B94E))
                ) {
                    Text("Kitabın Tamamını Dinle", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Kapat", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
fun FullQuoteDialog(quote: String, author: String, book: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).statusBarsPadding()) {
                Icon(Icons.Filled.Close, contentDescription = "Kapat", tint = MaterialTheme.colorScheme.onBackground)
            }
            
            Column(
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "\"$quote\"",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontWeight = FontWeight.Light,
                        lineHeight = 36.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(32.dp))
                Text(
                    text = author,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                if (book.isNotBlank()) {
                    Text(
                        text = book,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
