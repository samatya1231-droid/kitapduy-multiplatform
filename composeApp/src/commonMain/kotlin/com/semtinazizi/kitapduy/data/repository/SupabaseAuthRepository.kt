package com.semtinazizi.kitapduy.data.repository

import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.model.UserProfile
import com.semtinazizi.kitapduy.platform.AppConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import io.github.jan.supabase.auth.status.SessionStatus

class SupabaseAuthRepository(private val supabaseClient: SupabaseClient) : AuthRepository {
    override val authenticationChanges: Flow<Boolean?> = supabaseClient.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> status.session.user != null
            is SessionStatus.NotAuthenticated -> false
            else -> null
        }
    }.distinctUntilChanged()

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
        emit(getUserProfile(uid))
    }

    override suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            val profile = supabaseClient.postgrest["users"]
                .select { filter { eq("uid", uid) } }
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
            val uid = supabaseClient.auth.currentUserOrNull()?.id
            if (uid != null) {
                val newUser = UserProfile(uid = uid, name = name, bio = "Merhaba, ben yeni bir KitapDuy kullanicisiyim!")
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
            val updateMap = mapOf("name" to name, "bio" to bio, "username" to username)
            supabaseClient.postgrest["users"].update(updateMap) { filter { eq("uid", uid) } }
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun updateProfilePhoto(uid: String, photoUrl: String?): Boolean {
        return try {
            val updateMap = mapOf("photo_url" to photoUrl)
            supabaseClient.postgrest["users"].update(updateMap) { filter { eq("uid", uid) } }
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
                    val profile = UserProfile(uid = uid, name = safeName, username = "misafir_${uid.take(5)}", bio = "Merhaba!")
                    supabaseClient.postgrest["users"].upsert(profile)
                } catch (e: Throwable) { e.printStackTrace() }
                true
            } else false
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun signOut() {
        try { supabaseClient.auth.signOut() } catch (e: Throwable) { e.printStackTrace() }
    }

    /** Apple Guideline 5.1.1 - Uygulama ici hesap silme */
    override suspend fun deleteAccount(): Boolean {
        val uid = currentUserUid ?: return false
        return try {
            supabaseClient.postgrest["posts"].delete { filter { eq("user_id", uid) } }
            supabaseClient.postgrest["user_bookshelves"].delete { filter { eq("user_id", uid) } }
            // Takip kayitlarini iki ayri sorguyla sil (or{} DSL supabase 3.x'te farkli)
            supabaseClient.postgrest["followers"].delete { filter { eq("follower_id", uid) } }
            supabaseClient.postgrest["followers"].delete { filter { eq("following_id", uid) } }
            supabaseClient.postgrest["notifications"].delete { filter { eq("receiver_id", uid) } }
            supabaseClient.postgrest["users"].delete { filter { eq("uid", uid) } }
            supabaseClient.auth.signOut()
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    /** Engellenen kullanici UID listesi - blocked_users tablosu yoksa bos liste doner */
    override suspend fun getBlockedUsers(uid: String): List<String> {
        return try {
            @kotlinx.serialization.Serializable
            data class BlockRecord(@kotlinx.serialization.SerialName("blocked_uid") val blockedUid: String)
            supabaseClient.postgrest["blocked_users"]
                .select { filter { eq("blocker_uid", uid) } }
                .decodeList<BlockRecord>()
                .map { it.blockedUid }
        } catch (e: Throwable) { emptyList() }
    }
}
