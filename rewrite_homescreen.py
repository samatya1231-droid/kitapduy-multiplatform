import re

with open(r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\HomeScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# We want to replace everything inside LazyColumn { ... }
# So we match LazyColumn(\s*state = lazyListState,[^{]+)\{ and then replace everything up to the end of the LazyColumn.
# It might be easier to use string search since it's the main LazyColumn.

new_lazy_column = '''LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = if (isPremium) 24.dp else 84.dp)
                ) {
                    item {
                        com.semtinazizi.kitapduy.ui.components.ModernGreetingSection(
                            userName = userProfile?.username ?: "Okur",
                            userPhotoUrl = photoUrl,
                            greeting = "MERHABA",
                            streak = userProfile?.streak ?: 0,
                            unreadNotifications = 0,
                            isPremium = isPremium,
                            onStreakClick = { },
                            onNotificationsClick = { },
                            onPremiumClick = { navigator.push(PremiumUpgradeScreen()) },
                            selectedMoodId = null,
                            onMoodClick = { mood -> navigator.push(MoodDetailScreen(mood.id)) },
                            onMoodLongClick = { },
                            onProfileClick = { navigator.push(UserProfileScreen()) }
                        )
                    }

                    item {
                        com.semtinazizi.kitapduy.ui.components.DiscoverFeatureCard(
                            onClick = { navigator.push(com.semtinazizi.kitapduy.ui.screens.ScienceArticlesScreen()) },
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    if (recommendedBooks.isNotEmpty()) {
                        item {
                            com.semtinazizi.kitapduy.ui.components.SpotifySectionHeader("Trend Kitaplar")
                            com.semtinazizi.kitapduy.ui.components.TrendingTop10List(
                                books = recommendedBooks.take(10),
                                onBookClick = { navigator.push(BookDetailScreen(it.id)) }
                            )
                        }
                    }

                    if (allBooks.isNotEmpty()) {
                        item {
                            com.semtinazizi.kitapduy.ui.components.HeroBookCard(
                                book = allBooks.random(),
                                onPlayClick = { },
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    }

                    item {
                        com.semtinazizi.kitapduy.ui.components.SpotifySectionHeader("Sana Özel Kategoriler")
                        com.semtinazizi.kitapduy.ui.components.BentoGrid(
                            books = allBooks,
                            onBookClick = { navigator.push(BookDetailScreen(it.id)) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    item {
                        com.semtinazizi.kitapduy.ui.components.SpotifySectionHeader("Ruh Hali Keşfi")
                        com.semtinazizi.kitapduy.ui.components.MoodDiscovery(
                            onMoodClick = { mood -> navigator.push(MoodDetailScreen(mood.id)) }
                        )
                    }

                    item {
                        com.semtinazizi.kitapduy.ui.components.PodiumWeeklyStars(
                            books = allBooks.take(3),
                            onBookClick = { navigator.push(BookDetailScreen(it.id)) },
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                } // End of LazyColumn
'''

# Find the start of LazyColumn
start_idx = content.find('LazyColumn(')
# We want the FIRST LazyColumn in the Surface
start_idx = content.find('LazyColumn(', content.find('Surface('))

# Find the matching closing brace
open_braces = 0
found_open = False
end_idx = -1
for i in range(start_idx, len(content)):
    if content[i] == '{':
        open_braces += 1
        found_open = True
    elif content[i] == '}':
        open_braces -= 1
    
    if found_open and open_braces == 0:
        end_idx = i + 1
        break

if end_idx != -1:
    new_content = content[:start_idx] + new_lazy_column + content[end_idx:]
    with open(r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\HomeScreen.kt', 'w', encoding='utf-8') as f:
        f.write(new_content)
    print("Replaced LazyColumn successfully")
else:
    print("Could not find matching brace")

