package com.semtinazizi.kitapduy.domain.repository

import com.semtinazizi.kitapduy.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserUid: String?
    val isUserLoggedIn: Boolean
    val authenticationChanges: Flow<Boolean?>
        get() = kotlinx.coroutines.flow.flowOf(isUserLoggedIn)

    suspend fun awaitSessionInitialization()
    fun getUserProfileFlow(uid: String): Flow<UserProfile?>
    suspend fun getUserProfile(uid: String): UserProfile?
    suspend fun signInWithEmail(email: String, password: String): Boolean
    suspend fun signUpWithEmail(email: String, password: String, name: String): Boolean
    suspend fun signInWithGoogle(): Boolean
    suspend fun signInWithApple(): Boolean
    suspend fun getGoogleOAuthUrl(): String
    suspend fun getAppleOAuthUrl(): String
    suspend fun updateProfile(uid: String, name: String, bio: String, username: String?): Boolean
    suspend fun updateProfilePhoto(uid: String, photoUrl: String?): Boolean
    suspend fun signInAnonymously(name: String): Boolean
    suspend fun signOut()
    // Apple Guideline 5.1.1: Hesap olusturan uygulamalar uygulama icinden silmeyi desteklemeli
    suspend fun deleteAccount(): Boolean
    // Engellenen kullanici listesi - feed filtrelemesi icin
    suspend fun getBlockedUsers(uid: String): List<String>
}
