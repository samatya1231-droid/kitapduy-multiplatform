import os

path = "C:/Users/Rahmi/.gemini/antigravity-ide/brain/cc854bc2-39d0-4f8b-9e27-42b654db14ad/task.md"
with open(path, "r", encoding="utf-8") as f:
    code = f.read()

code = code.replace("- [ ] Fix QuizScreen \"Blitz Arena\" button", "- [x] Fix QuizScreen \"Blitz Arena\" button")
code = code.replace("- [ ] Fix QuizScreen \"Liderlik Tablosu\" button", "- [x] Fix QuizScreen \"Liderlik Tablosu\" button")

with open(path, "w", encoding="utf-8") as f:
    f.write(code)

print("SUCCESS")
