package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.semtinazizi.kitapduy.model.Book
import com.semtinazizi.kitapduy.platform.BookDownloader
import com.semtinazizi.kitapduy.platform.createBookDownloader
import com.semtinazizi.kitapduy.ui.viewmodel.HomeViewModel
import org.koin.compose.viewmodel.koinViewModel

class DownloadsScreen : Screen {
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val homeViewModel: HomeViewModel = koinViewModel()
        val bookDownloader = remember { createBookDownloader() }
        
        val BrandGold = Color(0xFFFFD700)
        val surfaceColor = MaterialTheme.colorScheme.surface

        val allBooks by homeViewModel.books.collectAsState()
        val allPodcasts by homeViewModel.podcasts.collectAsState()
        
        // Yeniden yüklemeyi tetiklemek için bir state
        var refreshTrigger by remember { mutableStateOf(0) }

        val downloadedBooks by remember(allBooks, allPodcasts, refreshTrigger) {
            val downloadedIds = bookDownloader.getDownloadedBookIds().toSet()
            
            val list = mutableListOf<Book>()
            // Normal Kitapları Ekle
            allBooks.forEach { book ->
                if (downloadedIds.contains(book.id)) {
                    list.add(book)
                }
            }
            // Podcastleri Book tipine dönüştürüp ekle (eski mantıkta Podcast'ler Book gibi gösteriliyordu)
            allPodcasts.forEach { podcast ->
                if (downloadedIds.contains(podcast.id)) {
                    list.add(
                        Book(
                            id = podcast.id,
                            title = podcast.name,
                            author = podcast.author,
                            category = "Podcast",
                            coverUrl = podcast.imageUrl,
                            audioUrl = bookDownloader.getLocalFileUri(podcast.id),
                            description = "İndirilmiş Podcast",
                            duration = 0L
                        )
                    )
                }
            }
            mutableStateOf(list.distinctBy { it.id })
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("İndirilenler", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface) },
                    navigationIcon = {
                        IconButton(
                            onClick = { navigator.pop() },
                            modifier = Modifier.background(MaterialTheme.colorScheme.onSurface.copy(0.05f), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Background mesh effects
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.size(300.dp).offset(x = 100.dp, y = (-50).dp).background(Color(0xFF3B82F6).copy(alpha = 0.03f), CircleShape).blur(80.dp))
                }

                if (downloadedBooks.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "📥", fontSize = 80.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Henüz kitap indirmemişsin",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "İnternetsiz dinlemek için kitap kapaklarından indir butonuna basabilirsin.",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 48.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(downloadedBooks, key = { it.id }) { book ->
                            DownloadedBookCard(
                                book = book,
                                onPlay = { 
                                    // navigator.push(PlayerScreen(book))
                                },
                                onDelete = {
                                    bookDownloader.deleteBook(book.id)
                                    refreshTrigger++ // Listeyi yenile
                                },
                                BrandGold = BrandGold,
                                surfaceColor = surfaceColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadedBookCard(
    book: Book,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    BrandGold: Color,
    surfaceColor: Color
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Kitabı Sil?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black) },
            text = { Text("${book.title} cihazından silinecek. Emin misin?", color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4B4B))
                ) {
                    Text("Evet, Sil", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Vazgeç", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E222D),
            shape = RoundedCornerShape(28.dp)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = "Kapak",
                    modifier = Modifier
                        .width(70.dp)
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(0.05f)),
                    contentScale = ContentScale.Crop
                )
                
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
                        .background(Color.Black.copy(0.4f), CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PlayArrow, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Surface(
                    color = BrandGold.copy(0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Çevrimdışı Hazır",
                        color = BrandGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.background(MaterialTheme.colorScheme.onSurface.copy(0.03f), CircleShape)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Sil", tint = Color(0xFFFF4B4B).copy(alpha = 0.8f))
            }
        }
    }
}
