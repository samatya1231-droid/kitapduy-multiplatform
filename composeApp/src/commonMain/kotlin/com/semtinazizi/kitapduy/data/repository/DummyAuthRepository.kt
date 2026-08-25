package com.semtinazizi.kitapduy.data.repository

import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.delay

class DummyAuthRepository : AuthRepository {
    override val currentUserUid: String? = "test_user_123"
    
    override val isUserLoggedIn: Boolean
        get() = currentUserUid != null

    override suspend fun awaitSessionInitialization() {
        // Dummy implementation, do nothing
    }
    
    override fun getUserProfileFlow(uid: String): Flow<UserProfile?> {
        return flowOf(
            UserProfile(
                uid = uid,
                name = "Ahmet Yılmaz",
                isPremium = true
            )
        )
    }

    override suspend fun getUserProfile(uid: String): UserProfile? {
        return UserProfile(
            uid = uid,
            name = "Test User",
            bio = "Bu bir test hesabıdır.",
            isPremium = true
        )
    }

    override suspend fun signInWithEmail(email: String, password: String): Boolean {
        return true
    }

    override suspend fun signUpWithEmail(email: String, password: String, name: String): Boolean {
        // Simüle edilmiş bekleme
        delay(1000)
        return true
    }

    override suspend fun signInWithGoogle(): Boolean = true

    override suspend fun signInWithApple(): Boolean = true

    override suspend fun getGoogleOAuthUrl(): String = ""
    
    override suspend fun getAppleOAuthUrl(): String = ""

    override suspend fun updateProfile(uid: String, name: String, bio: String, username: String?): Boolean {
        return true
    }

    override suspend fun updateProfilePhoto(uid: String, photoUrl: String?): Boolean {
        return true
    }

    override suspend fun signInAnonymously(name: String): Boolean = false
    override suspend fun signOut() {
        // Session'ı temizle (Gerekliyse)
        delay(500)
    }
}
