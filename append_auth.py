import os
import uuid

# 1. AuthRepository.kt
path1 = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\domain\repository\AuthRepository.kt'
with open(path1, 'r', encoding='utf-8') as f:
    c1 = f.read()

if 'suspend fun signInAnonymously' not in c1:
    c1 = c1.replace('suspend fun signOut()', 'suspend fun signInAnonymously(name: String): Boolean\n    suspend fun signOut()')
    with open(path1, 'w', encoding='utf-8') as f:
        f.write(c1)

# 2. SupabaseAuthRepository.kt
path2 = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\data\repository\SupabaseAuthRepository.kt'
with open(path2, 'r', encoding='utf-8') as f:
    c2 = f.read()

anon_impl = '''
    override suspend fun signInAnonymously(name: String): Boolean {
        try {
            // Supabase Kotlin SDK 2.x supports signInAnonymously if enabled in dashboard
            // For now, let's try the native anonymous sign in
            supabaseClient.auth.signInAnonymously()
            
            // Create user profile
            val uid = supabaseClient.auth.currentUserOrNull()?.id
            if (uid != null) {
                val profile = UserProfile(uid = uid, name = name, username = "misafir_\")
                supabaseClient.postgrest["users"].upsert(profile)
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: create random guest account
            try {
                val randomString = kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString()
                val email = "guest_\@kitapduy.app"
                val pass = "Guest123456!"
                val result = signUpWithEmail(email, pass, name)
                return result
            } catch (fallbackE: Exception) {
                fallbackE.printStackTrace()
            }
            return false
        }
    }
'''

if 'signInAnonymously' not in c2:
    c2 = c2.replace('override suspend fun signOut()', anon_impl + '\n    override suspend fun signOut()')
    with open(path2, 'w', encoding='utf-8') as f:
        f.write(c2)

# 3. AuthViewModel.kt
path3 = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\viewmodel\AuthViewModel.kt'
with open(path3, 'r', encoding='utf-8') as f:
    c3 = f.read()

vm_impl = '''
    suspend fun signInAnonymously(name: String): Boolean {
        val result = authRepository.signInAnonymously(name)
        if (result) {
            _isLoggedIn.value = true
        }
        return result
    }
'''

if 'suspend fun signInAnonymously' not in c3:
    c3 = c3.replace('suspend fun signInWithGoogle()', vm_impl + '\n    suspend fun signInWithGoogle()')
    with open(path3, 'w', encoding='utf-8') as f:
        f.write(c3)

print("Auth updated")
