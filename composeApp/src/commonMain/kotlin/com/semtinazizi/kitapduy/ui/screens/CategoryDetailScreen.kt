package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.util.root
import com.semtinazizi.kitapduy.model.Book
import com.semtinazizi.kitapduy.ui.theme.PremiumAccent
import com.semtinazizi.kitapduy.ui.theme.PlayfairDisplayFamily
import com.semtinazizi.kitapduy.ui.components.PremiumUpgradeCard
import com.semtinazizi.kitapduy.ui.components.CategoryHeroHeader
import com.semtinazizi.kitapduy.ui.components.FeaturedBookCard
import com.semtinazizi.kitapduy.ui.components.SpotifySectionHeader
import com.semtinazizi.kitapduy.ui.components.SpotifySquareCard
import com.semtinazizi.kitapduy.ui.components.PremiumTrialBanner
import com.semtinazizi.kitapduy.ui.components.VerticalBookCard
import com.semtinazizi.kitapduy.ui.viewmodel.HomeViewModel
import com.semtinazizi.kitapduy.ui.viewmodel.AuthViewModel
import com.semtinazizi.kitapduy.platform.BannerAdView
import org.koin.compose.viewmodel.koinViewModel

class CategoryDetailScreen(val categoryName: String) : Screen {
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val homeViewModel = koinViewModel<HomeViewModel>()
        val authViewModel = koinViewModel<AuthViewModel>()
        
        val userProfile by authViewModel.userProfile.collectAsState()
        val isPremium = userProfile?.isPremium == true
        val allBooks by homeViewModel.books.collectAsState()
        
        var searchQuery by remember { mutableStateOf("") }
        
        val filteredBooks = remember(allBooks, categoryName, searchQuery) {
            allBooks.filter { categoryName == "Tümü" || it.category == categoryName }
                .filter { 
                    it.title.contains(searchQuery, ignoreCase = true) || 
                    it.author.contains(searchQuery, ignoreCase = true) 
                }
        }

        val booksByAuthor = remember(filteredBooks) {
            filteredBooks.groupBy { it.author }
        }

        val categoryIcon = remember(categoryName) {
            when (categoryName) {
                "Din" -> "🕌"
                "Masal", "Çocuk Masalları" -> "🦄"
                "Polisiye" -> "🕵️‍♂️"
                "Dünya Edebiyatı" -> "🌍"
                "Şiir" -> "🌹"
                "Türk Edebiyatı" -> "🇹🇷"
                "Roman" -> "📖"
                "Klasik" -> "🏛️"
                "Bilim" -> "🧪"
                "Felsefe" -> "🧠"
                "Tarih" -> "📜"
                else -> "📚"
            }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 16.dp)
                                .height(50.dp),
                            placeholder = { Text("Yazar veya kitap ara...", color = Color.Gray, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color.Gray.copy(alpha = 0.6f), modifier = Modifier.size(18.dp)) },
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = PremiumAccent.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                if (!isPremium) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth().height(60.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            BannerAdView()
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                // Background Aura
                Box(
                    modifier = Modifier
                        .offset(x = 100.dp, y = (-50).dp)
                        .size(300.dp)
                        .background(PremiumAccent.copy(alpha = 0.1f), CircleShape)
                        .blur(100.dp)
                )

                Column(modifier = Modifier.fillMaxSize()) {
                    if (filteredBooks.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.SearchOff,
                                    contentDescription = null,
                                    tint = Color.Gray.copy(alpha = 0.3f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    if (searchQuery.isEmpty()) "Bu kategoride henüz kitap bulunamadı." else "Aramanızla eşleşen kitap bulunamadı.",
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 40.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (searchQuery.isEmpty()) {
                                // --- PREMIUM TRIAL BANNER ---
                                if (!isPremium) {
                                    item(key = "premium_trial_banner") {
                                        PremiumTrialBanner(
                                            onClick = { navigator.root.push(SubscriptionPaywallScreen()) }
                                        )
                                    }
                                }
                                
                                // --- CATEGORY HUB HEADER ---
                                item {
                                    CategoryHeroHeader(category = categoryName)
                                }
                                
                                // 2. Featured Book (Take top viewed or first)
                                val featuredBook = filteredBooks.maxByOrNull { it.viewCount } ?: filteredBooks.firstOrNull()
                                if (featuredBook != null) {
                                    item {
                                        FeaturedBookCard(
                                            book = featuredBook,
                                            index = 0,
                                            onClick = { navigator.push(BookDetailScreen(featuredBook)) }
                                        )
                                    }
                                }
                                
                                // 3. Popular Books (horizontal row, excluding featured)
                                val popularBooks = filteredBooks.filter { it.id != featuredBook?.id }.sortedByDescending { it.listenCount }
                                if (popularBooks.isNotEmpty()) {
                                    item {
                                        SpotifySectionHeader(title = "Popüler ${categoryName} Kitapları", onClick = {})
                                        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            items(popularBooks.take(10), key = { it.id }) { book ->
                                                SpotifySquareCard(
                                                    book = book,
                                                    subtitle = book.author,
                                                    onClick = { navigator.push(BookDetailScreen(book)) }
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                // Divider before authors
                                item {
                                    Text(
                                        text = "Yazarlara Göre Keşfet",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        ),
                                        modifier = Modifier.padding(horizontal = 20.dp).padding(top = 16.dp)
                                    )
                                }
                            } else {
                                item {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp)
                                    ) {
                                        Text(
                                            text = categoryIcon,
                                            fontSize = 32.sp
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = categoryName,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = PlayfairDisplayFamily
                                            )
                                            Text(
                                                text = "${filteredBooks.size} Kitap, ${booksByAuthor.size} Yazar",
                                                color = PremiumAccent,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            val authorsList = booksByAuthor.keys.toList()
                            
                            itemsIndexed(authorsList) { index, author ->
                                val authorBooks = booksByAuthor[author] ?: emptyList()
                                
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Author Header
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { navigator.push(AuthorDetailScreen(author)) }
                                            .padding(horizontal = 20.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Person,
                                                    contentDescription = null,
                                                    tint = PremiumAccent,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = author,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = PlayfairDisplayFamily
                                            )
                                        }
                                        
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Tümünü Gör",
                                                color = Color.Gray,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Icon(
                                                imageVector = Icons.Filled.ChevronRight,
                                                contentDescription = null,
                                                tint = Color.Gray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    
                                    // Books Horizontal List
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(authorBooks) { book ->
                                            Box(modifier = Modifier.width(140.dp)) {
                                                VerticalBookCard(
                                                    book = book,
                                                    onClick = { navigator.push(BookDetailScreen(book)) }
                                                )
                                            }
                                        }
                                    }
                                }

                                if (!isPremium && searchQuery.isEmpty()) {
                                    if ((index + 1) % 3 == 0) { 
                                        val adIndex = (index + 1) / 3
                                        if (adIndex % 2 != 0) {
                                            PremiumUpgradeCard(
                                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                                onClick = { navigator.root.push(SubscriptionPaywallScreen()) }
                                            )
                                        } else {
                                            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                                                BannerAdView()
                                            }
                                        }
                                    }
                                }
                            }

                            if (!isPremium) {
                                item {
                                    Spacer(Modifier.height(16.dp))
                                    Box(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
                                        BannerAdView()
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
