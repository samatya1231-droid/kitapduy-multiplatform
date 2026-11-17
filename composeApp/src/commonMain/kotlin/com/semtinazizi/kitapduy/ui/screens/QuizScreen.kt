package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.ui.viewmodel.ArenaRound
import com.semtinazizi.kitapduy.ui.viewmodel.QuizGameState
import com.semtinazizi.kitapduy.ui.viewmodel.QuizScreenModel
import cafe.adriel.voyager.koin.koinScreenModel
import com.semtinazizi.kitapduy.ui.theme.PlayfairDisplayFamily

class QuizScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<QuizScreenModel>()
        val gameState by screenModel.gameState.collectAsState()
        var showChallenges by remember { mutableStateOf(false) }

        LaunchedEffect(gameState) {
            if (gameState == QuizGameState.STARTING) screenModel.loadLeaderboard(open = false)
        }
        val title = when (gameState) {
            QuizGameState.STARTING -> if (showChallenges) "Meydan Okuma" else "KitapDuy Arena"
            QuizGameState.LEADERBOARD -> "Şampiyonlar Podyumu"
            QuizGameState.MULTIPLAYER_LOBBY -> "Düello"
            else -> screenModel.round.title
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface)
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    when {
                        gameState != QuizGameState.STARTING -> screenModel.resetGame()
                        showChallenges -> showChallenges = false
                        else -> navigator.pop()
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                }
                Text(title, style = MaterialTheme.typography.headlineSmall, fontFamily = PlayfairDisplayFamily)
            }

            when (gameState) {
                QuizGameState.STARTING -> ArenaMenu(
                    screenModel = screenModel,
                    showChallenges = showChallenges,
                    onChallenges = { showChallenges = true },
                    onAuthor = { navigator.push(GuessAuthorScreen()) }
                )
                QuizGameState.PLAYING -> QuizPlayingArea(screenModel)
                QuizGameState.MULTIPLAYER_LOBBY ->
                    QuizMultiplayerLobby(screenModel, onBackClick = screenModel::resetGame)
                QuizGameState.LEADERBOARD -> QuizLeaderboardScreen(screenModel)
                QuizGameState.GAME_OVER, QuizGameState.SUCCESS -> QuizResultScreen(
                    title = if (gameState == QuizGameState.SUCCESS) "Tur tamamlandı" else "Canın bitti",
                    score = screenModel.score.value,
                    onRetry = {
                        if (screenModel.round == ArenaRound.DUEL) screenModel.findMultiplayerMatch()
                        else screenModel.startGame(screenModel.round)
                    },
                    onBack = screenModel::resetGame
                )
            }
        }
    }
}

@Composable
private fun QuizPlayingArea(screenModel: QuizScreenModel) {
    val currentQuestion by screenModel.currentQuestion.collectAsState()
    val currentIndex by screenModel.currentQuestionIndex.collectAsState()
    val score by screenModel.score.collectAsState()
    val timeLeft by screenModel.timeLeft.collectAsState()
    val selectedAnswer by screenModel.selectedAnswer.collectAsState()
    val opponent by screenModel.opponent.collectAsState()
    val question = currentQuestion ?: return
    val totalQuestions = screenModel.round.questionCount
    val optionsScroll = rememberScrollState()

    LaunchedEffect(question.id) { optionsScroll.scrollTo(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(optionsScroll)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Soru ${currentIndex + 1}/$totalQuestions")
            if (screenModel.round.secondsPerQuestion != null) Text("$timeLeft sn")
            Text("$score puan", fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { currentIndex.toFloat() / totalQuestions },
            modifier = Modifier.fillMaxWidth()
        )
        opponent?.let {
            Text(
                "${it.name.ifBlank { "Rakip" }} · ${it.answeredQuestions}/$totalQuestions soru · ${it.score} puan",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().testTag("quiz-question-text").semantics(mergeDescendants = true) {},
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(
                text = question.question,
                modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = PlayfairDisplayFamily
            )
        }
        question.options.forEachIndexed { index, option ->
            val revealed = selectedAnswer != null
            val color = when {
                revealed && index == question.correctIndex -> Color(0xFF087950)
                selectedAnswer == index -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            }
            OutlinedCard(
                onClick = { screenModel.submitAnswer(index) },
                enabled = !revealed,
                modifier = Modifier.fillMaxWidth().testTag("quiz-option-$index"),
                border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(14.dp)
                ) {
                    Text(('A' + index).toString(), color = color, fontWeight = FontWeight.Bold)
                    Text(option, color = color)
                }
            }
        }
        if (selectedAnswer != null && question.hint.isNotBlank()) {
            Text(question.hint, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
fun QuizLeaderboardScreen(screenModel: QuizScreenModel) {
    val leaderboard by screenModel.leaderboard.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (leaderboard.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Sıralamada henüz puan yok.")
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
