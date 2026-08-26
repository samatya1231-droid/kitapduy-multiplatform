import os

path = "C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/ui/viewmodel/QuizScreenModel.kt"
with open(path, "r", encoding="utf-8") as f:
    code = f.read()

# Fix enum
if "MULTIPLAYER_LOBBY" not in code:
    code = code.replace("enum class QuizGameState {\n    STARTING, PLAYING, GAME_OVER, SUCCESS\n}", "enum class QuizGameState {\n    STARTING, PLAYING, GAME_OVER, SUCCESS, LEADERBOARD, MULTIPLAYER_LOBBY\n}")

with open(path, "w", encoding="utf-8") as f:
    f.write(code)

print("SUCCESS")
