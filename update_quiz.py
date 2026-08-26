import os

path = "C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/ui/screens/QuizScreen.kt"

with open(path, "r", encoding="utf-8") as f:
    code = f.read()

# 1. Add imports safely
imports_to_add = """
import org.koin.compose.koinInject
import com.semtinazizi.kitapduy.platform.AdManager
import com.semtinazizi.kitapduy.platform.BillingManager
import com.semtinazizi.kitapduy.platform.BannerAdView
"""
if "import org.koin.compose.koinInject" not in code:
    code = code.replace("import cafe.adriel.voyager.koin.koinScreenModel", "import cafe.adriel.voyager.koin.koinScreenModel" + imports_to_add)

# 2. Inject dependencies
if "val adManager: AdManager = koinInject()" not in code:
    code = code.replace("val screenModel = koinScreenModel<QuizScreenModel>()", """val screenModel = koinScreenModel<QuizScreenModel>()
        val adManager: AdManager = koinInject()
        val billingManager: BillingManager = koinInject()
        val isPremium by billingManager.isPremium.collectAsState()""")

# 3. Pass dependencies to PlayArea and ResultScreen
code = code.replace("QuizPlayingArea(screenModel = screenModel)", "QuizPlayingArea(screenModel = screenModel, adManager = adManager, isPremium = isPremium)")
code = code.replace("QuizResultScreen(screenModel = screenModel, onBackClick = onBackClick)", "QuizResultScreen(screenModel = screenModel, onBackClick = { if (!isPremium && !adManager.isAdFreeActive()) adManager.showInterstitialIfReady(); onBackClick() }, adManager = adManager)")

# 4. Add emoji state
code = code.replace("val isHintUsed by screenModel.isHintUsed.collectAsState()", "val isHintUsed by screenModel.isHintUsed.collectAsState()\n    val lastEmoji by screenModel.lastEmoji.collectAsState()")

# 5. ResultScreen definition signature + LaunchedEffect
if "adManager: AdManager" not in code.split("fun QuizResultScreen")[1]:
    code = code.replace("fun QuizResultScreen(screenModel: QuizScreenModel, onBackClick: () -> Unit) {", "fun QuizResultScreen(screenModel: QuizScreenModel, onBackClick: () -> Unit, adManager: AdManager) {")
    code = code.replace("val score by screenModel.score.collectAsState()", "val score by screenModel.score.collectAsState()\n    LaunchedEffect(Unit) { if (score >= 70) adManager.grantAdFreeTime() }")

# 6. PlayArea definition signature
if "adManager: AdManager" not in code.split("fun QuizPlayingArea")[1]:
    code = code.replace("fun QuizPlayingArea(screenModel: QuizScreenModel) {", "fun QuizPlayingArea(screenModel: QuizScreenModel, adManager: AdManager, isPremium: Boolean) {")
    
# 7. Jokers Ad Logic
code = code.replace("screenModel.useFiftyFifty()", "if (isPremium || adManager.isAdFreeActive()) screenModel.useFiftyFifty() else adManager.showRewardedAd { screenModel.useFiftyFifty() }")
code = code.replace("screenModel.useExtraTime()", "if (isPremium || adManager.isAdFreeActive()) screenModel.useExtraTime() else adManager.showRewardedAd { screenModel.useExtraTime() }")
code = code.replace("screenModel.useHint()", "if (isPremium || adManager.isAdFreeActive()) screenModel.useHint() else adManager.showRewardedAd { screenModel.useHint() }")

# 8. Emojis under question
emoji_row = """
            // Emojiler
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("💯", "🔥", "🤔", "😱").forEach { emoji ->
                    Text(emoji, fontSize = 32.sp, modifier = Modifier.clickable { screenModel.sendEmoji(emoji) })
                }
            }
"""
code = code.replace("// Şıklar", emoji_row + "\n            // Şıklar")

# 9. Emoji visibility inside box
visibility_code = """
        AnimatedVisibility(
            visible = lastEmoji != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            lastEmoji?.let { Text(it, fontSize = 100.sp) }
        }
"""
code = code.replace("if (showHintDialog) {", visibility_code + "\n        if (showHintDialog) {")

# 10. Banner Ad
banner_code = """
        // Banner Ad
        if (!isPremium && !adManager.isAdFreeActive()) {
            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                BannerAdView(modifier = Modifier.fillMaxWidth().height(50.dp))
            }
        }
"""
code = code.replace("ConfettiAnimation(visible = isCorrect == true)", "ConfettiAnimation(visible = isCorrect == true)\n" + banner_code)

# 11. Main Menu update
import re
new_menu = """@Composable
fun QuizMainMenu(onStartSinglePlayer: () -> Unit) {
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
            modifier = Modifier.fillMaxWidth().height(90.dp),
            onClick = { /* TODO: Tournament Logic */ }
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
            modifier = Modifier.fillMaxWidth().height(90.dp),
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
            modifier = Modifier.fillMaxWidth().height(90.dp),
            onClick = { /* TODO: Blitz Logic */ }
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
            modifier = Modifier.fillMaxWidth().height(90.dp),
            onClick = { /* TODO: Multiplayer Logic */ }
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
            modifier = Modifier.fillMaxWidth().height(80.dp),
            onClick = { /* TODO: Leaderboard */ }
        ) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text("👑 Liderlik Tablosu", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}"""
code = re.sub(r"@Composable\s*fun QuizMainMenu\(onStartSinglePlayer: \(\) -> Unit\) \{.*?(?=@Composable\s*fun QuizPlayingArea)", new_menu + "\n\n", code, flags=re.DOTALL)

with open(path, "w", encoding="utf-8") as f:
    f.write(code)

print("SUCCESS")
