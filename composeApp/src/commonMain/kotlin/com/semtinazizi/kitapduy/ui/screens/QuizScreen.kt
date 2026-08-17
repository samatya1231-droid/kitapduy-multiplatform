package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import kotlin.random.Random

class QuizScreen : Screen {
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<QuizScreenModel>()
        
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
                        QuizMainMenu(onStartSinglePlayer = { screenModel.startGame() })
                    }
                    QuizGameState.PLAYING -> {
                        QuizPlayingArea(screenModel = screenModel)
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
fun QuizMainMenu(onStartSinglePlayer: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text("📚 KitapDuy Arena", color = MaterialTheme.colorScheme.onSurface, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Text("Edebiyat bilgini sına, ödülleri kazan!", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 16.sp, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(32.dp))

        // Tek Oyunculu Mod
        GlassCard(
            modifier = Modifier.fillMaxWidth().height(90.dp),
            onClick = onStartSinglePlayer
        ) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Tek Oyunculu", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Antrenman Yap • 3 Can", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Çok Oyunculu Mod (Yakında)
        GlassCard(
            modifier = Modifier.fillMaxWidth().height(90.dp),
            onClick = { /* TODO: Multiplayer Logic */ }
        ) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.People, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Çok Oyunculu", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Yakında Çok Daha Güçlü!", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun QuizPlayingArea(screenModel: QuizScreenModel) {
    val currentQuestion by screenModel.currentQuestion.collectAsState()
    val score by screenModel.score.collectAsState()
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
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Text(
                text = question.question,
                modifier = Modifier.padding(24.dp),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Şıklar
        question.options.forEachIndexed { index, option ->
            val isSelected = selectedAnswer == index
            val showCorrect = selectedAnswer != null && index == question.correctIndex
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
