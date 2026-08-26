import re
path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\HomeScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    c = f.read()

c = c.replace('onClick = { book -> navigator.push(BookDetailScreen(book)) },', 'onClick = {  }, // FIXME: HeroBookCard has no book argument in onClick')
with open(path, 'w', encoding='utf-8') as f:
    f.write(c)

print("Fixed HomeScreen")
