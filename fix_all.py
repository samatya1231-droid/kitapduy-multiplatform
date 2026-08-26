import os
import re

vm_path = "C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/ui/viewmodel/QuizScreenModel.kt"
ui_path = "C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/ui/screens/QuizScreen.kt"

# Fix VM
with open(vm_path, "r", encoding="utf-8") as f:
    vm_code = f.read()

if "import com.semtinazizi.kitapduy.model.QuizQuestion" not in vm_code:
    vm_code = vm_code.replace("import com.semtinazizi.kitapduy.data.model.QuizMatch", "import com.semtinazizi.kitapduy.data.model.QuizMatch\nimport com.semtinazizi.kitapduy.model.QuizQuestion")

if "fun resetGame()" not in vm_code:
    vm_code = vm_code.replace("fun startGame(isMultiplayer: Boolean = false) {", "fun resetGame() {\n        _gameState.value = QuizGameState.STARTING\n        _score.value = 0\n        _lives.value = 3\n    }\n\n    fun startGame(isMultiplayer: Boolean = false) {")

with open(vm_path, "w", encoding="utf-8") as f:
    f.write(vm_code)

# Fix UI
with open(ui_path, "r", encoding="utf-8") as f:
    ui_code = f.read()

ui_code = ui_code.replace("currentQuestion.question", "currentQuestion?.question ?: \"\"")
ui_code = ui_code.replace("currentQuestion.options", "currentQuestion?.options ?: emptyList()")
ui_code = ui_code.replace("currentQuestion.correctIndex", "currentQuestion?.correctIndex")
ui_code = ui_code.replace("currentQuestion.hint", "currentQuestion?.hint ?: \"\"")

with open(ui_path, "w", encoding="utf-8") as f:
    f.write(ui_code)

print("SUCCESS")
