import re
import os

def fix_homescreen():
    with open(r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\HomeScreen.kt', 'r', encoding='utf-8') as f:
        content = f.read()

    # Fix BentoGrid
    old_bento = '''com.semtinazizi.kitapduy.ui.components.BentoGrid(
                            books = allBooks,
                            onBookClick = { navigator.push(BookDetailScreen(it.id)) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )'''
    new_bento = '''com.semtinazizi.kitapduy.ui.components.BentoGrid(
                            onQuizClick = { navigator.push(com.semtinazizi.kitapduy.ui.screens.QuizScreen()) },
                            onPoetryClick = { navigator.push(com.semtinazizi.kitapduy.ui.screens.IndependentPensScreen()) },
                            onTvClick = { },
                            onArticleClick = { navigator.push(com.semtinazizi.kitapduy.ui.screens.ScienceArticlesScreen()) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )'''
    content = content.replace(old_bento, new_bento)

    # Fix TrendingTop10List
    content = content.replace('books = recommendedBooks.take(10),', 'topBooks = recommendedBooks.take(10),')
    content = content.replace('onBookClick = { navigator.push(BookDetailScreen(it.id)) }', 'onBookClick = { book -> navigator.push(com.semtinazizi.kitapduy.ui.screens.BookDetailScreen(book.id)) }')

    # Fix PodiumWeeklyStars
    content = content.replace('onBookClick = { navigator.push(BookDetailScreen(it.id)) }', 'onBookClick = { book -> navigator.push(com.semtinazizi.kitapduy.ui.screens.BookDetailScreen(book.id)) }')

    with open(r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\HomeScreen.kt', 'w', encoding='utf-8') as f:
        f.write(content)

def fix_mooddetail():
    with open(r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\MoodDetailScreen.kt', 'r', encoding='utf-8') as f:
        content = f.read()

    content = re.sub(r'import com\.semtinazizi\.kitapduy\..*PodcastViewModel', '', content)
    content = re.sub(r'val podcastViewModel = .*', '', content)
    content = content.replace('podcastViewModel.podcasts', 'homeViewModel.podcasts')
    
    # Fix Koin injection
    content = content.replace('cafe.adriel.voyager.koin.koinScreenModel', 'org.koin.compose.viewmodel.koinViewModel')
    content = content.replace('koinScreenModel', 'koinViewModel')

    # Fix Missing PodcastViewModel in MoodDetailScreen
    content = content.replace('val homeViewModel = koinViewModel<HomeViewModel>()', 'val homeViewModel = koinViewModel<com.semtinazizi.kitapduy.ui.viewmodel.HomeViewModel>()')

    # Fix MoodPodcastCard arguments
    # Wait, what are the args of MoodPodcastCard?
    # fun MoodPodcastCard(podcast: Podcast, onClick: () -> Unit)
    # The error was: 170:114 No value passed for parameter 'podcastTitle'.
    # Oh! So MoodPodcastCard doesn't take podcast, it takes podcastTitle etc?!
    # No, MoodDetailScreen has its own MoodPodcastCard! I added it in the same file!
    # Let me check the definition I added.
    # @Composable fun MoodPodcastCard(podcast: Podcast, onClick: () -> Unit) {
    # It takes Podcast. Then why did it say 'Int'? Because 'filteredPodcasts' became List<Int> due to type inference failure on 'homeViewModel.podcasts' which failed to resolve because PodcastViewModel was missing!
    
    with open(r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\MoodDetailScreen.kt', 'w', encoding='utf-8') as f:
        f.write(content)

fix_homescreen()
fix_mooddetail()
print("Fixed files.")
