import os
import re

path = "C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/ui/viewmodel/QuizScreenModel.kt"

with open(path, "r", encoding="utf-8") as f:
    code = f.read()

# Add imports
imports = """
import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.domain.repository.QuizMultiplayerRepository
import com.semtinazizi.kitapduy.data.model.QuizLeaderboardEntry
import com.semtinazizi.kitapduy.data.model.QuizMatch
import com.semtinazizi.kitapduy.data.model.QuizQuestion
import kotlinx.coroutines.flow.asStateFlow
"""
if "import com.semtinazizi.kitapduy.domain.repository.AuthRepository" not in code:
    code = code.replace("import kotlinx.coroutines.flow.MutableStateFlow", "import kotlinx.coroutines.flow.MutableStateFlow\n" + imports)

# Change signature
if "class QuizScreenModel() : ScreenModel {" in code:
    code = code.replace("class QuizScreenModel() : ScreenModel {", "class QuizScreenModel(private val authRepo: AuthRepository, private val multiplayerRepo: QuizMultiplayerRepository) : ScreenModel {")
    code = code.replace("class QuizScreenModel : ScreenModel {", "class QuizScreenModel(private val authRepo: AuthRepository, private val multiplayerRepo: QuizMultiplayerRepository) : ScreenModel {")


with open(path, "w", encoding="utf-8") as f:
    f.write(code)

print("SUCCESS")
