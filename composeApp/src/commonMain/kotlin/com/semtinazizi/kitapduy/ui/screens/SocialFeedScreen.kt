package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.ui.components.FeedCard
import com.semtinazizi.kitapduy.ui.components.SmartImage
import com.semtinazizi.kitapduy.ui.viewmodel.FeedScreenModel
import com.semtinazizi.kitapduy.ui.viewmodel.AuthViewModel
import org.koin.compose.viewmodel.koinViewModel
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.delay

class SocialFeedScreen(val openComposer: Boolean = false) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<FeedScreenModel>()
        LaunchedEffect(Unit) { viewModel.loadFeed(true) }

        val generalItems by viewModel.generalFeedItems.collectAsState()
        val listeningItems by viewModel.listeningFeedItems.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val actionError by viewModel.actionError.collectAsState()
        actionError?.let { message ->
            AlertDialog(onDismissRequest = { viewModel.actionError.value = null }, text = { Text(message) },
                confirmButton = { TextButton(onClick = { viewModel.actionError.value = null }) { Text("Tamam") } })
        }

        var selectedTabIndex by remember { mutableStateOf(0) }
        val displayItems = if (selectedTabIndex == 0) generalItems else listeningItems
        val generalListState = rememberLazyListState()
        val listeningListState = rememberLazyListState()
        val isLoadingMore by viewModel.isLoadingMore.collectAsState()

        val authViewModel = koinViewModel<AuthViewModel>()
        val userProfile by authViewModel.userProfile.collectAsState()

        val isAdmin = userProfile?.badges?.contains("admin") == true
        val isPremium = userProfile?.hasPremium() == true
        val BrandGold = Color(0xFFF9E4B7)
        val BrandAccent = Color(0xFFD4AF37)

        // Post Dialog State
        var showPostDialog by remember { mutableStateOf(openComposer) }
        var selectedPostTab by remember { mutableStateOf(0) }
        var postText by remember { mutableStateOf("") }
        var pollQuestion by remember { mutableStateOf("") }
        val pollOptions = remember { mutableStateListOf("", "") }
        var selectedImageFile by remember { mutableStateOf<PlatformFile?>(null) }
        var selectedImageName by remember { mutableStateOf("") }

        val launcher = rememberFilePickerLauncher(
            type = PickerType.Image,
            mode = PickerMode.Single,
            title = "Fotoğraf Seç"
        ) { file ->
            if (file != null) {
                selectedImageFile = file
                selectedImageName = file.name
            }
        }

        // FAB State
        var fabExpanded by remember { mutableStateOf(false) }
        val fabRotation by animateFloatAsState(
            targetValue = if (fabExpanded) 45f else 0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "fabRotation"
        )
        val fabWidth by animateDpAsState(
            targetValue = if (fabExpanded) 56.dp else 120.dp,
            label = "fabWidth"
        )

        if (showPostDialog) {
            AlertDialog(
                onDismissRequest = { showPostDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(28.dp),
                title = { Text("Duygularını Paylaş 🖋️", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        TabRow(
                            selectedTabIndex = selectedPostTab,
                            containerColor = Color.Transparent,
                            contentColor = BrandGold,
                            divider = {}
                        ) {
                            Tab(selected = selectedPostTab == 0, onClick = { selectedPostTab = 0; postText = "" }, text = { Text("Yazı") })
                            Tab(selected = selectedPostTab == 1, onClick = { selectedPostTab = 1 }, text = { Text("Anket") })
                            Tab(selected = selectedPostTab == 2, onClick = { selectedPostTab = 2 }, text = { Text("Resim") })
                            if (isAdmin) {
                                Tab(selected = selectedPostTab == 3, onClick = { selectedPostTab = 3; postText = "" }, text = { Text("Mısra") })
                            }
                        }
                        Spacer(Modifier.height(24.dp))

                        if (selectedPostTab == 0 || selectedPostTab == 3) {
                            OutlinedTextField(
                                value = postText,
                                onValueChange = { if (it.length <= 280) postText = it },
                                placeholder = { Text(if (selectedPostTab == 3) "Mısralarını buraya fısılda..." else "Düşüncelerin...", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth().height(150.dp).testTag("feed-post-text"),
                                maxLines = 10,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedBorderColor = BrandGold,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                )
                            )
                        } else if (selectedPostTab == 1) {
                            OutlinedTextField(
                                value = pollQuestion,
                                onValueChange = { if (it.length <= 100) pollQuestion = it },
                                label = { Text("Tartışma Konusu") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            pollOptions.forEachIndexed { index, option ->
                                OutlinedTextField(
                                    value = option,
                                    onValueChange = { pollOptions[index] = it },
                                    label = { Text("Seçenek ${index + 1}") },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                            if (pollOptions.size < 4) {
                                TextButton(onClick = { pollOptions.add("") }) {
                                    Text("+ Yeni Seçenek", color = BrandGold)
                                }
                            }
                        } else if (selectedPostTab == 2) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(0.05f))
                                    .clickable { launcher.launch() },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedImageFile != null) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(48.dp))
                                        Text(selectedImageName, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                                    }
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.AddPhotoAlternate, null, modifier = Modifier.size(48.dp), tint = BrandGold)
                                        Text("Fotoğraf Seç", color = Color.Gray)
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            OutlinedTextField(
                                value = postText,
                                onValueChange = { postText = it },
                                label = { Text("Açıklama Ekle") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (selectedPostTab == 0 && postText.isNotBlank()) {
                                viewModel.shareUserPost(postText, userProfile)
                                showPostDialog = false
                            } else if (selectedPostTab == 1 && pollQuestion.isNotBlank()) {
                                viewModel.sharePoll(pollQuestion, pollOptions.filter { it.isNotBlank() }, userProfile)
                                showPostDialog = false
                            } else if (selectedPostTab == 2 && selectedImageFile != null) {
                                viewModel.shareImagePost(postText, selectedImageFile!!, userProfile)
                                showPostDialog = false
                            } else if (selectedPostTab == 3 && postText.isNotBlank()) {
                                viewModel.sharePoetryPost(postText, userProfile)
                                showPostDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGold),
                        modifier = Modifier.testTag("feed-post-submit"),
                        shape = RoundedCornerShape(50)
                    ) { Text("Paylaş", color = Color.Black, fontWeight = FontWeight.Black) }
                },
                dismissButton = {
                    TextButton(onClick = { showPostDialog = false }) { Text("İptal", color = Color.Gray) }
                }
            )
        }

        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Ambient Aura
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 50.dp, y = (-50).dp)
                    .size(250.dp)
                    .background(Color(0xFFF9E4B7).copy(alpha = 0.15f), CircleShape)
                    .blur(80.dp)
            )

            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Edebiyat Duvarı",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Kitapseverlerin buluşma noktası",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                .clickable { viewModel.loadFeed(true) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Refresh, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                .clickable { navigator.push(NotificationScreen()) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Notifications, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                // Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("Mısralar", "Etkinlik").forEachIndexed { index, label ->
                        val isSelected = selectedTabIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) MaterialTheme.colorScheme.background else Color.Transparent)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { selectedTabIndex = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Feed Content
                AnimatedContent(
                    targetState = selectedTabIndex,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { width -> width }.plus(fadeIn()).togetherWith(slideOutHorizontally { width -> -width }.plus(fadeOut()))
                        } else {
                            slideInHorizontally { width -> -width }.plus(fadeIn()).togetherWith(slideOutHorizontally { width -> width }.plus(fadeOut()))
                        }.using(SizeTransform(clip = false))
                    },
                    label = "tab_transition"
                ) { targetIndex ->
                    val currentListState = if (targetIndex == 0) generalListState else listeningListState
                    val shouldLoadMore = remember(currentListState) {
                        derivedStateOf {
                            val total = currentListState.layoutInfo.totalItemsCount
                            val lastVisible = currentListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            total > 0 && lastVisible >= total - 3
                        }
                    }
                    LaunchedEffect(shouldLoadMore.value) {
                        if (shouldLoadMore.value) {
                            viewModel.loadMore(isListeningTab = targetIndex == 1)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp, start = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        state = currentListState
                    ) {
                        // "Şu An Dinleyenler" Header
                        if (targetIndex == 0 && listeningItems.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(modifier = Modifier.size(8.dp).background(Brush.linearGradient(listOf(BrandGold, BrandAccent)), CircleShape))
                                        Text(
                                            text = "Şu An Dinleyenler",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = "${listeningItems.size} okur",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(items = listeningItems, key = { "${it.id}_${listeningItems.indexOf(it)}" }) { item ->
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.width(70.dp).clickable { navigator.push(UserProfileScreen(item.userId)) }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    // ListeningRipple Equivalent
                                                    Box(
                                                        modifier = Modifier
                                                            .size(68.dp)
                                                            .clip(CircleShape)
                                                            .background(BrandGold.copy(0.2f))
                                                    )
                                                    SmartImage(
                                                        imageUrl = item.userPhotoUrl,
                                                        modifier = Modifier
                                                            .size(64.dp)
                                                            .border(2.dp, Brush.linearGradient(listOf(BrandGold, BrandAccent)), CircleShape)
                                                            .padding(3.dp)
                                                            .clip(CircleShape),
                                                        fallbackText = item.userName,
                                                        isAvatar = true
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.BottomEnd)
                                                            .size(22.dp)
                                                            .background(Brush.linearGradient(listOf(BrandGold, BrandAccent)), CircleShape)
                                                            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Filled.Headset, null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                                    }
                                                }
                                                Spacer(Modifier.height(8.dp))
                                                Text(
                                                    text = item.userName.split(" ").firstOrNull() ?: "Okur",
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                        }

                        if (isLoading) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(top = 60.dp), Alignment.Center) {
                                    CircularProgressIndicator(color = BrandGold)
                                }
                            }
                        } else if (displayItems.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(top = 60.dp), Alignment.Center) {
                                    Text(
                                        text = if (targetIndex == 0) "Henüz bir mısra fısıldanmadı." else "Henüz bir dinleme aktivitesi yok.",
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        } else {
                            items(displayItems.size, key = { "${displayItems[it].id}_$it" }) { index ->
                                val item = displayItems[index]
                                com.semtinazizi.kitapduy.ui.components.InteractiveFeedCard(
                                    item = item, onChanged = { viewModel.loadFeed(true) })
                            }
                            if (isLoadingMore) {
                                item {
                                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BrandGold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // FAB Layer
            if (fabExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { fabExpanded = false }
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 96.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedVisibility(
                    visible = fabExpanded,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 }
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        data class FabOption(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val tab: Int)
                        val options = buildList {
                            if (isAdmin) add(FabOption("Mısra", Icons.Filled.AutoAwesome, 3))
                            add(FabOption("Fotoğraf", Icons.Filled.AddPhotoAlternate, 2))
                            add(FabOption("Anket", Icons.Filled.Poll, 1))
                            add(FabOption("Yazı", Icons.Filled.Edit, 0))
                        }
                        options.forEach { opt ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.testTag("feed-compose-${opt.tab}").clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    selectedPostTab = opt.tab
                                    postText = ""
                                    pollQuestion = ""
                                    pollOptions.clear()
                                    pollOptions.addAll(listOf("", ""))
                                    selectedImageFile = null
                                    fabExpanded = false
                                    showPostDialog = true
                                }
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    shadowElevation = 4.dp
                                ) {
                                    Text(
                                        text = opt.label,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Brush.linearGradient(listOf(BrandGold, BrandAccent)), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(opt.icon, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .height(56.dp)
                        .width(fabWidth).testTag("feed-compose")
                        .shadow(8.dp, RoundedCornerShape(28.dp))
                        .background(Brush.linearGradient(listOf(BrandGold, BrandAccent)), RoundedCornerShape(28.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { fabExpanded = !fabExpanded },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Paylaş",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp).rotate(fabRotation)
                        )
                        androidx.compose.animation.AnimatedVisibility(visible = !fabExpanded) {
                            Row {
                                Spacer(Modifier.width(8.dp))
                                Text("Paylaş", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
