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

    override suspend fun signOut() {
        try {
            supabaseClient.auth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
