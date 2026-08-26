path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\HomeScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
import re
content = re.sub(r'(selectedMoodId = null,\s*)+', 'selectedMoodId = null, ', content)
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
