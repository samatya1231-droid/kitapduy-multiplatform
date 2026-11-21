@file:OptIn(ExperimentalMaterial3Api::class)

package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.ui.platform.testTag

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.model.AskidaKitap
import com.semtinazizi.kitapduy.ui.components.SmartImage
import com.semtinazizi.kitapduy.ui.viewmodel.AskidaViewModel
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.launch

class AskidaKitapScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<AskidaViewModel>()
        val focusManager = LocalFocusManager.current
        
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = listOf("Askıdakiler", "Bağışla", "Sohbetler")
        val BrandGold = Color(0xFFFFD700)

        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // 🌟 DİNAMİK ARKA PLAN 🌟
            val infiniteTransition = rememberInfiniteTransition(label = "askida_bg")
            val blobOffset by infiniteTransition.animateValue(
                initialValue = IntOffset(-100, 200),
                targetValue = IntOffset(100, -200),
                typeConverter = IntOffset.VectorConverter,
                animationSpec = infiniteRepeatable(animation = tween(25000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
                label = "blob"
            )
            Box(modifier = Modifier.offset(blobOffset.x.dp, blobOffset.y.dp).size(450.dp).background(Color(0xFF10B981).copy(alpha = 0.04f), CircleShape).blur(100.dp))

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "İyilik Köprüsü",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-1).sp
                        )
                        Text("Kitapları askıya bırak, paylaş", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // 🌟 MODERN SEKMELER 🌟
                Surface(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                    modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val isSelected = selectedTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) BrandGold else Color.Transparent)
                                    .clickable {
                                        focusManager.clearFocus()
                                        selectedTab = index
                                    }
                                    .testTag("askida-tab-$index"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) Color.Black else Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            (fadeIn() + scaleIn(initialScale = 0.95f)).togetherWith(fadeOut())
                        },
                        label = "tab_content"
                    ) { target ->
                        when (target) {
                            0 -> AskidaKitaplarList(
                                viewModel = viewModel,
                                onClaimSuccess = { selectedTab = 2 },
                                onNavigateToChat = { chatId -> navigator.push(AskidaSohbetScreen(chatId)) }
                            )
                            1 -> AskidaKitapBagis(
                                viewModel = viewModel,
                                onSuccess = { selectedTab = 0 }
                            )
                            2 -> AskidaSohbetlerList(
                                viewModel = viewModel,
                                onChatClick = { chatId -> navigator.push(AskidaSohbetScreen(chatId)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AskidaSohbetlerList(viewModel: AskidaViewModel, onChatClick: (String) -> Unit) {
    val myChats by viewModel.myChats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    // We can't directly use FirebaseAuth here since it's commonMain.
    // Instead we rely on the logic that isMyDonation can be checked by donorName for now or just generic texts.
    // Let's assume we don't have currentUid easily accessible without authRepository in Composable.
    // For now we'll just show the chats.

    LaunchedEffect(Unit) { viewModel.loadMyChats() }

    if (isLoading && myChats.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFFFD700))
        }
    } else if (myChats.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.ChatBubbleOutline, null, tint = Color.Gray.copy(0.2f), modifier = Modifier.size(80.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Henüz bir sohbetin yok.", color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(myChats, key = { it.id }) { chat ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onChatClick(chat.id) },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.onSurface.copy(0.05f))) {
                            if (chat.imageUrl.isNotEmpty()) {
                                SmartImage(imageUrl = chat.imageUrl, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = Color.Gray, modifier = Modifier.align(Alignment.Center))
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(chat.bookName, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = "Kitap Sohbeti ✨",
                                color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AskidaKitaplarList(viewModel: AskidaViewModel, onClaimSuccess: () -> Unit, onNavigateToChat: (String) -> Unit) {
    val books by viewModel.availableBooks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Hepsi") }
    val categories = listOf("Hepsi", "Roman", "Eğitim", "Akademik", "Çocuk", "Dini", "Diğer")

    val focusManager = LocalFocusManager.current
    var claimingBookId by remember { mutableStateOf<String?>(null) }
    var showClaimConfirmation by remember { mutableStateOf<AskidaKitap?>(null) }

    val filteredBooks = books.filter {
        (selectedCategory == "Hepsi" || it.category == selectedCategory) &&
        (searchQuery.isBlank() || it.bookName.contains(searchQuery, ignoreCase = true) || it.city.contains(searchQuery, ignoreCase = true))
    }

    LaunchedEffect(Unit) { viewModel.loadAskidaBooks() }

    Column(modifier = Modifier.fillMaxSize()) {
        // 🔍 ARAMA VE FİLTRELEME ÇUBUĞU
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().testTag("askida-search"),
                placeholder = { Text("Kitap veya şehir ara...", color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFFD700),
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(0.1f)
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { focusManager.clearFocus() },
                    onDone = { focusManager.clearFocus() }
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories, key = { it }) { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        modifier = Modifier.clickable {
                            focusManager.clearFocus()
                            selectedCategory = cat
                        },
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(0.05f),
                        border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.1f)) else null
                    ) {
                        Text(
                            text = cat,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            color = if (isSelected) Color.Black else Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (isLoading && filteredBooks.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFFD700))
            }
        } else if (filteredBooks.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = Color.Gray.copy(0.2f), modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Sonuç bulunamadı.", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 160.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredBooks, key = { it.id }) { book ->
                    // In KMP we don't have direct access to currentUid synchronously here, so we assume all books in this list are available for claiming unless we handle "my books" specifically.
                    AskidaKitapCard(
                        book = book,
                        isClaiming = claimingBookId == book.id,
                        onClaim = {
                            showClaimConfirmation = book
                        }
                    )
                }
            }
        }
    }

    // 🌟 KİTAP TALEP ONAY DİYALOGU 🌟
    showClaimConfirmation?.let { book ->
        AlertDialog(
            onDismissRequest = { showClaimConfirmation = null },
            icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Color(0xFFFFD700)) },
            title = { Text("Kitabı Talep Et", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface) },
            text = { 
                Text(
                    "\"${book.bookName}\" kitabını talep etmek üzeresiniz. Onayladığınızda bağışçı ile bir sohbet başlatılacaktır.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetBook = showClaimConfirmation
                        showClaimConfirmation = null
                        targetBook?.let {
                            claimingBookId = it.id
                            viewModel.claimBook(it)
                            // In a real scenario we'd wait for callback, but we optimistic UI update so we can just navigate or show success
                            claimingBookId = null
                            onClaimSuccess()
                            onNavigateToChat(it.id)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                ) {
                    Text("Evet, İstiyorum", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClaimConfirmation = null }) {
                    Text("Vazgeç", color = Color.Gray)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun AskidaKitapCard(book: AskidaKitap, isClaiming: Boolean, onClaim: () -> Unit) {
    val isClaimed = book.status != "AVAILABLE"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(100.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.onSurface.copy(0.05f))
                ) {
                    if (book.imageUrl.isNotEmpty()) {
                        SmartImage(
                            imageUrl = book.imageUrl,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Color.Gray, modifier = Modifier.align(Alignment.Center))
                    }
                }
                
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(text = book.bookName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color(0xFFFFD700).copy(0.1f), shape = RoundedCornerShape(4.dp)) {
                            Text(text = book.category.ifBlank { "Diğer" }, color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(text = "📍 ${book.city}", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Bağışlayan: ${book.donorName}", color = Color.Gray, fontSize = 12.sp)
                    // TODO: Text(text = "📅 ${formatDate(book.timestamp)}", color = Color.Gray.copy(0.6f), fontSize = 10.sp)
                }
            }

            Row(modifier = Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Kondisyon:", color = Color.Gray, fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                val conditionColor = when(book.condition) {
                    "Yeni Gibi" -> Color(0xFF4CAF50)
                    "İyi" -> Color(0xFF2196F3)
                    else -> Color(0xFFFF9800)
                }
                Text(book.condition, color = conditionColor, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }

            if (book.donorNote.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(0.03f))
                        .padding(16.dp)
                ) {
                    Text(text = "\"${book.donorNote}\"", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onClaim,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isClaimed && !isClaiming,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), disabledContainerColor = Color.DarkGray)
            ) {
                if (isClaiming) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                } else {
                    val buttonText = if (isClaimed) "Alındı" else "Kitabı Talep Et"
                    Text(text = buttonText, color = Color.Black, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun AskidaKitapBagis(viewModel: AskidaViewModel, onSuccess: () -> Unit) {
    var bookName by remember { mutableStateOf("") }
    var donorNote by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Roman") }
    var selectedCondition by remember { mutableStateOf("İyi") }
    
    // KMP File Picker State
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedImageExtension by remember { mutableStateOf<String?>(null) }
    var selectedImageName by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    
    val categories = listOf("Roman", "Eğitim", "Akademik", "Çocuk", "Dini", "Diğer")
    val conditions = listOf("Yeni Gibi", "İyi", "Yıpranmış")
    
    val isUploading by viewModel.isLoading.collectAsState()

    val filePicker = rememberFilePickerLauncher(
        type = PickerType.Image,
        title = "Kitap Fotoğrafı Seç"
    ) { file: PlatformFile? ->
        if (file != null) {
            selectedImageName = file.name
            selectedImageExtension = file.name.substringAfterLast('.', "")
            coroutineScope.launch {
                selectedImageBytes = file.readBytes()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() }
            .padding(24.dp)
    ) {
        Text("Bir İyilik Bırak", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
        Text("Kitabın yeni bir hayat bulsun.", color = Color.Gray, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(0.03f))
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.05f)), RoundedCornerShape(24.dp))
                .clickable { filePicker.launch() },
            contentAlignment = Alignment.Center
        ) {
            if (selectedImageBytes != null) {
                // We show a generic checkmark or file name since AsyncImage with bytearray requires specific Coil setup
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(selectedImageName ?: "Fotoğraf Seçildi", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Değiştirmek için dokun", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddPhotoAlternate, null, tint = Color(0xFFFFD700), modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Kitap Fotoğrafı Ekle", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = bookName, onValueChange = { bookName = it },
            label = { Text("Kitap Adı") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth().testTag("askida-donate-title"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFFD700), unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(0.1f))
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = city, onValueChange = { city = it },
            label = { Text("Şehir") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth().testTag("askida-donate-city"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFFD700), unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(0.1f)),
            placeholder = { Text("Örn: İstanbul", color = Color.Gray) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Kategori", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        LazyRow(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories, key = { it }) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFD700),
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Kondisyon", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Row(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            conditions.forEach { cond ->
                FilterChip(
                    selected = selectedCondition == cond,
                    onClick = { selectedCondition = cond },
                    label = { Text(cond) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFD700),
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = donorNote, onValueChange = { donorNote = it },
            label = { Text("Kısa Bir Not") },
            modifier = Modifier.fillMaxWidth().height(120.dp).testTag("askida-donate-note"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFFD700), unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(0.1f))
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                focusManager.clearFocus()
                if (bookName.isBlank()) return@Button
                if (city.isBlank()) return@Button
                viewModel.donateBook(bookName, city, selectedCondition, donorNote, selectedCategory, selectedImageBytes, selectedImageExtension) { success -> 
                    if (success) onSuccess() 
                }
            },
            modifier = Modifier.fillMaxWidth().height(60.dp).testTag("askida-donate-submit"),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
            enabled = !isUploading && bookName.isNotBlank() && city.isNotBlank()
        ) {
            if (isUploading) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
            else Text("Askıya Bırak", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(140.dp))
    }
}
