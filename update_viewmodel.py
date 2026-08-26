import os

path = "C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/di/ViewModelModule.kt"

with open(path, "r", encoding="utf-8") as f:
    code = f.read()

# I need to change factory { QuizScreenModel() } to inject dependencies if I change it
if "factory { com.semtinazizi.kitapduy.ui.viewmodel.QuizScreenModel() }" in code:
    code = code.replace("factory { com.semtinazizi.kitapduy.ui.viewmodel.QuizScreenModel() }", "factory { com.semtinazizi.kitapduy.ui.viewmodel.QuizScreenModel(get(), get()) }")
    with open(path, "w", encoding="utf-8") as f:
        f.write(code)

print("SUCCESS")
