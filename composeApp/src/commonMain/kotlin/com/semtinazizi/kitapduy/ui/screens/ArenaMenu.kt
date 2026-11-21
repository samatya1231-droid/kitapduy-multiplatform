package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        ArenaHeaderHub(leaderScore = leaderboard.firstOrNull()?.totalScore ?: 0L)
        if (showChallenges) {
            ArenaCard(
                badge = "⚡ HIZLI TUR",
                title = ArenaRound.BLITZ.title,
                description = "10 soru · Her soruya 10 saniye · 1 can",
                accent = Color(0xFFEF4444),
                modifier = Modifier.testTag("quiz-start-blitz"),
                onClick = { briefingRound = ArenaRound.BLITZ }
            )
            ArenaCard(
                badge = "🏆 ELEME MODU",
                title = ArenaRound.TOURNAMENT.title,
                description = "10 soru · 1 can",
                accent = Color(0xFFF59E0B),
                modifier = Modifier.testTag("quiz-tournament-button"),
                onClick = { briefingRound = ArenaRound.TOURNAMENT }
            )
            ArenaCard(
                badge = "⚔️ 1 BİLET",
                title = "Rastgele düello",
                description = "Rakip ara",
                accent = Color(0xFF10B981),
                onClick = screenModel::findMultiplayerMatch
            )
        } else {
            ArenaCard(
                badge = "✨ 30 DK REKLAMSIZ DİNLEME",
                title = ArenaRound.LITERATURE.title,
                description = "5 soruluk hızlı kültür turu. Bilginizi tazeleyin, reklamsız dinleme ödülünü kazanın.",
                accent = Color(0xFFD4AF37),
                ctaText = "Molaya Başla",
                modifier = Modifier.testTag("quiz-start-classic"),
                onClick = { briefingRound = ArenaRound.LITERATURE }
            )
            DailyRiddle(question = riddle)
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ArenaCard(
                    badge = "⚔️ 1 BİLET",
                    title = "Canlı Düello",
                    description = "Rakiplerle anlık yarış",
                    accent = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f).fillMaxHeight().testTag("quiz-duel-button"),
                    onClick = screenModel::findMultiplayerMatch
                )
                ArenaCard(
                    badge = "⚡ 3 MOD",
                    title = "Meydan Okuma",
                    description = "Süreye karşı yarış",
                    accent = Color(0xFF06B6D4),
                    modifier = Modifier.weight(1f).fillMaxHeight().testTag("quiz-challenges-button"),
                    onClick = onChallenges
                )
            }
            QuoteDiscoveryCard()
        }
        ArenaCard(
            badge = "🖋️ YAZAR TAHMİNİ",
            title = "Kimin Kaleminden?",
            description = "Alıntının yazarını bul",
            accent = Color(0xFF38BDF8),
            modifier = Modifier.testTag("quiz-guess-author-button"),
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
private fun ArenaHeaderHub(leaderScore: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xFFFF6D00).copy(alpha = 0.12f),
            border = BorderStroke(1.dp, Color(0xFFFF6D00).copy(alpha = 0.3f))
        ) {
            Text(
                text = "🔥 Günlük Seri",
                color = Color(0xFFFF8F00),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xFF8B5CF6).copy(alpha = 0.14f),
            border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.35f))
        ) {
            Text(
                text = if (leaderScore > 0) "🎫 2 Bilet · 🏆 $leaderScore P" else "🎫 2 Düello Bileti",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun QuoteDiscoveryCard() {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFD4AF37).copy(alpha = 0.06f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "EDEBİ KEŞİF",
                color = Color(0xFFD4AF37),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "\"Bir milletin kültür seviyesi, okuduğu ve yaşattığı kitapların derinliğiyle ölçülür.\"",
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "— KitapDuy Edebî Seçki",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ArenaCard(
    badge: String? = null,
    title: String,
    description: String,
    accent: Color,
    ctaText: String? = null,
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
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {}
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().background(gradient).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!badge.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .background(accent.copy(alpha = 0.16f), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
            }
            Text(title, style = MaterialTheme.typography.titleLarge, fontFamily = PlayfairDisplayFamily)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            if (!ctaText.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$ctaText →",
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🧩 GÜNÜN BİLMECESİ",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFD4AF37),
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF10B981).copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "+25 Puan",
                        color = Color(0xFF10B981),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
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
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).testTag("quiz-riddle-option-$index")
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
                    for ((index, height) in listOf(1 to 88, 0 to 118, 2 to 76)) {
                        val entry = leaderboard.getOrNull(index) ?: continue
                        val medalColor = listOf(Color(0xFFD4AF37), Color(0xFF9CA3AF), Color(0xFFCD7F32))[index]
                        Surface(
                            modifier = Modifier.weight(1f).height(height.dp),
                            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
                            color = medalColor.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, medalColor.copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (index == 0) {
                                    Text("👑", fontSize = 14.sp)
                                }
                                Text("${index + 1}.", fontWeight = FontWeight.Bold)
                                Text(entry.userName, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                Text("${entry.totalScore} P", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            OutlinedButton(onClick = onLeaderboard, modifier = Modifier.fillMaxWidth().testTag("quiz-leaderboard-button")) {
                Text("Liderlik Tablosu · Sıralamanı Yükselt ↗")
            }
        }
    }
}
