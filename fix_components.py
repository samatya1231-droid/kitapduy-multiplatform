import re

def safe_replace(path, old, new):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    content = content.replace(old, new)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

path1 = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\components\ModernGreetingSection.kt'
# Instead of commenting out, let's just delete the block calling HeaderPremiumButton
safe_replace(path1, '''if (!isPremium) {
                        HeaderPremiumButton(
                            onClick = onPremiumClick,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }''', '')
# Fix ModernMoodCard -> MoodQuickAccessCard
with open(path1, 'r', encoding='utf-8') as f:
    c = f.read()
c = re.sub(r'ModernMoodCard\(', 'MoodQuickAccessCard(', c)
c = re.sub(r'isSelected\s*=\s*.*?,\n\s*index\s*=\s*.*?,\n', '', c)
c = re.sub(r'onLongClick\s*=\s*.*?,\n', '', c)
with open(path1, 'w', encoding='utf-8') as f:
    f.write(c)

path2 = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\components\MoodDiscovery.kt'
with open(path2, 'r', encoding='utf-8') as f:
    c2 = f.read()
c2 = re.sub(r'ModernMoodCard\(', 'MoodQuickAccessCard(', c2)
c2 = re.sub(r'isSelected\s*=\s*.*?,\n\s*index\s*=\s*.*?,\n', '', c2)
c2 = re.sub(r'onLongClick\s*=\s*.*?,\n', '', c2)
with open(path2, 'w', encoding='utf-8') as f:
    f.write(c2)

print("Fixed cleanly.")
