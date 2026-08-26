import os

path = "C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/ui/screens/QuizScreen.kt"
with open(path, "r", encoding="utf-8") as f:
    code = f.read()

code = code.replace("QuizMainMenu(onStartSinglePlayer = { screenModel.startGame() })", 
                    "QuizMainMenu(\n                            onStartSinglePlayer = { screenModel.startGame() },\n                            onStartMultiplayer = { screenModel.findMultiplayerMatch() },\n                            onLeaderboard = { screenModel.loadLeaderboard() }\n                        )")

code = code.replace("fun QuizMainMenu(onStartSinglePlayer: () -> Unit) {",
                    "fun QuizMainMenu(onStartSinglePlayer: () -> Unit, onStartMultiplayer: () -> Unit, onLeaderboard: () -> Unit) {")

code = code.replace("onClick = { screenModel.findMultiplayerMatch() }", "onClick = onStartMultiplayer")
code = code.replace("onClick = { screenModel.loadLeaderboard() }", "onClick = onLeaderboard")

with open(path, "w", encoding="utf-8") as f:
    f.write(code)

print("SUCCESS")
