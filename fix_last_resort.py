import re
import os

def fix_file(path, replacements):
    if not os.path.exists(path): return
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    for old, new in replacements:
        content = re.sub(old, new, content)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

# HeroBookCard: remove darkTheme argument from whatever is calling it
fix_file(r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\components\HeroBookCard.kt', [
    (r'darkTheme\s*=\s*[a-zA-Z0-9_]+', '')
])

# ModernGreetingSection: 
# 1. HeaderPremiumButton
# 2. ModernMoodCard
fix_file(r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\components\ModernGreetingSection.kt', [
    (r'HeaderPremiumButton\(.*?\)', '/* HeaderPremiumButton */'),
    (r'ModernMoodCard\(', 'MoodQuickAccessCard(')
])

# MoodDiscovery: ModernMoodCard
fix_file(r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\components\MoodDiscovery.kt', [
    (r'ModernMoodCard\(', 'MoodQuickAccessCard(')
])

# HomeScreen: selectedMoodId
fix_file(r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\HomeScreen.kt', [
    (r'onMoodClick\s*=\s*\{\s*mood\s*->\s*navigator\.push\(MoodDetailScreen\(mood\.id\)\)\s*\}', 'selectedMoodId = null, onMoodClick = { mood -> navigator.push(MoodDetailScreen(mood.id)) }')
])

print("Fixed again.")
