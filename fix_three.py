for path in [r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\components\TrendingTop10List.kt', r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\components\HeroBookCard.kt']:
    with open(path, 'r', encoding='utf-8') as f:
        c = f.read()
    c = c.replace('import coil.', 'import coil3.')
    c = c.replace('import coil.compose.', 'import coil3.compose.')
    # Only remove actual Android dependencies, not androidx compose
    c = c.replace('import android.app.', '// import android.app.')
    c = c.replace('import android.content.', '// import android.content.')
    c = c.replace('import android.view.', '// import android.view.')
    c = c.replace('import android.widget.', '// import android.widget.')
    c = c.replace('import android.os.', '// import android.os.')
    c = c.replace('import android.net.', '// import android.net.')
    with open(path, 'w', encoding='utf-8') as f:
        f.write(c)
print("Done safely")
