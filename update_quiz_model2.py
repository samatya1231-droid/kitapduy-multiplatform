import os

path = "C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/ui/viewmodel/QuizScreenModel.kt"

with open(path, "r", encoding="utf-8") as f:
    code = f.read()

leaderboard_logic = """
    private val _leaderboard = MutableStateFlow<List<QuizLeaderboardEntry>>(emptyList())
    val leaderboard = _leaderboard.asStateFlow()

    fun loadLeaderboard() {
        screenModelScope.launch {
            _leaderboard.value = multiplayerRepo.getLeaderboard()
        }
    }
    
    // Save Score to Leaderboard
    fun saveScoreToLeaderboard() {
        screenModelScope.launch {
            try {
                val user = authRepo.getCurrentUser()
                if (user != null) {
                    val profile = authRepo.getUserProfile(user.id)
                    val userName = profile?.name ?: "Anonim"
                    multiplayerRepo.updateLeaderboard(user.id, userName, _score.value.toLong())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
"""

if "fun loadLeaderboard()" not in code:
    code = code.replace("private val _lives = MutableStateFlow(3)", leaderboard_logic + "\n    private val _lives = MutableStateFlow(3)")

# Hook saveScoreToLeaderboard to when game is over
if "saveScoreToLeaderboard()" not in code:
    code = code.replace("_gameState.value = QuizGameState.GAME_OVER", "_gameState.value = QuizGameState.GAME_OVER\n        saveScoreToLeaderboard()")

with open(path, "w", encoding="utf-8") as f:
    f.write(code)

print("SUCCESS")
