package com.semtinazizi.kitapduy.data.repository

import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.model.UserProfile
import com.semtinazizi.kitapduy.platform.AppConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SupabaseAuthRepository(private val supabaseClient: SupabaseClient) : AuthRepository {
    
    private fun fixUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""
        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("data:image")) return url
        return "${AppConfig.cfPublicUrl.trimEnd('/')}/${url.trimStart('/')}"
    }

    override val currentUserUid: String?
        get() = supabaseClient.auth.currentUserOrNull()?.id

    override val isUserLoggedIn: Boolean
        get() = currentUserUid != null

    override suspend fun awaitSessionInitialization() {
        try {
            supabaseClient.auth.awaitInitialization()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun getUserProfileFlow(uid: String): Flow<UserProfile?> = flow {
        // Normalde Realtime veya ak��kan bir yap� kurulabilir.
        // Basitlik ad�na tek seferlik �ekip emit ediyoruz. Gerekirse flow mant��� geni�letilebilir.
        emit(getUserProfile(uid))
        
        // Ger�ekte: supabaseClient.realtime ile tablo dinlenip g�ncellemeler emit edilir.
    }

    override suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            val profile = supabaseClient.postgrest["users"]
                .select {
                    filter { eq("uid", uid) }
                }
                .decodeSingleOrNull<UserProfile>()
            profile?.copy(
                photoUrl = fixUrl(profile.photoUrl),
                coverPhotoUrl = fixUrl(profile.coverPhotoUrl)
            )
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Boolean {
        return try {
            supabaseClient.auth.signInWith(io.github.jan.supabase.auth.providers.builtin.Email) {
                this.email = email
                this.password = password
            }
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String, name: String): Boolean {
        return try {
            supabaseClient.auth.signUpWith(io.github.jan.supabase.auth.providers.builtin.Email) {
                this.email = email
                this.password = password
            }
            
            // E�er kay�t ba�ar�l�ysa users tablosuna ekle
            val uid = supabaseClient.auth.currentUserOrNull()?.id
            if (uid != null) {
                val newUser = UserProfile(
                    uid = uid,
                    name = name,
                    bio = "Merhaba, ben yeni bir KitapDuy kullan�c�s�y�m!"
                )
                supabaseClient.postgrest["users"].insert(newUser)
            }
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun signInWithGoogle(): Boolean {
        return try {
            // Web taray�c�s� �zerinden Google OAuth ba�lat�r ve geri d�ner
            supabaseClient.auth.signInWith(io.github.jan.supabase.auth.providers.Google)
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun getGoogleOAuthUrl(): String {
        return supabaseClient.auth.getOAuthUrl(
            provider = io.github.jan.supabase.auth.providers.Google,
            redirectUrl = "kitapduy://login-callback"
        )
    }

    override suspend fun getAppleOAuthUrl(): String {
        return supabaseClient.auth.getOAuthUrl(
            provider = io.github.jan.supabase.auth.providers.Apple,
            redirectUrl = "kitapduy://login-callback"
        )
    }

    override suspend fun signInWithApple(): Boolean {
        return try {
            supabaseClient.auth.signInWith(io.github.jan.supabase.auth.providers.Apple)
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun updateProfile(uid: String, name: String, bio: String, username: String?): Boolean {
        return try {
            val updateMap = mapOf(
                "name" to name,
                "bio" to bio,
                "username" to username
            )
            supabaseClient.postgrest["users"]
                .update(updateMap) {
                    filter { eq("uid", uid) }
                }
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun updateProfilePhoto(uid: String, photoUrl: String?): Boolean {
        return try {
            val updateMap = mapOf("photo_url" to photoUrl)
            supabaseClient.postgrest["users"]
                .update(updateMap) {
                    filter { eq("uid", uid) }
                }
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    
    override suspend fun signInAnonymously(name: String): Boolean {
        return try {
            supabaseClient.auth.signInAnonymously()
            
            val uid = supabaseClient.auth.currentUserOrNull()?.id
            if (uid != null) {
                val safeName = if (name.isBlank()) "Misafir" else name.trim()
                try {
                    val profile = UserProfile(
                        uid = uid, 
                        name = safeName, 
                        username = "misafir_${uid.take(5)}",
                        bio = "Merhaba, ben bir misafir kullan�c�y�m!"
                    )
                    supabaseClient.postgrest["users"].upsert(profile)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
                true
            } else {
                println("Auth Error: Current user is null after signInAnonymously")
                false
            }
        } catch (e: Throwable) {
            println("Auth Error: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    override suspend fun signOut() {
        try {
            supabaseClient.auth.signOut()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
