package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.model.SupabaseBookshelf
import com.semtinazizi.kitapduy.ui.components.SmartImage
import com.semtinazizi.kitapduy.ui.viewmodel.FeedContentTarget
import com.semtinazizi.kitapduy.ui.viewmodel.LibraryScreenModel
import cafe.adriel.voyager.koin.koinScreenModel
import kotlinx.coroutines.launch

data class PrideShelfScreen(val shelfType: String = "FINISHED", val title: String = "Gurur Rafım") : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val libraryViewModel = koinScreenModel<LibraryScreenModel>()
        val scope = rememberCoroutineScope()

        val finishedBooks by libraryViewModel.bookshelfItems.collectAsState()
        val error by libraryViewModel.errorMessage.collectAsState()
        val isLoading by libraryViewModel.isLoading.collectAsState()

        LaunchedEffect(shelfType) {
            libraryViewModel.loadBookshelf(shelfType)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (error != null) {
                    Text(error.orEmpty(), modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.error)
                } else if (finishedBooks.isEmpty()) {
                    Text(
                        "Bu raf henüz boş.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(finishedBooks) { book ->
                            BookOnShelf(book) {
                                scope.launch {
                                    when (val target = libraryViewModel.resolveNavigationTarget(book)) {
                                        is FeedContentTarget.BookTarget -> navigator.push(BookDetailScreen(target.book))
                                        is FeedContentTarget.PodcastTarget -> navigator.push(PodcastDetailScreen(target.episode, target.podcastTitle))
                                        is FeedContentTarget.NotFound -> Unit
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookOnShelf(book: SupabaseBookshelf, onClick: () -> Unit = {}) {
    Column(modifier = Modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        // Kitap Kapağı ve 3D Gölgesi
        Box(
            modifier = Modifier
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.DarkGray)
        ) {
            val coverUrl = book.bookCoverUrl
            if (!coverUrl.isNullOrBlank()) {
                SmartImage(
                    imageUrl = coverUrl,
                    contentDescription = book.bookTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    fallbackText = book.bookTitle ?: ""
                )
            }

            // 3D Shadow Overlay for "Book Spine" feel
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Ahşap Raf (Wooden Shelf) Tasarımı
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF8D6E63), Color(0xFF5D4037))
                    ),
                    RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)
                )
        )

        Text(
            text = book.bookTitle ?: "İsimsiz",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
