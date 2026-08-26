const fs = require('fs');

let repoPath = 'C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/ui/screens/QuizScreen.kt';
let code = fs.readFileSync(repoPath, 'utf8');

const newMenu = `
@Composable
fun QuizMainMenu(onStartSinglePlayer: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text("📚 KitapDuy Arena", color = MaterialTheme.colorScheme.onSurface, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Text("Edebiyat bilgini sına, ödülleri kazan!", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 16.sp, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(32.dp))

        // Haftalık Turnuva
        GlassCard(
            modifier = Modifier.fillMaxWidth().height(90.dp),
            onClick = { /* TODO: Tournament Logic */ }
        ) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("🏆 Haftalık Turnuva", color = Color(0xFFD4AF37), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Dünya Klasikleri • 1 Can", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Tek Oyunculu Mod
        GlassCard(
            modifier = Modifier.fillMaxWidth().height(90.dp),
            onClick = onStartSinglePlayer
        ) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Klasik Mod", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Antrenman Yap • 3 Can", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Blitz Arena
        GlassCard(
            modifier = Modifier.fillMaxWidth().height(90.dp),
            onClick = { /* TODO: Blitz Logic */ }
        ) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("⚡ Blitz Arena", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Hızlı Cevaplar • 1 Can", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Çok Oyunculu Mod
        GlassCard(
            modifier = Modifier.fillMaxWidth().height(90.dp),
            onClick = { /* TODO: Multiplayer Logic */ }
        ) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.People, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Çok Oyunculu Düello", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Yakında Çok Daha Güçlü!", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 12.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Liderlik Tablosu
        GlassCard(
            modifier = Modifier.fillMaxWidth().height(80.dp),
            onClick = { /* TODO: Leaderboard */ }
        ) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text("👑 Liderlik Tablosu", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}
`;

const regex = /@Composable\s+fun QuizMainMenu\(onStartSinglePlayer:\s*\(\)\s*->\s*Unit\)\s*\{[\s\S]*?Spacer\(modifier\s*=\s*Modifier\.height\(100\.dp\)\)\s*\}\s*\}/m;
code = code.replace(regex, newMenu.trim());

fs.writeFileSync(repoPath, code, 'utf8');
console.log("Updated QuizMainMenu");
