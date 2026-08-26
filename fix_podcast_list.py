import os

path = "C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/ui/screens/PodcastListScreen.kt"
with open(path, "r", encoding="utf-8") as f:
    code = f.read()

replacement = """
                if (podcasts.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(150.dp),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(podcasts, key = { it.id }) { podcast ->
                            com.semtinazizi.kitapduy.ui.screens.SpotifySquarePodcastCard(podcast = podcast) {
                                navigator.push(com.semtinazizi.kitapduy.ui.screens.PodcastEpisodesScreen(podcast.id, podcast.name, podcast.rssUrl))
                            }
                        }
                    }
                }
"""

code = code.replace("""
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(podcasts, key = { it.id }) { podcast ->
                        com.semtinazizi.kitapduy.ui.screens.SpotifySquarePodcastCard(podcast = podcast) {
                            navigator.push(com.semtinazizi.kitapduy.ui.screens.PodcastEpisodesScreen(podcast.id, podcast.name, podcast.rssUrl))
                        }
                    }
                }
""", replacement)

with open(path, "w", encoding="utf-8") as f:
    f.write(code)

print("SUCCESS")
