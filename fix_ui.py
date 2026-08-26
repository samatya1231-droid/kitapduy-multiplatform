import os
import re

path = "C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/ui/screens/QuizScreen.kt"
with open(path, "r", encoding="utf-8") as f:
    code = f.read()

code = code.replace("question.options", "currentQuestion?.options ?: emptyList()")
code = code.replace("question.correctIndex", "currentQuestion?.correctIndex")
code = code.replace("question.question", "currentQuestion?.question ?: \"\"")
code = code.replace("question.hint", "currentQuestion?.hint ?: \"\"")

with open(path, "w", encoding="utf-8") as f:
    f.write(code)

print("SUCCESS")
