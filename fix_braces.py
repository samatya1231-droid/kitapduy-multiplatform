import re

with open(r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\HomeScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# I need to insert a closing brace right before the next top-level @Composable, which is @Composable fun MoodQuickAccessCard, or @Composable fun SpotifySquareCard, actually let's just look for the first @Composable after HomeScreen Content
match = re.search(r'^\s*@Composable\s*\n\s*fun (MoodQuickAccessCard|SpotifySquareCard|CategoryHeroHeader|FeaturedCategoryBook)', content, re.MULTILINE)
if match:
    idx = match.start()
    content = content[:idx] + "}\n" + content[idx:]
    with open(r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\HomeScreen.kt', 'w', encoding='utf-8') as f:
        f.write(content)
    print("Brace fixed.")
else:
    print("Not found")
