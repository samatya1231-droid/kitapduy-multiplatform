import re

path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\HomeScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace package
content = content.replace('package com.semtinazizi.kitapduy.ui.composables', 'package com.semtinazizi.kitapduy.ui.screens')

# Add imports
imports = '''
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
'''
content = content.replace('import androidx.compose.material3.MaterialTheme', 'import androidx.compose.material3.MaterialTheme' + imports)

# Remove bad imports
bad_imports = [
    'import android.app.Activity',
    'import android.widget.Toast',
    'import androidx.activity.compose.BackHandler',
    'import androidx.lifecycle.Lifecycle',
    'import androidx.lifecycle.compose.LifecycleEventEffect',
    'import com.google.firebase.firestore.FirebaseFirestore',
    'import com.semtinazizi.kitapduy.MainActivity',
    'import com.semtinazizi.kitapduy.util.StreakManager',
    'import com.semtinazizi.kitapduy.util.RewardedAdHelper',
    'import com.semtinazizi.kitapduy.AppOpenAdManager',
    'import androidx.navigation.NavController'
]
for imp in bad_imports:
    content = content.replace(imp, '// ' + imp)

# Replace coil
content = content.replace('import coil.compose.AsyncImage', 'import coil3.compose.AsyncImage')

# Find fun HomeScreen( ... ) {
# and replace it with:
# class HomeScreen : Screen {
#     @Composable override fun Content() {
#         val navigator = LocalNavigator.currentOrThrow
#         val homeViewModel = koinViewModel<com.semtinazizi.kitapduy.ui.viewmodel.HomeViewModel>()
#         ...

match = re.search(r'@Composable\s*fun HomeScreen\s*\([^)]*\)\s*\{', content)
if match:
    old_decl = match.group(0)
    new_decl = '''class HomeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val homeViewModel = koinViewModel<com.semtinazizi.kitapduy.ui.viewmodel.HomeViewModel>()
        val feedViewModel = koinViewModel<com.semtinazizi.kitapduy.ui.viewmodel.FeedViewModel>()
        val notificationViewModel = koinViewModel<com.semtinazizi.kitapduy.ui.viewmodel.NotificationViewModel>()
        val scienceViewModel = koinViewModel<com.semtinazizi.kitapduy.ui.viewmodel.ScienceViewModel>()
        val authViewModel = koinViewModel<com.semtinazizi.kitapduy.ui.viewmodel.AuthViewModel>()
        val streakManager: Any? = null
        val onBookClick: (com.semtinazizi.kitapduy.model.Book) -> Unit = { book -> navigator.push(BookDetailScreen(book)) }
        val onArticleClick: (String) -> Unit = {}
        val onMindLibraryClick: () -> Unit = { navigator.push(ScienceArticlesScreen()) }
        val onNotificationClick: () -> Unit = { navigator.push(NotificationScreen()) }
        val progressManager = com.semtinazizi.kitapduy.util.KmpBookProgressManager
        val onShowAd: () -> Unit = {}
        val onQuizClick: () -> Unit = { navigator.push(QuizScreen()) }
        val onPoetryClick: () -> Unit = { navigator.push(IndependentPensScreen()) }
        val onFocusClick: () -> Unit = { navigator.push(FocusScreen()) }
        val onCategoryClick: (String) -> Unit = { navigator.push(CategoryHubScreen()) }
        val onPodcastClick: () -> Unit = {}
        val onPodcastDetailClick: (Any) -> Unit = {}
        val podcastViewModel: Any? = null
        val onMoodClick: (com.semtinazizi.kitapduy.model.Mood) -> Unit = { mood -> navigator.push(MoodDetailScreen(mood.id)) }
        val onDailyDiscoverClick: () -> Unit = {}
        '''
    content = content.replace(old_decl, new_decl)

# Fix some inline navigations if they use navController
content = re.sub(r'navController\.navigate\("book_detail/\$\{([^}]+)\}"\)', r'navigator.push(BookDetailScreen(homeViewModel.allBooks.value.find { it.id == \1 } ?: com.semtinazizi.kitapduy.model.Book()))', content)
content = re.sub(r'navController\.navigate\("notification"\)', r'navigator.push(NotificationScreen())', content)
content = re.sub(r'navController\.navigate\("mind_library"\)', r'navigator.push(ScienceArticlesScreen())', content)
content = re.sub(r'navController\.navigate\("quiz"\)', r'navigator.push(QuizScreen())', content)
content = re.sub(r'navController\.navigate\("poetry"\)', r'navigator.push(IndependentPensScreen())', content)
content = re.sub(r'navController\.navigate\("focus"\)', r'navigator.push(FocusScreen())', content)
content = re.sub(r'navController\.navigate\("category_hub"\)', r'navigator.push(CategoryHubScreen())', content)

# Fix shimmering
content = content.replace('import com.valentinilk.shimmer.shimmer', '// import com.valentinilk.shimmer.shimmer')
content = content.replace('.shimmer()', '')

# Replace BackHandler
content = re.sub(r'BackHandler\s*\{[^}]*\}', '', content)

# AppOpenAdManager
content = content.replace('AppOpenAdManager.showAdIfAvailable', '// AppOpenAdManager')

# LocalContext.current as Activity
content = content.replace('val activity = LocalContext.current as Activity', 'val activity: Any? = null')
content = content.replace('activity = activity,', '')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Conversion complete.")
