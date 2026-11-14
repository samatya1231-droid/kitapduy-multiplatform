package com.semtinazizi.kitapduy.ui.screens.tabs

import androidx.compose.ui.platform.testTag
import com.semtinazizi.kitapduy.ui.screens.LiteraryAdvisorScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.util.root
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import coil3.compose.AsyncImage
import com.semtinazizi.kitapduy.model.SupabaseBookshelf
import com.semtinazizi.kitapduy.ui.viewmodel.LibraryScreenModel
import org.koin.compose.viewmodel.koinViewModel
import com.semtinazizi.kitapduy.ui.screens.BookDetailScreen
import cafe.adriel.voyager.koin.koinScreenModel
import com.semtinazizi.kitapduy.ui.components.PremiumUpgradeCard
import com.semtinazizi.kitapduy.ui.screens.SubscriptionPaywallScreen
import com.semtinazizi.kitapduy.ui.viewmodel.AuthViewModel
import com.semtinazizi.kitapduy.platform.BannerAdView

object LibraryTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val title = "Kitaplık"
            val icon = rememberVectorPainter(Icons.Default.Favorite)

            return remember {
                TabOptions(
                    index = 1u,
                    title = title,
                    icon = icon
                )
            }
        }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        // Koin üzerinden ViewModel oluşturuyoruz
        val viewModel = koinScreenModel<LibraryScreenModel>()

        var showShelves by remember { mutableStateOf(false) }
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = listOf("Favoriler", "İndirilenler", "Geçmiş")
        val shelfTypes = listOf("FAVORITE", "DOWNLOADED", "LISTENING")

        val bookshelfItems by viewModel.bookshelfItems.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val errorMessage by viewModel.errorMessage.collectAsState()

        val authViewModel = koinViewModel<AuthViewModel>()
        val userProfile by authViewModel.userProfile.collectAsState()
        val isPremium = userProfile?.hasPremium() == true

        LaunchedEffect(selectedTab, userProfile?.uid) {
            viewModel.loadBookshelf(shelfTypes[selectedTab])
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kitaplığım",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    onClick = { (navigator.parent ?: navigator).push(LiteraryAdvisorScreen()) },
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF6D4C2D).copy(alpha = 0.14f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6D4C2D).copy(alpha = 0.28f)),
                    modifier = Modifier.testTag("library-advisor-pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text("✨", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Danışman",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Box {
                TextButton(onClick = { showShelves = true }, modifier = Modifier.testTag("library-shelves")) { Text("Raflarım") }
                DropdownMenu(expanded = showShelves, onDismissRequest = { showShelves = false }) {
                    listOf("READING" to "Dinliyorum", "FINISHED" to "Bitirdiklerim", "WISHLIST" to "Dinleyeceklerim").forEach { (type, title) ->
                        DropdownMenuItem(text = { Text(title) }, onClick = {
                            showShelves = false
                            (navigator.parent ?: navigator).push(com.semtinazizi.kitapduy.ui.screens.PrideShelfScreen(type, title))
                        })
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Sekmeler
            Surface(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { i, title ->
                        val isSelected = selectedTab == i
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) Color(0xFF6D4C2D)
                                    else Color.Transparent
                                )
                                .clickable { selectedTab = i }.testTag("library-tab-$i"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) Color.White
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
            }

            PremiumUpgradeCard(
                modifier = Modifier.padding(top = 16.dp),
                isPremium = isPremium,
                onClick = { navigator.root.push(SubscriptionPaywallScreen()) }
            )

            Spacer(Modifier.height(16.dp))

            // İçerik
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (errorMessage != null) {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { viewModel.loadBookshelf(shelfTypes[selectedTab]) }) { Text("Tekrar dene") }
                    }
                } else if (bookshelfItems.isEmpty()) {
                    Text(
                        text = "${tabs[selectedTab]} listeniz şu an boş.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        if (!isPremium) {
                            item(span = { GridItemSpan(2) }) {
                                BannerAdView(modifier = Modifier.padding(bottom = 8.dp))
                            }
                        }
                        items(bookshelfItems, key = { it.bookId }) { item ->
                            LibraryCoverCard(
                                item = item,
                                onClick = {
                                    viewModel.bookFor(item)?.let { (navigator.parent ?: navigator).push(BookDetailScreen(it)) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookshelfItemCard(item: SupabaseBookshelf, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                val coverUrl = item.bookCoverUrl
                if (!coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = item.bookTitle,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.bookTitle?.ifBlank { "İsimsiz Kitap" } ?: "İsimsiz Kitap",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LibraryCoverCard(item: SupabaseBookshelf, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        AsyncImage(model = item.bookCoverUrl, contentDescription = item.bookTitle,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(0.72f).clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant))
        Spacer(Modifier.height(10.dp))
        Text(item.bookTitle.orEmpty(), style = MaterialTheme.typography.titleMedium,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
