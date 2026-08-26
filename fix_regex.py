import re

path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\HomeScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# We need to find the start of the function and the opening bracket
# It's something like:
# @Composable
# fun HomeScreen(
#     ... many lines ...
# ) {
# So we can match @Composable\s*fun HomeScreen[\s\S]*?\)\s*\{
# Let's replace it.

match = re.search(r'@Composable\s*fun HomeScreen[\s\S]*?\)\s*\{', content)
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
        val onMindLibraryClick: () -> Unit = { navigator.push(com.semtinazizi.kitapduy.ui.screens.ScienceArticlesScreen()) }
        val onNotificationClick: () -> Unit = {  }
        val progressManager = com.semtinazizi.kitapduy.util.KmpBookProgressManager
        val onShowAd: () -> Unit = {}
        val onQuizClick: () -> Unit = { navigator.push(com.semtinazizi.kitapduy.ui.screens.QuizScreen()) }
        val onPoetryClick: () -> Unit = { navigator.push(com.semtinazizi.kitapduy.ui.screens.IndependentPensScreen()) }
        val onFocusClick: () -> Unit = { navigator.push(com.semtinazizi.kitapduy.ui.screens.FocusScreen()) }
        val onCategoryClick: (String) -> Unit = {  }
        val onPodcastClick: () -> Unit = {}
        val onPodcastDetailClick: (Any) -> Unit = {}
        val podcastViewModel: Any? = null
        val onMoodClick: (com.semtinazizi.kitapduy.model.Mood) -> Unit = { mood -> navigator.push(MoodDetailScreen(mood.id)) }
        val onDailyDiscoverClick: () -> Unit = {}
        val onPlayRequest: (Any) -> Unit = {}
        val controller: Any? = null
        '''
    content = content.replace(old_decl, new_decl)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed the regex replacement.")
