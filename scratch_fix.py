import re

file_path = r'C:\Users\Rahmi\AndroidStudioProjects\kitapduy-multiplatform\composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\QuizScreen.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace all InterstitialAdManager
content = re.sub(r'com\.semtinazizi\.kitapduy\.util\.InterstitialAdManager[^\n]*', '// InterstitialAdManager removed', content)

# Replace all AppOpenAdManager
content = re.sub(r'com\.semtinazizi\.kitapduy\.AppOpenAdManager[^\n]*', 'false /* AppOpenAdManager removed */', content)

# Replace composables with screens in imports/qualifiers if any
content = content.replace('com.semtinazizi.kitapduy.ui.composables', 'com.semtinazizi.kitapduy.ui.screens')

# Fix MainActivity billing
content = re.sub(r'\(context as\? MainActivity\)\?\.billingManager[^\n]*', '// billing manager removed', content)

# There's a compile error with trailing braces because of my signature replace
# wait, my signature replace added `class QuizScreen(...) : Screen { override fun Content() {`
# but my previous python script added `}\n}` to the end. That should perfectly match.

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Fixes applied.")
