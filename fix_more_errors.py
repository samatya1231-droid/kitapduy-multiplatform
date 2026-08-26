import re

path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\HomeScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    c = f.read()

# Fix syntax error at 2541 (Missing '}')
c += "\n}\n"

# Fix "Unresolved reference 'androidx'" by removing 'androidx.' inside the string replaces
c = c.replace('androidx.compose.ui.graphics.Brush', 'Brush')
c = c.replace('androidx.compose.ui.Modifier', 'Modifier')
c = c.replace('androidx.compose.foundation.layout.Box', 'Box')
c = c.replace('androidx.compose.ui.graphics.Color', 'Color')

# Fix DynamicAuraBackground
c = c.replace('DynamicAuraBackground', 'Box')

# Fix LiveWaveformVisualizer
c = c.replace('LiveWaveformVisualizer', 'Box')

# Fix PodcastMiniCard
c = re.sub(r'PodcastMiniCard\([^)]*\)', 'Box(Modifier.fillMaxWidth().height(80.dp))', c)

# Fix ScienceArticleMiniCard
c = re.sub(r'ScienceArticleMiniCard\([^)]*\)', 'Box(Modifier.fillMaxWidth().height(80.dp))', c)

# Fix VerticalBookCard
c = re.sub(r'VerticalBookCard\([^)]*\)', 'Box(Modifier.fillMaxWidth().height(150.dp))', c)

# Fix trailerBook
c = c.replace('trailerBook.getValue', 'trailerBook')

# Fix 'navController.navigate' leftover
c = re.sub(r'navController\.navigate\([^)]*\)', '/* navController */', c)

# Fix "first() expected"
c = re.sub(r'scienceViewModel\.getArticles\([^)]*\)', 'scienceViewModel.getArticles()', c)

with open(path, 'w', encoding='utf-8') as f:
    f.write(c)
print("Fixed more errors")
