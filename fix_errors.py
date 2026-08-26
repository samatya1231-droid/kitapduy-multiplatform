import os

# 1. FirebaseAuthRepository
path = r'composeApp\src\androidMain\kotlin\com\semtinazizi\kitapduy\data\repository\FirebaseAuthRepository.kt'
if os.path.exists(path):
    with open(path, 'r', encoding='utf-8') as f:
        c = f.read()
    if 'override suspend fun signInAnonymously' not in c:
        c = c.replace('override suspend fun signOut()', 'override suspend fun signInAnonymously(name: String): Boolean = false\n    override suspend fun signOut()')
        with open(path, 'w', encoding='utf-8') as f:
            f.write(c)

# 2. DummyAuthRepository
path2 = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\data\repository\DummyAuthRepository.kt'
if os.path.exists(path2):
    with open(path2, 'r', encoding='utf-8') as f:
        c2 = f.read()
    if 'override suspend fun signInAnonymously' not in c2:
        c2 = c2.replace('override suspend fun signOut()', 'override suspend fun signInAnonymously(name: String): Boolean = false\n    override suspend fun signOut()')
        with open(path2, 'w', encoding='utf-8') as f:
            f.write(c2)

# 3. SupabaseAuthRepository (Fix escape sequence)
path3 = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\data\repository\SupabaseAuthRepository.kt'
if os.path.exists(path3):
    with open(path3, 'r', encoding='utf-8') as f:
        c3 = f.read()
    c3 = c3.replace('guest_\\@kitapduy.app', 'guest_\@kitapduy.app')
    with open(path3, 'w', encoding='utf-8') as f:
        f.write(c3)

# 4. WelcomeScreen.kt (remove Icons.Filled.Apple)
path4 = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\WelcomeScreen.kt'
if os.path.exists(path4):
    with open(path4, 'r', encoding='utf-8') as f:
        c4 = f.read()
    c4 = c4.replace('import androidx.compose.material.icons.filled.Apple\n', '')
    c4 = c4.replace('Icon(Icons.Filled.Apple, contentDescription = "Apple", tint = Color.White, modifier = Modifier.size(20.dp))', '/* Removed Apple Icon temporarily */')
    with open(path4, 'w', encoding='utf-8') as f:
        f.write(c4)

print("Fixed compile errors!")
