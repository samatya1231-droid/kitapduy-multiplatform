import os

path = "C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/data/model/QuizMultiplayerModels.kt"
with open(path, "r", encoding="utf-8") as f:
    code = f.read()

# Add import
if "import com.semtinazizi.kitapduy.model.QuizQuestion" not in code:
    code = code.replace("import kotlinx.serialization.SerialName", "import kotlinx.serialization.SerialName\nimport com.semtinazizi.kitapduy.model.QuizQuestion")

# Remove QuizQuestion
parts = code.split("@Serializable\ndata class QuizQuestion(")
if len(parts) > 1:
    code = parts[0].strip()

with open(path, "w", encoding="utf-8") as f:
    f.write(code)

print("SUCCESS")
