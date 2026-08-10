package com.semtinazizi.kitapduy.data.repository

import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.delay

class DummyAuthRepository : AuthRepository {
    override val currentUserUid: String? = "test_user_123"
    
    override val isUserLoggedIn: Boolean = true
    
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
        delay(500)
        return UserProfile(
            uid = uid,
            name = "Ahmet Yılmaz",
            isPremium = true
        )
    }

    override suspend fun signOut() {
        // Dummy sign out
    }
}
