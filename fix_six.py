import re

path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\HomeScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    c = f.read()

c = c.replace('HeroBookCard(', 'val randomBook = allBooks.random()\n                        HeroBookCard(')
c = c.replace('book = allBooks.random(),', 'book = randomBook,')
c = c.replace('onClick = {  }, // FIXME: HeroBookCard has no book argument in onClick', 'onClick = { navigator.push(BookDetailScreen(randomBook)) },')

with open(path, 'w', encoding='utf-8') as f:
    f.write(c)
print("Fixed navigation in HeroBookCard")
