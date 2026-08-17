package com.semtinazizi.kitapduy.data.repository

import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.model.UserProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay

class SupabaseAuthRepository(private val supabaseClient: SupabaseClient) : AuthRepository {
    
    override val currentUserUid: String?
        get() = supabaseClient.auth.currentUserOrNull()?.id

    override val isUserLoggedIn: Boolean
        get() = currentUserUid != null

    override suspend fun awaitSessionInitialization() {
        try {
            supabaseClient.auth.awaitInitialization()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getUserProfileFlow(uid: String): Flow<UserProfile?> = flow {
        // Normalde Realtime veya akışkan bir yapı kurulabilir.
        // Basitlik adına tek seferlik çekip emit ediyoruz. Gerekirse flow mantığı genişletilebilir.
        emit(getUserProfile(uid))
        
        // Gerçekte: supabaseClient.realtime ile tablo dinlenip güncellemeler emit edilir.
    }

    override suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            supabaseClient.postgrest["users"]
                .select {
                    filter { eq("uid", uid) }
                }
                .decodeSingleOrNull<UserProfile>()
        } catch (e: Exception) {
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
        } catch (e: Exception) {
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
            
            // Eğer kayıt başarılıysa users tablosuna ekle
            val uid = supabaseClient.auth.currentUserOrNull()?.id
            if (uid != null) {
                val newUser = UserProfile(
                    uid = uid,
                    name = name,
                    bio = "Merhaba, ben yeni bir KitapDuy kullanıcısıyım!"
                )
                supabaseClient.postgrest["users"].insert(newUser)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun signInWithGoogle(): Boolean {
        return try {
            // Web tarayıcısı üzerinden Google OAuth başlatır ve geri döner
            supabaseClient.auth.signInWith(io.github.jan.supabase.auth.providers.Google)
            true
        } catch (e: Exception) {
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
        } catch (e: Exception) {
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
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun updateProfilePhoto(uid: String, photoUrl: String?): Boolean {
        return try {
            val updateMap = mapOf("photoUrl" to photoUrl)
            supabaseClient.postgrest["users"]
                .update(updateMap) {
                    filter { eq("uid", uid) }
                }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun signOut() {
        try {
            supabaseClient.auth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
