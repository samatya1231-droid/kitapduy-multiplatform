package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.semtinazizi.kitapduy.model.Book
import com.semtinazizi.kitapduy.model.Podcast
import com.semtinazizi.kitapduy.model.ScienceArticle
import com.semtinazizi.kitapduy.model.getMoodList
import com.semtinazizi.kitapduy.ui.components.VerticalBookCard
import com.semtinazizi.kitapduy.ui.theme.*
import com.semtinazizi.kitapduy.ui.viewmodel.HomeViewModel
import org.koin.compose.viewmodel.koinViewModel

data class MoodDetailScreen(val moodId: String) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val homeViewModel = koinViewModel<HomeViewModel>()
        
        val mood = remember { getMoodList().find { it.id == moodId } }
        
        if (mood == null) {
            LaunchedEffect(Unit) { navigator.pop() }
            return
        }

        val allBooks by homeViewModel.books.collectAsState()
        val allPodcasts by homeViewModel.podcasts.collectAsState()
        val allArticles by homeViewModel.articles.collectAsState()
        val isLoading by homeViewModel.isLoading.collectAsState()

        val filteredBooks = remember(allBooks, mood.keywords) {
            allBooks.filter { book ->
                mood.keywords.any { keyword ->
                    book.title.contains(keyword, ignoreCase = true) ||
                    book.category.contains(keyword, ignoreCase = true) ||
                    book.description.contains(keyword, ignoreCase = true)
                }
            }.take(12).let { if (it.isEmpty() && allBooks.isNotEmpty()) allBooks.shuffled().take(6) else it }
        }

        val filteredPodcasts = remember(allPodcasts, mood.keywords, mood.id) {
            val matches = allPodcasts.filter { podcast ->
                podcast.rssUrl.isNotBlank() && (
                    mood.keywords.any { keyword ->
                        podcast.name.contains(keyword, ignoreCase = true) ||
                        podcast.category.contains(keyword, ignoreCase = true) ||
                        podcast.author.contains(keyword, ignoreCase = true)
                    } || podcast.category.contains(mood.name, ignoreCase = true)
                )
            }.take(10)
            
            if (matches.isEmpty() && allPodcasts.isNotEmpty()) {
                // Eşleşme yoksa bile rssUrl'i dolu olanlardan öneri göster
                allPodcasts.filter { it.rssUrl.isNotBlank() }
                    .shuffled(kotlin.random.Random(moodId.hashCode()))
                    .take(5)
            } else matches
        }

        val filteredArticles = remember(allArticles, mood.keywords, mood.id) {
            val matches = allArticles.filter { article ->
                mood.keywords.any { keyword ->
                    article.title.contains(keyword, ignoreCase = true) ||
                    article.description.contains(keyword, ignoreCase = true) ||
                    article.source.contains(keyword, ignoreCase = true)
                } || article.source.contains(mood.name, ignoreCase = true)
            }.take(10)

            if (matches.isEmpty() && allArticles.isNotEmpty()) {
                allArticles.shuffled(kotlin.random.Random(moodId.hashCode())).take(5)
            } else matches
        }

        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Immersive Header
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(350.dp)) {
                        AsyncImage(
                            model = mood.imageUrl,
                            contentDescription = mood.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.4f),
                                            MaterialTheme.colorScheme.background
                                        )
                                    )
                                )
                        )
                        
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 24.dp, vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.15f),
                                shape = CircleShape,
                                modifier = Modifier.size(90.dp).blur(if (mood.imageUrl.isEmpty()) 0.dp else 4.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(mood.emoji, fontSize = 48.sp)
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                            Text(
                                text = mood.name,
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SleekChampagne,
                                    letterSpacing = 1.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = mood.description,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color.White.copy(alpha = 0.9f),
                                    lineHeight = 26.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }

                // Books Section
                item {
                    SectionHeader("Sana Özel Kitaplar")
                    if (isLoading && allBooks.isEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(5) { BookShimmer() }
                        }
                    } else if (filteredBooks.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredBooks) { book ->
                                VerticalBookCard(book = book, onClick = { navigator.push(BookDetailScreen(book)) })
                            }
                        }
                    } else {
                        EmptyStateMessage("Bu ruh hali için henüz kitap bulamadık.")
                    }
                }

                // Podcasts Section
                item {
                    SectionHeader("Podcast Önerileri")
                    if (isLoading && allPodcasts.isEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(4) { PodcastShimmer() }
                        }
                    } else if (filteredPodcasts.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filteredPodcasts) { podcast ->
                                PremiumPodcastCard(podcast = podcast) {
                                    navigator.push(PodcastEpisodesScreen(podcast.id, podcast.name, podcast.rssUrl))
                                }
                            }
                        }
                    } else {
                        EmptyStateMessage("Bu kategoride henüz podcast bulunmuyor.")
                    }
                }

                // Articles Section
                item {
                    SectionHeader("İlgini Çekebilecek Makaleler")
                    if (isLoading && allArticles.isEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(3) { ArticleShimmer() }
                        }
                    } else if (filteredArticles.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filteredArticles) { article ->
                                PremiumArticleCard(article = article) {
                                    navigator.push(ArticleDetailScreen(article.link, article.title))
                                }
                            }
                        }
                    } else {
                        EmptyStateMessage("Okuyacak yeni makaleler yakında eklenecek.")
                    }
                }
            }

            // Navigation Bar (Floating Back Button)
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navigator.pop() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Geri",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.ExtraBold,
            color = SleekChampagne,
            letterSpacing = 0.5.sp
        ),
        modifier = Modifier.padding(start = 24.dp, top = 40.dp, bottom = 20.dp)
    )
}

@Composable
fun EmptyStateMessage(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
fun BookShimmer() {
    Column(modifier = Modifier.width(130.dp)) {
        Box(modifier = Modifier.size(130.dp, 190.dp).shimmerEffect())
        Spacer(Modifier.height(12.dp))
        Box(modifier = Modifier.fillMaxWidth(0.8f).height(16.dp).shimmerEffect())
        Spacer(Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth(0.5f).height(12.dp).shimmerEffect())
    }
}

@Composable
fun PodcastShimmer() {
    Column(modifier = Modifier.width(160.dp)) {
        Box(modifier = Modifier.size(160.dp).shimmerEffect())
        Spacer(Modifier.height(14.dp))
        Box(modifier = Modifier.fillMaxWidth(0.9f).height(18.dp).shimmerEffect())
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp).shimmerEffect())
    }
}

@Composable
fun ArticleShimmer() {
    Box(modifier = Modifier.size(280.dp, 150.dp).shimmerEffect())
}

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsets by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "shimmer"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.05f),
                Color.White.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.05f),
            ),
            start = Offset(startOffsets, 0f),
            end = Offset(startOffsets + size.width.toFloat(), size.height.toFloat())
        ),
        shape = RoundedCornerShape(16.dp)
    ).onGloballyPositioned { size = it.size }
}

@Composable
fun PremiumPodcastCard(podcast: Podcast, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() }
    ) {
        Box {
            AsyncImage(
                model = podcast.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SleekSurface)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                        )
                    )
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = podcast.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
        Text(
            text = podcast.author,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = SleekGray
        )
    }
}

@Composable
fun PremiumArticleCard(article: ScienceArticle, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(150.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = article.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(20.dp))
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = article.source.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = SleekChampagne,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = article.title,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
