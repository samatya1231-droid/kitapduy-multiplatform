import os

path = "C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/ui/screens/QuizScreen.kt"

with open(path, "r", encoding="utf-8") as f:
    code = f.read()

# Fix the signature to not require unused parameters
code = code.replace("fun QuizMainMenu(onStartSinglePlayer: () -> Unit, adManager: AdManager, isPremium: Boolean) {", "fun QuizMainMenu(onStartSinglePlayer: () -> Unit) {")

with open(path, "w", encoding="utf-8") as f:
    f.write(code)
