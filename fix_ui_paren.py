import os

path = "C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/ui/screens/QuizScreen.kt"
with open(path, "r", encoding="utf-8") as f:
    code = f.read()

code = code.replace("currentQuestion?.options ?: emptyList().forEachIndexed", "(currentQuestion?.options ?: emptyList<String>()).forEachIndexed")
code = code.replace("currentQuestion?.options ?: emptyList<String>().forEachIndexed", "(currentQuestion?.options ?: emptyList<String>()).forEachIndexed")

with open(path, "w", encoding="utf-8") as f:
    f.write(code)

print("SUCCESS")
