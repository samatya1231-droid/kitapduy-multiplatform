import os
import re

path = "C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/ui/screens/QuizScreen.kt"
vm_path = "C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/ui/viewmodel/QuizScreenModel.kt"

with open(path, "r", encoding="utf-8") as f:
    code = f.read()

rank_func = """
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
"""

if "fun getRankInfo" not in code:
    code = code + "\n" + rank_func

leaderboard_ui = """
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
"""

if "fun QuizLeaderboardScreen" not in code:
    code = code + "\n" + leaderboard_ui
    code = code.replace("import androidx.compose.foundation.lazy.LazyColumn", "import androidx.compose.foundation.lazy.LazyColumn\nimport androidx.compose.foundation.lazy.itemsIndexed")
    
if "QuizGameState.LEADERBOARD" not in code:
    code = code.replace("QuizGameState.GAME_OVER -> {", "QuizGameState.LEADERBOARD -> {\n                        QuizLeaderboardScreen(screenModel = screenModel, onBackClick = { screenModel.resetGame() })\n                    }\n                    QuizGameState.GAME_OVER -> {")

# Connect Leaderboard button
code = re.sub(r"""GlassCard\(\s*modifier = Modifier\.fillMaxWidth\(\)\.height\(80\.dp\),\s*onClick = \{ showComingSoon = true \}\s*\)\s*\{\s*Row\(modifier = Modifier\.fillMaxSize\(\), verticalAlignment = Alignment\.CenterVertically, horizontalArrangement = Arrangement\.Center\) \{\s*Text\("👑 Liderlik Tablosu" """, """GlassCard(
            modifier = Modifier.fillMaxWidth().height(80.dp),
            onClick = { screenModel.showLeaderboard() }
        ) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text("👑 Liderlik Tablosu" """, code)

with open(path, "w", encoding="utf-8") as f:
    f.write(code)

with open(vm_path, "r", encoding="utf-8") as fv:
    vm_code = fv.read()

if "LEADERBOARD" not in vm_code:
    vm_code = vm_code.replace("GAME_OVER\n}", "GAME_OVER,\n    LEADERBOARD\n}")

if "fun showLeaderboard()" not in vm_code:
    vm_code = vm_code.replace("fun resetGame() {", "fun showLeaderboard() {\n        _gameState.value = QuizGameState.LEADERBOARD\n    }\n\n    fun resetGame() {")

with open(vm_path, "w", encoding="utf-8") as fv:
    fv.write(vm_code)

print("SUCCESS")
