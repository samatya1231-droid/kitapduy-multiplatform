import re
import os

def fix_screen(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Use koinViewModel instead of koinScreenModel
    content = content.replace('cafe.adriel.voyager.koin.koinScreenModel', 'org.koin.compose.viewmodel.koinViewModel\nimport androidx.lifecycle.ViewModel')
    content = content.replace('koinScreenModel', 'koinViewModel')

    # Remove PodcastViewModel
    content = content.replace('import com.semtinazizi.kitapduy.ui.viewmodel.PodcastViewModel', '')
    content = content.replace('val podcastViewModel = koinViewModel<PodcastViewModel>()', '')
    content = content.replace('podcastViewModel.podcasts', 'homeViewModel.podcasts')

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

fix_screen(r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\MoodDetailScreen.kt')
fix_screen(r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\AuthorDetailScreen.kt')

print("Screens fixed.")
