import os

path = "C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/ui/viewmodel/QuizScreenModel.kt"
with open(path, "r", encoding="utf-8") as f:
    code = f.read()

code = code.replace("val user = authRepo.getCurrentUser()", "val uid = authRepo.currentUserUid")
code = code.replace("if (user == null)", "if (uid == null)")
code = code.replace("if (user != null)", "if (uid != null)")
code = code.replace("authRepo.getUserProfile(user.id)", "authRepo.getUserProfile(uid)")
code = code.replace("user.id", "uid")

with open(path, "w", encoding="utf-8") as f:
    f.write(code)

print("SUCCESS")
