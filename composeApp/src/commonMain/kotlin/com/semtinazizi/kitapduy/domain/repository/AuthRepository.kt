package com.semtinazizi.kitapduy.domain.repository

import com.semtinazizi.kitapduy.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserUid: String?
    val isUserLoggedIn: Boolean
    
    fun getUserProfileFlow(uid: String): Flow<UserProfile?>
    suspend fun getUserProfile(uid: String): UserProfile?
    suspend fun signOut()
}
