import re

for path in [r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\components\TrendingTop10List.kt', r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\components\HeroBookCard.kt']:
    with open(path, 'r', encoding='utf-8') as f:
        c = f.read()
    c = c.replace('import coil.', 'import coil3.')
    c = c.replace('import coil.compose.', 'import coil3.compose.')
    c = c.replace('import android', '// import android')
    with open(path, 'w', encoding='utf-8') as f:
        f.write(c)

path2 = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\PodcastListScreen.kt'
with open(path2, 'r', encoding='utf-8') as f:
    c = f.read()
c = c.replace('import androidx.', '// import androidx.')
c = re.sub(r'\}[\s\n]*$', '}', c) # fix end brace maybe?
# the error in PodcastListScreen was: Syntax error: Expecting a top level declaration. at line 78
with open(path2, 'w', encoding='utf-8') as f:
    f.write(c)
print("Done")
