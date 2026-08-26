import os
import re

def replace_in_file(path, replacements):
    if not os.path.exists(path): return
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    for old, new in replacements:
        content = content.replace(old, new)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

base = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\tabs'

replace_in_file(os.path.join(base, 'AskidaTab.kt'), [('val title = "Askda"', 'val title = "Askıda"')])
replace_in_file(os.path.join(base, 'EntertainmentTab.kt'), [('val title = "E?lence"', 'val title = "Eğlence"'), ('"E?lence & Ke?fet"', '"Eğlence & Keşfet"')])
replace_in_file(os.path.join(base, 'FeedTab.kt'), [('val title = "Ak"', 'val title = "Akış"')])
replace_in_file(os.path.join(base, 'LibraryTab.kt'), [('val title = "Kitapl?k"', 'val title = "Kitaplık"')])
replace_in_file(r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\KitapBilScreen.kt', [('"Elence & Kefet"', '"Eğlence & Keşfet"')])

print("Encoding fixed.")
