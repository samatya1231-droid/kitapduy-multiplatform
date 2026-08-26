path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\AuthorDetailScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('text = "\\ Eser"', 'text = " Eser"')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed AuthorDetailScreen escape.")
