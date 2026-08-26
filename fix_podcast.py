import re

path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\PodcastListScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    c = f.read()

c = re.sub(r'SpotifySquarePodcastCard\([^)]*\)', 'androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier)', c)

with open(path, 'w', encoding='utf-8') as f:
    f.write(c)
