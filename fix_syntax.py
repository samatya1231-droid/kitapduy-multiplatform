import re

def fix(path):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Replace the broken syntax block
    bad_block = '''MoodQuickAccessCard(
                    mood = mood,
                                        onClick = { onMoodClick(mood) },
                    ) }
                )'''
    good_block = '''MoodQuickAccessCard(
                    mood = mood,
                    onClick = { onMoodClick(mood) }
                )'''
    
    content = content.replace(bad_block, good_block)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

fix(r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\components\MoodDiscovery.kt')

# For ModernGreetingSection.kt, let's look at what's broken
with open(r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\components\ModernGreetingSection.kt', 'r', encoding='utf-8') as f:
    content = f.read()

bad_block_2 = '''MoodQuickAccessCard(
                                mood = mood,
                                                                onClick = { onMoodClick(mood) },
                                ) }
                            )'''
good_block_2 = '''MoodQuickAccessCard(
                                mood = mood,
                                onClick = { onMoodClick(mood) }
                            )'''

content = content.replace(bad_block_2, good_block_2)
with open(r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\components\ModernGreetingSection.kt', 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed syntax")
