import os
import re

path = "C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/ui/screens/QuizScreen.kt"
vm_path = "C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/ui/viewmodel/QuizScreenModel.kt"

with open(path, "r", encoding="utf-8") as f:
    code = f.read()

lobby_ui = """
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
"""

if "fun QuizMultiplayerLobby" not in code:
    code = code + "\n" + lobby_ui
    
if "QuizGameState.MULTIPLAYER_LOBBY" not in code:
    code = code.replace("QuizGameState.LEADERBOARD -> {", "QuizGameState.MULTIPLAYER_LOBBY -> {\n                        QuizMultiplayerLobby(screenModel = screenModel, onBackClick = { screenModel.resetGame() })\n                    }\n                    QuizGameState.LEADERBOARD -> {")

# Connect Blitz Arena button
code = re.sub(r"""GlassCard\(\s*modifier = Modifier\.fillMaxWidth\(\)\.height\(80\.dp\),\s*onClick = \{ showComingSoon = true \}\s*\)\s*\{\s*Row\(modifier = Modifier\.fillMaxSize\(\), verticalAlignment = Alignment\.CenterVertically, horizontalArrangement = Arrangement\.Center\) \{\s*Text\("⚡ Blitz Arena" """, """GlassCard(
            modifier = Modifier.fillMaxWidth().height(80.dp),
            onClick = { screenModel.findMultiplayerMatch() }
        ) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text("⚡ Blitz Arena" """, code)

with open(path, "w", encoding="utf-8") as f:
    f.write(code)

with open(vm_path, "r", encoding="utf-8") as fv:
    vm_code = fv.read()

if "MULTIPLAYER_LOBBY" not in vm_code:
    vm_code = vm_code.replace("LEADERBOARD\n}", "LEADERBOARD,\n    MULTIPLAYER_LOBBY\n}")

lobby_logic = """
    private val _lobbyState = MutableStateFlow("Rakip Aranıyor...")
    val lobbyState = _lobbyState.asStateFlow()
    
    private var currentMatchId: String? = null

    fun findMultiplayerMatch() {
        _gameState.value = QuizGameState.MULTIPLAYER_LOBBY
        _lobbyState.value = "Rakip Aranıyor..."
        
        screenModelScope.launch {
            try {
                val user = authRepo.getCurrentUser()
                if (user == null) {
                    _lobbyState.value = "Önce giriş yapmalısınız!"
                    return@launch
                }
                val profile = authRepo.getUserProfile(user.id)
                val userName = profile?.name ?: "Anonim"
                
                // Try finding waiting match
                val match = multiplayerRepo.findWaitingMatch()
                if (match != null) {
                    _lobbyState.value = "Rakip Bulundu! Bağlanıyor..."
                    val joinedMatch = multiplayerRepo.joinMatch(match.matchId, user.id, userName)
                    if (joinedMatch != null) {
                        currentMatchId = joinedMatch.matchId
                        startGame(isMultiplayer = true)
                    } else {
                        _lobbyState.value = "Bağlantı hatası!"
                    }
                } else {
                    _lobbyState.value = "Oda Oluşturuluyor..."
                    val newMatch = multiplayerRepo.createMatch(user.id, userName)
                    if (newMatch != null) {
                        currentMatchId = newMatch.matchId
                        _lobbyState.value = "Oda Oluşturuldu. Rakip Bekleniyor..."
                        
                        // Listen for player 2
                        multiplayerRepo.observeMatch(newMatch.matchId).collect { update ->
                            if (update.status == "PLAYING") {
                                _lobbyState.value = "Rakip Katıldı! Oyun Başlıyor..."
                                startGame(isMultiplayer = true)
                            }
                        }
                    } else {
                        _lobbyState.value = "Oda oluşturulamadı."
                    }
                }
            } catch (e: Exception) {
                _lobbyState.value = "Hata: ${e.message}"
            }
        }
    }
"""

if "fun findMultiplayerMatch()" not in vm_code:
    vm_code = vm_code.replace("fun showLeaderboard() {", lobby_logic + "\n    fun showLeaderboard() {")

# Update startGame to accept boolean
if "fun startGame() {" in vm_code:
    vm_code = vm_code.replace("fun startGame() {", "fun startGame(isMultiplayer: Boolean = false) {")

with open(vm_path, "w", encoding="utf-8") as fv:
    fv.write(vm_code)

print("SUCCESS")
