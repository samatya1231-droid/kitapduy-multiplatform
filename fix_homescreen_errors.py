import re

path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\HomeScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    c = f.read()

# Replace NativeAdCard(...)
c = re.sub(r'NativeAdCard\([^)]*\)', 'Box(Modifier.fillMaxWidth().height(100.dp))', c)

# Replace AdmobBanner(...)
c = re.sub(r'AdmobBanner\([^)]*\)', 'Box(Modifier.fillMaxWidth().height(50.dp))', c)

# Replace BookItemSkeleton(...)
c = re.sub(r'BookItemSkeleton\([^)]*\)', 'androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxWidth().height(120.dp).background(androidx.compose.ui.graphics.Color.LightGray))', c)

# Replace shimmerBrush(...)
c = re.sub(r'shimmerBrush\(\)', 'androidx.compose.ui.graphics.Brush.linearGradient(listOf(androidx.compose.ui.graphics.Color.LightGray, androidx.compose.ui.graphics.Color.Gray))', c)

# Fix coil unresolved
c = c.replace('import coil.', 'import coil3.')
c = c.replace('coil.compose.', 'coil3.compose.')

with open(path, 'w', encoding='utf-8') as f:
    f.write(c)

print("Fixed components")
