const fs = require('fs');

let repoPath = 'C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/ui/screens/QuizScreen.kt';
let code = fs.readFileSync(repoPath, 'utf8');

// Add imports
if (!code.includes('import com.semtinazizi.kitapduy.platform.AdManager')) {
    code = code.replace(
        'import androidx.compose.ui.window.Dialog',
        'import androidx.compose.ui.window.Dialog\nimport com.semtinazizi.kitapduy.platform.AdManager\nimport com.semtinazizi.kitapduy.platform.BillingManager\nimport org.koin.compose.koinInject'
    );
}

// In QuizScreen, inject AdManager and BillingManager
if (!code.includes('val adManager: AdManager = koinInject()')) {
    code = code.replace(
        'val screenModel = koinScreenModel<QuizScreenModel>()',
        'val screenModel = koinScreenModel<QuizScreenModel>()\n        val adManager: AdManager = koinInject()\n        val billingManager: BillingManager = koinInject()\n        val isPremium by billingManager.isPremium.collectAsState()'
    );
}

// Pass dependencies to PlayArea and ResultScreen
if (code.includes('QuizPlayingArea(screenModel = screenModel)')) {
    code = code.replace(
        'QuizPlayingArea(screenModel = screenModel)',
        'QuizPlayingArea(screenModel = screenModel, adManager = adManager, isPremium = isPremium)'
    );
}
if (code.includes('QuizResultScreen(screenModel = screenModel, onBackClick = onBackClick)')) {
    code = code.replace(
        'QuizResultScreen(screenModel = screenModel, onBackClick = onBackClick)',
        'QuizResultScreen(screenModel = screenModel, onBackClick = { \n                if (!isPremium && !adManager.isAdFreeActive()) adManager.showInterstitialIfReady()\n                onBackClick()\n            }, adManager = adManager)'
    );
}

// Update PlayArea signature
if (!code.includes('fun QuizPlayingArea(screenModel: QuizScreenModel, adManager: AdManager, isPremium: Boolean)')) {
    code = code.replace(
        'fun QuizPlayingArea(screenModel: QuizScreenModel) {',
        'fun QuizPlayingArea(screenModel: QuizScreenModel, adManager: AdManager, isPremium: Boolean) {'
    );
}

// Update Jokers to use AdManager
if (code.includes('screenModel.useFiftyFifty()')) {
    code = code.replace(
        'screenModel.useFiftyFifty()',
        'if (isPremium || adManager.isAdFreeActive()) screenModel.useFiftyFifty() else adManager.showRewardedAd { screenModel.useFiftyFifty() }'
    );
    code = code.replace(
        'screenModel.useExtraTime()',
        'if (isPremium || adManager.isAdFreeActive()) screenModel.useExtraTime() else adManager.showRewardedAd { screenModel.useExtraTime() }'
    );
    code = code.replace(
        'screenModel.useHint()',
        'if (isPremium || adManager.isAdFreeActive()) screenModel.useHint() else adManager.showRewardedAd { screenModel.useHint() }'
    );
}

// Add Emoji State
if (!code.includes('val lastEmoji by screenModel.lastEmoji.collectAsState()')) {
    code = code.replace(
        'val isHintUsed by screenModel.isHintUsed.collectAsState()',
        'val isHintUsed by screenModel.isHintUsed.collectAsState()\n    val lastEmoji by screenModel.lastEmoji.collectAsState()'
    );
}

// Add Banner Ad
if (!code.includes('BannerAdView(')) {
    code = code.replace(
        'import org.koin.compose.koinInject',
        'import org.koin.compose.koinInject\nimport com.semtinazizi.kitapduy.platform.BannerAdView'
    );
    code = code.replace(
        'ConfettiAnimation(visible = isCorrect == true)',
        'ConfettiAnimation(visible = isCorrect == true)\n\n        // Banner Ad\n        if (!isPremium && !adManager.isAdFreeActive()) {\n            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {\n                BannerAdView(modifier = Modifier.fillMaxWidth().height(50.dp))\n            }\n        }'
    );
}

// Add Emoji Overlay and Buttons
if (!code.includes('AnimatedVisibility(visible = lastEmoji != null')) {
    code = code.replace(
        '// Şıklar\n            question.options.forEachIndexed',
        `// Emojiler
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("💯", "🔥", "🤔", "😱").forEach { emoji ->
                    Text(emoji, fontSize = 32.sp, modifier = Modifier.clickable { screenModel.sendEmoji(emoji) })
                }
            }
            
            // Şıklar
            question.options.forEachIndexed`
    );
    
    code = code.replace(
        'if (showHintDialog) {',
        `AnimatedVisibility(
            visible = lastEmoji != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            lastEmoji?.let { Text(it, fontSize = 100.sp) }
        }
        
        if (showHintDialog) {`
    );
}

// Result Screen signature
if (!code.includes('fun QuizResultScreen(screenModel: QuizScreenModel, onBackClick: () -> Unit, adManager: AdManager)')) {
    code = code.replace(
        'fun QuizResultScreen(screenModel: QuizScreenModel, onBackClick: () -> Unit) {',
        'fun QuizResultScreen(screenModel: QuizScreenModel, onBackClick: () -> Unit, adManager: AdManager) {'
    );
    
    // Add ad-free grant logic
    code = code.replace(
        'val score by screenModel.score.collectAsState()',
        'val score by screenModel.score.collectAsState()\n    \n    LaunchedEffect(Unit) {\n        if (score >= 70) {\n            adManager.grantAdFreeTime()\n        }\n    }'
    );
}

fs.writeFileSync(repoPath, code, 'utf8');
console.log("Updated QuizScreen.kt");
