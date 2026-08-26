import os

path = "C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/di/AppModule.kt"

with open(path, "r", encoding="utf-8") as f:
    code = f.read()

repo_bind = """    single<com.semtinazizi.kitapduy.domain.repository.SocialRepository> { 
        com.semtinazizi.kitapduy.data.repository.SupabaseSocialRepository(get(), get()) 
    }
    
    single<com.semtinazizi.kitapduy.domain.repository.QuizMultiplayerRepository> { 
        com.semtinazizi.kitapduy.data.repository.SupabaseQuizMultiplayerRepository(get()) 
    }"""

if "QuizMultiplayerRepository" not in code:
    code = code.replace("""    single<com.semtinazizi.kitapduy.domain.repository.SocialRepository> { 
        com.semtinazizi.kitapduy.data.repository.SupabaseSocialRepository(get(), get()) 
    }""", repo_bind)
    
    with open(path, "w", encoding="utf-8") as f:
        f.write(code)

print("SUCCESS")
