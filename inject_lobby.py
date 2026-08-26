import os

path = "C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/ui/viewmodel/QuizScreenModel.kt"
with open(path, "r", encoding="utf-8") as f:
    code = f.read()

lobby_logic = """
    private val _lobbyState = MutableStateFlow("Rakip Aranıyor...")
    val lobbyState = _lobbyState.asStateFlow()
    
    private var currentMatchId: String? = null

    fun findMultiplayerMatch() {
        _gameState.value = QuizGameState.MULTIPLAYER_LOBBY
        _lobbyState.value = "Rakip Aranıyor..."
        
        screenModelScope.launch {
            try {
                val uid = authRepo.currentUserUid
                if (uid == null) {
                    _lobbyState.value = "Önce giriş yapmalısınız!"
                    return@launch
                }
                val profile = authRepo.getUserProfile(uid)
                val userName = profile?.name ?: "Anonim"
                
                // Try finding waiting match
                val match = multiplayerRepo.findWaitingMatch()
                if (match != null) {
                    _lobbyState.value = "Rakip Bulundu! Bağlanıyor..."
                    val joinedMatch = multiplayerRepo.joinMatch(match.matchId, uid, userName)
                    if (joinedMatch != null) {
                        currentMatchId = joinedMatch.matchId
                        startGame(isMultiplayer = true)
                    } else {
                        _lobbyState.value = "Bağlantı hatası!"
                    }
                } else {
                    _lobbyState.value = "Oda Oluşturuluyor..."
                    val newMatch = multiplayerRepo.createMatch(uid, userName)
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

code = code.replace("fun resetGame() {", lobby_logic + "\n    fun resetGame() {")

with open(path, "w", encoding="utf-8") as f:
    f.write(code)

print("SUCCESS")
