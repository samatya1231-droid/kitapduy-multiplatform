package com.semtinazizi.kitapduy.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.model.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import android.util.Log

class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : AuthRepository {

    override val currentUserUid: String?
        get() = auth.currentUser?.uid

    override val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    override fun getUserProfileFlow(uid: String): Flow<UserProfile?> = callbackFlow {
        if (uid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = db.collection("users").document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirebaseAuthRepo", "User profile listen failed", error)
                trySend(null)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                try {
                    val profile = UserProfile(
                        uid = snapshot.id,
                        name = snapshot.getString("name") ?: "",
                        photoUrl = snapshot.getString("photoUrl"),
                        coverPhotoUrl = snapshot.getString("coverPhotoUrl"),
                        badges = snapshot.get("badges") as? List<String> ?: emptyList(),
                        myReferralCode = snapshot.getString("myReferralCode") ?: "",
                        referredBy = snapshot.getString("referredBy"),
                        premiumUntil = snapshot.getLong("premiumUntil") ?: 0L,
                        successfulInvites = snapshot.getLong("successfulInvites")?.toInt() ?: 0,
                        bio = snapshot.getString("bio") ?: "",
                        username = snapshot.getString("username"),
                        streak = snapshot.getLong("streak")?.toInt() ?: 0,
                        isPremium = snapshot.getBoolean("isPremium") ?: false
                    )
                    trySend(profile)
                } catch (e: Exception) {
                    Log.e("FirebaseAuthRepo", "User profile parse error", e)
                    trySend(null)
                }
            } else {
                trySend(null)
            }
        }

        awaitClose { listener.remove() }
    }

    override suspend fun getUserProfile(uid: String): UserProfile? {
        if (uid.isBlank()) return null
        return try {
            val snapshot = db.collection("users").document(uid).get().await()
            if (snapshot.exists()) {
                UserProfile(
                    uid = snapshot.id,
                    name = snapshot.getString("name") ?: "",
                    photoUrl = snapshot.getString("photoUrl"),
                    coverPhotoUrl = snapshot.getString("coverPhotoUrl"),
                    badges = snapshot.get("badges") as? List<String> ?: emptyList(),
                    myReferralCode = snapshot.getString("myReferralCode") ?: "",
                    referredBy = snapshot.getString("referredBy"),
                    premiumUntil = snapshot.getLong("premiumUntil") ?: 0L,
                    successfulInvites = snapshot.getLong("successfulInvites")?.toInt() ?: 0,
                    bio = snapshot.getString("bio") ?: "",
                    username = snapshot.getString("username"),
                    streak = snapshot.getLong("streak")?.toInt() ?: 0,
                    isPremium = snapshot.getBoolean("isPremium") ?: false
                )
            } else null
        } catch (e: Exception) {
            Log.e("FirebaseAuthRepo", "Failed to get user profile", e)
            null
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }
}
