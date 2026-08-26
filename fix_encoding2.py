import os
import re

def fix_file_encoding(path):
    if not os.path.exists(path): return
    try:
        with open(path, 'r', encoding='utf-8') as f:
            content = f.read()
    except UnicodeDecodeError:
        with open(path, 'r', encoding='windows-1254') as f:
            content = f.read()
            
    # Fix the common broken strings
    # "Ak" -> "Akış", "Askda" -> "Askıda", "E?lence" -> "Eğlence", "Kitapl?k" -> "Kitaplık"
    content = re.sub(r'val title = "Ak?"', 'val title = "Akış"', content)
    content = re.sub(r'val title = "Ask?da"', 'val title = "Askıda"', content)
    content = re.sub(r'val title = "E\?lence"', 'val title = "Eğlence"', content)
    content = re.sub(r'val title = "Elence"', 'val title = "Eğlence"', content)
    content = re.sub(r'val title = "Kitapl\?k"', 'val title = "Kitaplık"', content)
    content = re.sub(r'val title = "Kitaplk"', 'val title = "Kitaplık"', content)
    
    content = re.sub(r'"E\?lence & Ke\?fet"', '"Eğlence & Keşfet"', content)
    content = re.sub(r'"Elence & Kefet"', '"Eğlence & Keşfet"', content)

    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

base = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\tabs'
fix_file_encoding(os.path.join(base, 'AskidaTab.kt'))
fix_file_encoding(os.path.join(base, 'EntertainmentTab.kt'))
fix_file_encoding(os.path.join(base, 'FeedTab.kt'))
fix_file_encoding(os.path.join(base, 'LibraryTab.kt'))
fix_file_encoding(r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\KitapBilScreen.kt')

print("Encoding fixed.")
