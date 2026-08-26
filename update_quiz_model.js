const fs = require('fs');

let repoPath = 'C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/ui/viewmodel/QuizScreenModel.kt';
let code = fs.readFileSync(repoPath, 'utf8');

if (!code.includes('val lastEmoji = MutableStateFlow<String?>(null)')) {
    code = code.replace(
        'val isHintUsed = MutableStateFlow(false)',
        'val isHintUsed = MutableStateFlow(false)\n    val lastEmoji = MutableStateFlow<String?>(null)'
    );
    
    const sendEmojiMethod = `
    fun sendEmoji(emoji: String) {
        lastEmoji.value = emoji
        screenModelScope.launch {
            delay(2000)
            if (lastEmoji.value == emoji) lastEmoji.value = null
        }
    }
`;
    
    code = code.replace(
        'fun useHint() {',
        `${sendEmojiMethod}\n    fun useHint() {`
    );
    
    fs.writeFileSync(repoPath, code, 'utf8');
}
console.log("Updated QuizScreenModel.kt");
