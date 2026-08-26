path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\data\repository\SupabaseAuthRepository.kt'
with open(path, 'r', encoding='utf-8') as f:
    c = f.read()

old = '''    override suspend fun signInAnonymously(name: String): Boolean {
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

new = '''    override suspend fun signInAnonymously(name: String): Boolean {
        return try {
            val randomString = kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString()
            val guestEmail = "guest_@kitapduy.app"
            val guestPassword = "Guest_!"

            // 1. Kayit ol
            supabaseClient.auth.signUpWith(io.github.jan.supabase.auth.providers.builtin.Email) {
                this.email = guestEmail
                this.password = guestPassword
            }

            // 2. Email onay gerekmese oturum acildi, yoksa tekrar giris yap
            if (supabaseClient.auth.currentUserOrNull() == null) {
                supabaseClient.auth.signInWith(io.github.jan.supabase.auth.providers.builtin.Email) {
                    this.email = guestEmail
                    this.password = guestPassword
                }
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
                true
            } else {
                // Hala oturum yoksa email dogrulamasi zorunlu - kullaniciyi bilgilendir
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }'''

if old in c:
    c = c.replace(old, new)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(c)
    print("SUCCESS")
else:
    print("NOT FOUND")
    idx = c.find('override suspend fun signInAnonymously')
    print(repr(c[idx:idx+300]))
