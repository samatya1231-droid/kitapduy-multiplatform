import re

def regex_replace(path, pattern, new):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    content = re.sub(pattern, new, content)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\components\ModernGreetingSection.kt'
regex_replace(path, r'import coil\.', 'import coil3.')
regex_replace(path, r'onLongClick\s*=\s*.*?(\n|,|\))', r'\1')

path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\components\MoodDiscovery.kt'
regex_replace(path, r'onLongClick\s*=\s*.*?(\n|,|\))', r'\1')

print("Fixed coil and onLongClick")
