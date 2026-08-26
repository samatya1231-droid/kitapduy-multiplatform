import re

path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\MoodDetailScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Remove description check
content = content.replace('||\\n                    podcast.description.contains(keyword, ignoreCase = true)', '')
content = content.replace('|| podcast.description.contains(keyword, ignoreCase = true)', '')

# Fix PodcastDetailScreen navigation
content = content.replace('PodcastDetailScreen(com.semtinazizi.kitapduy.model.PodcastEpisode(id = podcast.id.toString(), title = podcast.title, description = podcast.title, audioUrl = podcast.audioUrl ?: "", duration = podcast.duration ?: 0L, publishedAt = 0L), podcast.title)', 'PodcastDetailScreen(com.semtinazizi.kitapduy.model.PodcastEpisode(id = podcast.id, title = podcast.name, description = "", audioUrl = "", duration = "", pubDate = ""), podcast.name)')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed MoodDetailScreen 2.")
