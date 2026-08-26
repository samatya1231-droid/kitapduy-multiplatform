import re

path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\components\ModernGreetingSection.kt'
with open(path, 'r', encoding='utf-8') as f:
    c = f.read()

c = re.sub(r'onClick\s*=\s*\{\s*onMoodClick\(mood\)\s*\},\s*\)\s*\}\s*\)', r'onClick = { onMoodClick(mood) }\n            )', c)

with open(path, 'w', encoding='utf-8') as f:
    f.write(c)
print("Fixed.")
