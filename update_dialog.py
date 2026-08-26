import os
import re

path = "C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/ui/screens/QuizScreen.kt"

with open(path, "r", encoding="utf-8") as f:
    code = f.read()

new_logic = """@Composable
fun QuizMainMenu(onStartSinglePlayer: () -> Unit, adManager: AdManager, isPremium: Boolean) {
    var showComingSoon by remember { mutableStateOf(false) }
    
    if (showComingSoon) {
        AlertDialog(
            onDismissRequest = { showComingSoon = false },
            title = { Text("Çok Yakında!", fontWeight = FontWeight.Bold) },
            text = { Text("Bu oyun modu şu anda geliştirme aşamasında. Çok yakında sizlerle olacak!") },
            confirmButton = {
                TextButton(onClick = { showComingSoon = false }) {
                    Text("Tamam")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {"""

code = code.replace("""@Composable
fun QuizMainMenu(onStartSinglePlayer: () -> Unit, adManager: AdManager, isPremium: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {""", new_logic)

# For any onClick that is a TODO, replace it with showComingSoon = true
code = code.replace("onClick = { /* TODO: Tournament Logic */ }", "onClick = { showComingSoon = true }")
code = code.replace("onClick = { /* TODO: Blitz Logic */ }", "onClick = { showComingSoon = true }")
code = code.replace("onClick = { /* TODO: Multiplayer Logic */ }", "onClick = { showComingSoon = true }")
code = code.replace("onClick = { /* TODO: Leaderboard */ }", "onClick = { showComingSoon = true }")


with open(path, "w", encoding="utf-8") as f:
    f.write(code)

print("SUCCESS")
