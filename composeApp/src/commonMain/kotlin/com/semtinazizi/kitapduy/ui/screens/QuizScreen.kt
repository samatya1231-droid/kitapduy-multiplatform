package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.ui.viewmodel.QuizGameState
import com.semtinazizi.kitapduy.ui.viewmodel.QuizScreenModel
import cafe.adriel.voyager.koin.koinScreenModel
import org.koin.compose.koinInject
import com.semtinazizi.kitapduy.platform.AdManager
import com.semtinazizi.kitapduy.platform.BillingManager
import com.semtinazizi.kitapduy.platform.BannerAdView

import kotlin.random.Random

class QuizScreen : Screen {
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<QuizScreenModel>()
        val adManager: AdManager = koinInject()
        val billingManager: BillingManager = koinInject()
        val isPremium by billingManager.isPremium.collectAsState()
        
        val gameState by screenModel.gameState.collectAsState()
        
        Box(modifier = Modifier.fillMaxSize()) {
            ModernArenaBackground()
            
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).statusBarsPadding()) {
                    IconButton(
                        onClick = { 
                            if (gameState != QuizGameState.STARTING) {
                                // Geri dönünce ana menüye geç (bunun için ViewModel'a reset fonksiyonu eklenebilir, 
                                // şimdilik doğrudan navigator'dan çıkıyoruz)
                                navigator.pop()
                            } else {
                                navigator.pop() 
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                when (gameState) {
                    QuizGameState.STARTING -> {
                        QuizMainMenu(
                            onStartSinglePlayer = { screenModel.startGame() },
                            onStartMultiplayer = { screenModel.findMultiplayerMatch() },
                            onLeaderboard = { screenModel.loadLeaderboard() }
                        )
                    }
                    QuizGameState.PLAYING -> {
                        QuizPlayingArea(screenModel = screenModel, adManager = adManager, isPremium = isPremium)
                    }
                    QuizGameState.MULTIPLAYER_LOBBY -> {
                        QuizMultiplayerLobby(screenModel = screenModel, onBackClick = { screenModel.resetGame() })
                    }
                    QuizGameState.LEADERBOARD -> {
                        QuizLeaderboardScreen(screenModel = screenModel, onBackClick = { screenModel.resetGame() })
                    }
                    QuizGameState.GAME_OVER -> {
                        QuizResultScreen(
                            title = "💔 Canın Bitti!",
                            score = screenModel.score.value,
                            onRetry = { screenModel.startGame() },
                            onBack = { navigator.pop() }
                        )
                    }
                    QuizGameState.SUCCESS -> {
                        QuizResultScreen(
                            title = "🏆 Mükemmel!",
                            score = screenModel.score.value,
                            onRetry = { screenModel.startGame() },
                            onBack = { navigator.pop() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuizMainMenu(onStartSinglePlayer: () -> Unit, onStartMultiplayer: () -> Unit, onLeaderboard: () -> Unit) {
    var showComingSoon by remember { mutableStateOf(false) }
    
    if (showComingSoon) {
        AlertDialog(
            onDismissRequest = { showComingSoon = false },
            title = { Text("Çok Yakında!", fontWeight = FontWeight.Bold) },
            text = { Text("Bu oyun modu şu anda geliştirme aşamasında. Çok yakında sizlerle olacak!") },
            confirmButton = {
                TextButton(onClick = { showComingSoon = false }) {
                    Text("Tamam")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(24.dp))
        Text("📚 KitapDuy Arena", color = MaterialTheme.colorScheme.onSurface, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Text("Edebiyat bilgini sına, ödülleri kazan!", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 16.sp, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(32.dp))

        // Haftalık Turnuva
        GlassCard(
            modifier = Modifier.fillMaxWidth().height(90.dp).testTag("quiz-tournament-button"),
            onClick = { showComingSoon = true }
        ) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("🏆 Haftalık Turnuva", color = Color(0xFFD4AF37), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Dünya Klasikleri • 1 Can", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Tek Oyunculu Mod
        GlassCard(
            modifier = Modifier.fillMaxWidth().height(90.dp).testTag("quiz-start-classic"),
            onClick = onStartSinglePlayer
        ) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Klasik Mod", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Antrenman Yap • 3 Can", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Blitz Arena
        GlassCard(
            modifier = Modifier.fillMaxWidth().height(90.dp).testTag("quiz-start-blitz"),
            onClick = onStartMultiplayer
        ) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("⚡ Blitz Arena", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Hızlı Cevaplar • 1 Can", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Çok Oyunculu Mod
        GlassCard(
            modifier = Modifier.fillMaxWidth().height(90.dp).testTag("quiz-duel-button"),
            onClick = { showComingSoon = true }
        ) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.People, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Çok Oyunculu Düello", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Yakında Çok Daha Güçlü!", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 12.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Liderlik Tablosu
        GlassCard(
            modifier = Modifier.fillMaxWidth().height(80.dp).testTag("quiz-leaderboard-button"),
            onClick = onLeaderboard
        ) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text("👑 Liderlik Tablosu", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun QuizPlayingArea(screenModel: QuizScreenModel, adManager: AdManager, isPremium: Boolean) {
    val currentQuestion by screenModel.currentQuestion.collectAsState()
    val score by screenModel.score.collectAsState()
    LaunchedEffect(Unit) { if (score >= 70) adManager.grantAdFreeTime() }
    val lives by screenModel.lives.collectAsState()
    val timeLeft by screenModel.timeLeft.collectAsState()
    val selectedAnswer by screenModel.selectedAnswer.collectAsState()
    val isCorrect by screenModel.isCorrect.collectAsState()

    val question = currentQuestion ?: return

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)
    ) {
        // Üst Bar: Can, Skor ve Zaman
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("❤️ $lives", color = Color.Red, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("⏳ $timeLeft sn", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("🏆 $score", color = Color(0xFFD4AF37), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Soru Kartı
        Card(
            modifier = Modifier.fillMaxWidth().testTag("quiz-question-text").semantics(mergeDescendants = true) {},
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Text(
                text = currentQuestion?.question ?: "",
                modifier = Modifier.padding(24.dp),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        
            // Emojiler
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("💯", "🔥", "🤔", "😱").forEach { emoji ->
                    Text(emoji, fontSize = 32.sp, modifier = Modifier.clickable { screenModel.sendEmoji(emoji) })
                }
            }

            // Şıklar
        (currentQuestion?.options ?: emptyList<String>()).forEachIndexed { index, option ->
            val isSelected = selectedAnswer == index
            val showCorrect = selectedAnswer != null && index == currentQuestion?.correctIndex
            val showWrong = isSelected && isCorrect == false

            val bgColor = when {
                showCorrect -> Color(0xFF4CAF50).copy(alpha = 0.8f) // Yeşil
                showWrong -> Color(0xFFFF4B4B).copy(alpha = 0.8f) // Kırmızı
                else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            }

            val contentColor = when {
                showCorrect || showWrong -> Color.White
                else -> MaterialTheme.colorScheme.onSurface
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("quiz-option-$index")
                    .clickable(enabled = selectedAnswer == null) {
                        screenModel.submitAnswer(index)
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = bgColor)
            ) {
                Text(
                    text = option,
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun QuizResultScreen(title: String, score: Int, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Puanın: $score", fontSize = 24.sp, color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
        ) {
            Text("Tekrar Oyna", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = onBack) {
            Text("Ana Menüye Dön", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), Color.Transparent))),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun ModernArenaBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val isLight = !MaterialTheme.colorScheme.background.copy(alpha = 1f).luminance().isNaN() && MaterialTheme.colorScheme.background.luminance() > 0.5f
    
    val xOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Reverse),
        label = "xOffset"
    )

    val bgColor = MaterialTheme.colorScheme.background
    val particleColor = if (isLight) Color(0xFFD4AF37).copy(alpha = 0.07f) else Color(0xFFD4AF37).copy(alpha = 0.10f)
    val gradientMid = if (isLight) Color(0xFFF5F0E8) else Color(0xFF1A1A2E)

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            Brush.verticalGradient(
                listOf(bgColor, gradientMid, bgColor)
            )
        )
        
        // Rastgele parlayan noktalar (Partiküller)
        for (i in 0..15) {
            val x = (i * 137 + xOffset) % size.width
            val y = (i * 243) % size.height
            drawCircle(
                color = particleColor,
                radius = 100f,
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
        }
    }
}


fun getRankInfo(score: Long): Pair<String, String> {
    return when {
        score >= 20000 -> "Edebiyat Profesörü (Efsanevi)" to "🌌"
        score >= 10000 -> "Elmas Okur" to "💎"
        score >= 5001 -> "Platin Şövalye" to "⚔️"
        score >= 2001 -> "Altın Kitap Kurdu" to "🥇"
        score >= 1001 -> "Gümüş Okur" to "🥈"
        score >= 501 -> "Bronz Okur" to "🥉"
        else -> "Yeni Başlayan" to "🔰"
    }
}


@Composable
fun QuizLeaderboardScreen(screenModel: QuizScreenModel, onBackClick: () -> Unit) {
    val leaderboard by screenModel.leaderboard.collectAsState()

    LaunchedEffect(Unit) {
        screenModel.loadLeaderboard()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text("👑 Liderlik Tablosu", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (leaderboard.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(leaderboard) { index, entry ->
                    val rankInfo = getRankInfo(entry.totalScore)
                    val bgColor = when (index) {
                        0 -> Color(0xFFFFD700).copy(alpha = 0.2f)
                        1 -> Color(0xFFC0C0C0).copy(alpha = 0.2f)
                        2 -> Color(0xFFCD7F32).copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = bgColor)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}.", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(36.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.userName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("${rankInfo.second} ${rankInfo.first}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            }
                            
                            Text("${entry.totalScore} Puan", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD4AF37))
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun QuizMultiplayerLobby(screenModel: QuizScreenModel, onBackClick: () -> Unit) {
    val lobbyState by screenModel.lobbyState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.People, contentDescription = "Multiplayer", modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Çok Oyunculu Arena", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(lobbyState, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), textAlign = TextAlign.Center)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (lobbyState.contains("Aranıyor") || lobbyState.contains("Bağlanıyor")) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onBackClick, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
            Text("İptal Et")
        }
    }
}
