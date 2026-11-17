package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.semtinazizi.kitapduy.data.model.QuizLeaderboardEntry
import com.semtinazizi.kitapduy.model.QuizQuestion
import com.semtinazizi.kitapduy.ui.theme.PlayfairDisplayFamily
import com.semtinazizi.kitapduy.ui.viewmodel.ArenaRound
import com.semtinazizi.kitapduy.ui.viewmodel.QuizScreenModel

@Composable
internal fun ArenaMenu(
    screenModel: QuizScreenModel,
    showChallenges: Boolean,
    onChallenges: () -> Unit,
    onAuthor: () -> Unit
) {
    val leaderboard by screenModel.leaderboard.collectAsState()
    val riddle = remember(screenModel) { screenModel.dailyQuestion() }
    var briefingRound by remember { mutableStateOf<ArenaRound?>(null) }
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showChallenges) {
            ArenaCard(
                title = ArenaRound.BLITZ.title,
                description = "10 soru · Her soruya 10 saniye · 1 can",
                accent = Color(0xFFEF4444),
                modifier = Modifier.testTag("quiz-start-blitz"),
                onClick = { briefingRound = ArenaRound.BLITZ }
            )
            ArenaCard(
                title = ArenaRound.TOURNAMENT.title,
                description = "10 soru · 1 can",
                accent = Color(0xFFF59E0B),
                modifier = Modifier.testTag("quiz-tournament-button"),
                onClick = { briefingRound = ArenaRound.TOURNAMENT }
            )
            ArenaCard(
                title = "Rastgele düello",
                description = "Rakip ara",
                accent = Color(0xFF10B981),
                onClick = screenModel::findMultiplayerMatch
            )
        } else {
            ArenaCard(
                title = ArenaRound.LITERATURE.title,
                description = "5 soru · Süre sınırı yok",
                accent = Color(0xFFD4AF37),
                modifier = Modifier.testTag("quiz-start-classic"),
                onClick = { briefingRound = ArenaRound.LITERATURE }
            )
            DailyRiddle(question = riddle)
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ArenaCard(
                    title = "Düello",
                    description = "Rastgele eşleşme",
                    accent = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f).fillMaxHeight().testTag("quiz-duel-button"),
                    onClick = screenModel::findMultiplayerMatch
                )
                ArenaCard(
                    title = "Meydan Okuma",
                    description = "Blitz ve turnuva",
                    accent = Color(0xFF06B6D4),
                    modifier = Modifier.weight(1f).fillMaxHeight().testTag("quiz-challenges-button"),
                    onClick = onChallenges
                )
            }
        }
        ArenaCard(
            title = "Kimin Kaleminden?",
            description = "Alıntının yazarını bul",
            accent = Color(0xFF38BDF8),
            onClick = onAuthor
        )
        if (!showChallenges) {
            ArenaPodium(leaderboard = leaderboard, onLeaderboard = { screenModel.loadLeaderboard() })
        }
    }

    briefingRound?.let { round ->
        val details = when (round) {
            ArenaRound.LITERATURE -> "Beş soruluk bir edebiyat molası. Her doğru yanıttan sonra kısa bir açıklama göreceksin."
            ArenaRound.BLITZ -> "On soru, her biri için on saniye. Doğru yanıtlar yirmişer puan kazandırır."
            ArenaRound.TOURNAMENT -> "On edebiyat sorusu. İlk yanlış cevabında tur sona erer."
            ArenaRound.DUEL -> ""
        }
        AlertDialog(
            onDismissRequest = { briefingRound = null },
            title = { Text(round.title, fontFamily = PlayfairDisplayFamily) },
            text = { Text(details) },
            confirmButton = {
                Button(
                    onClick = {
                        briefingRound = null
                        screenModel.startGame(round)
                    },
                    modifier = Modifier.testTag("quiz-briefing-start")
                ) { Text("Başla") }
            },
            dismissButton = {
                TextButton(onClick = { briefingRound = null }) { Text("Vazgeç") }
            }
        )
    }
}

@Composable
private fun ArenaCard(
    title: String,
    description: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.surface.luminance() > 0.5f
    val gradient = Brush.linearGradient(
        listOf(scheme.surface, accent.copy(alpha = if (isLight) 0.19f else 0.15f))
    )
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.40f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().background(gradient).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontFamily = PlayfairDisplayFamily)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DailyRiddle(question: QuizQuestion) {
    var selection by rememberSaveable(question.id) { mutableStateOf<Int?>(null) }
    OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Günün Edebî Bilmecesi", style = MaterialTheme.typography.titleMedium, fontFamily = PlayfairDisplayFamily)
            Text(question.question)
            question.options.forEachIndexed { index, option ->
                val label = when {
                    selection != null && index == question.correctIndex -> "✓ $option"
                    selection == index -> "✕ $option"
                    else -> option
                }
                OutlinedButton(
                    onClick = { selection = index },
                    enabled = selection == null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(label)
                }
            }
            if (selection != null && question.hint.isNotBlank()) {
                Text(question.hint, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ArenaPodium(leaderboard: List<QuizLeaderboardEntry>, onLeaderboard: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Şampiyonlar Podyumu", style = MaterialTheme.typography.titleLarge, fontFamily = PlayfairDisplayFamily)
            if (leaderboard.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    for ((index, height) in listOf(1 to 88, 0 to 110, 2 to 76)) {
                        val entry = leaderboard.getOrNull(index) ?: continue
                        val medalColor = listOf(Color(0xFFD4AF37), Color(0xFF9CA3AF), Color(0xFFCD7F32))[index]
                        Surface(
                            modifier = Modifier.weight(1f).height(height.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = medalColor.copy(alpha = 0.19f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("${index + 1}.", fontWeight = FontWeight.Bold)
                                Text(entry.userName, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                Text("${entry.totalScore} P", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            OutlinedButton(onClick = onLeaderboard, modifier = Modifier.fillMaxWidth().testTag("quiz-leaderboard-button")) {
                Text("Liderlik Tablosu")
            }
        }
    }
}
