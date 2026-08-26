path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\data\repository\SupabaseAuthRepository.kt'
with open(path, 'r', encoding='utf-8') as f:
    c = f.read()

old = '''    override suspend fun signInAnonymously(name: String): Boolean {
        try {
            // Supabase Kotlin SDK 2.x supports signInAnonymously if enabled in dashboard
            // For now, let's try the native anonymous sign in
            supabaseClient.auth.signInAnonymously()
            
            // Create user profile
            val uid = supabaseClient.auth.currentUserOrNull()?.id
            if (uid != null) {
                val profile = UserProfile(uid = uid, name = name, username = "misafir_")
                supabaseClient.postgrest["users"].upsert(profile)
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: create random guest account
            try {
                val randomString = kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString()
                val email = "guest_@kitapduy.app"
                val pass = "Guest123456!"
                val result = signUpWithEmail(email, pass, name)
                return result
            } catch (fallbackE: Exception) {
                fallbackE.printStackTrace()
            }
            return false
        }
    }'''

new = '''    override suspend fun signInAnonymously(name: String): Boolean {
        return try {
            val randomString = kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString()
            val guestEmail = "guest_@kitapduy.app"
            val guestPassword = "Guest_!"

            supabaseClient.auth.signUpWith(io.github.jan.supabase.auth.providers.builtin.Email) {
                this.email = guestEmail
                this.password = guestPassword
            }

            val uid = supabaseClient.auth.currentUserOrNull()?.id
            if (uid != null) {
                val safeName = if (name.isBlank()) "Misafir" else name.trim()
                val profile = UserProfile(
                    uid = uid,
                    name = safeName,
                    username = "misafir_"
                )
                supabaseClient.postgrest["users"].upsert(profile)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }'''

if old in c:
    c = c.replace(old, new)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(c)
    print("SUCCESS: signInAnonymously rewritten")
else:
    print("NOT FOUND - printing current method:")
    idx = c.find('override suspend fun signInAnonymously')
    if idx >= 0:
        print(repr(c[idx:idx+800]))
