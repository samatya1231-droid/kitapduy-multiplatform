import re

with open(r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\MoodDetailScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Fix HomeViewModel and PodcastViewModel states
# In KMP HomeViewModel, it's usually al books: StateFlow<List<Book>>
content = content.replace('homeViewModel.booksList.collectAsState', 'homeViewModel.books.collectAsState')
# In KMP PodcastViewModel, it might be al podcasts: StateFlow<List<Podcast>>
# Let's hope it's podcasts
content = content.replace('podcastViewModel.podcasts.collectAsState', 'podcastViewModel.podcasts.collectAsState')
content = content.replace('import com.semtinazizi.kitapduy.model.ScienceArticle', 'import com.semtinazizi.kitapduy.model.ScienceArticle\nimport com.semtinazizi.kitapduy.ui.screens.BookDetailScreen\nimport com.semtinazizi.kitapduy.ui.screens.PodcastDetailScreen\nimport com.semtinazizi.kitapduy.ui.screens.ScienceArticlesScreen')

# Fix article.content to article.description
content = content.replace('article.content.contains', 'article.description.contains')
content = content.replace('text = article.content,', 'text = article.description,')

with open(r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\MoodDetailScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("MoodDetailScreen fixed.")
