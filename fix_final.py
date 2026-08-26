import re
import os

path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\HomeScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    c = f.read()

c = c.replace('val photoUrl by authViewModel.userPhotoUrl', 'val photoUrl = userProfile?.photoUrl')
c = c.replace('val allBooks by homeViewModel.allBooks.collectAsState()', 'val allBooks by homeViewModel.books.collectAsState()')
c = c.replace('onFocusClick = { navigator.push(FocusScreen()) },', 'onTvClick = {  },')
c = c.replace('onDiscoverClick = { navigator.push(ScienceArticlesScreen()) },', 'onArticleClick = { navigator.push(ScienceArticlesScreen()) },')

with open(path, 'w', encoding='utf-8') as f:
    f.write(c)

path2 = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\PodcastListScreen.kt'
if os.path.exists(path2):
    with open(path2, 'r', encoding='utf-8') as f:
        c2 = f.read()
    c2 = c2.replace('SpotifySquarePodcastCard(', 'androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxWidth().height(100.dp)) //')
    with open(path2, 'w', encoding='utf-8') as f:
        f.write(c2)

print("Fixed")
