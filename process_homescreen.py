import re

with open(r'C:\Users\Rahmi\.gemini\antigravity\brain\e590d70a-57f4-4e14-b649-165027e87d3d\scratch\HomeScreen_new.kt', 'r', encoding='utf-16') as f:
    content = f.read()

# Fix package
content = content.replace('package com.semtinazizi.kitapduy.ui.composables', 'package com.semtinazizi.kitapduy.ui.screens')

# Add KMP Voyager imports
voyager_imports = '''
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.koin.koinScreenModel
import com.semtinazizi.kitapduy.ui.viewmodel.AuthViewModel
import com.semtinazizi.kitapduy.ui.viewmodel.PodcastViewModel
'''
content = content.replace('import androidx.compose.material3.MaterialTheme', 'import androidx.compose.material3.MaterialTheme\n' + voyager_imports)

# Remove Android specific imports
content = re.sub(r'import android\..*', '', content)
content = re.sub(r'import androidx\.activity\..*', '', content)
content = re.sub(r'import androidx\.navigation\..*', '', content)

# Change HomeScreen from composable to Screen
content = re.sub(r'@Composable\s*fun HomeScreen\([^)]+\)\s*\{', '''
class HomeScreen : Screen {
    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val homeViewModel = koinScreenModel<HomeViewModel>()
        // val feedViewModel = koinScreenModel<FeedViewModel>()
        // val notificationViewModel = koinScreenModel<NotificationViewModel>()
        // val scienceViewModel = koinScreenModel<ScienceViewModel>()
        val podcastViewModel = koinScreenModel<PodcastViewModel>()
        val authViewModel = koinScreenModel<AuthViewModel>()
''', content)

# Replace navController.navigate calls
content = re.sub(r'navController\.navigate\("book_detail/\$\{([^}]+)\}"\)', r'navigator.push(BookDetailScreen(\1))', content)
content = re.sub(r'navController\.navigate\("podcast_detail/\$\{([^}]+)\}"\)', r'navigator.push(PodcastDetailScreen(\1))', content)
content = re.sub(r'navController\.navigate\("mood_detail/\$\{([^}]+)\}"\)', r'navigator.push(MoodDetailScreen(\1))', content)
content = re.sub(r'navController\.navigate\("author_detail/\$\{([^}]+)\}"\)', r'navigator.push(AuthorDetailScreen(\1))', content)
content = re.sub(r'navController\.navigate\("premium_upgrade"\)', r'navigator.push(PremiumUpgradeScreen())', content)
content = re.sub(r'navController\.navigate\("profile"\)', r'navigator.push(UserProfileScreen())', content)

# Change context and activity checks
content = re.sub(r'val context = LocalContext\.current', 'val context = null', content)
content = re.sub(r'\(context as\? Activity\)\?', 'null', content)
content = re.sub(r'val isPremium by \([^)]+\)\?\.billingManager\?\.isPremium\?\.collectAsState\(\) \?: remember \{ mutableStateOf\(false\) \}', 'val isPremium = authViewModel.userProfile.collectAsState().value?.hasPremium() ?: false', content)

# Replace Coil AsyncImage
content = content.replace('coil.compose.AsyncImage', 'coil3.compose.AsyncImage')

with open(r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\HomeScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)

print("HomeScreen processed.")
