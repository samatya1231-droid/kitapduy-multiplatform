package com.semtinazizi.kitapduy.data.repository

import com.russhwolf.settings.Settings
import com.semtinazizi.kitapduy.data.AccountDeletionClient
import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.model.SupabasePost
import com.semtinazizi.kitapduy.model.UserProfile
import com.semtinazizi.kitapduy.platform.AppConfig
import com.semtinazizi.kitapduy.platform.PushNotificationBridge
import com.semtinazizi.kitapduy.util.UrlSanitizer
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal class LocalUserStore(
    private val settings: Settings? = runCatching { Settings() }.getOrNull()
) {
    companion object {
        private val memoryFallback = mutableMapOf<String, String>()

        internal fun clearMemoryFallbackForTests() {
            memoryFallback.clear()
        }
    }

    fun getString(key: String): String? {
        return if (settings != null) {
            settings.getStringOrNull(key)
        } else {
            memoryFallback[key]
        }
    }

    fun hasKey(key: String): Boolean {
        return if (settings != null) {
            settings.hasKey(key)
        } else {
            memoryFallback.containsKey(key)
        }
    }

    fun putString(key: String, value: String) {
        if (settings != null) {
            settings.putString(key, value)
        } else {
            memoryFallback[key] = value
        }
    }

    fun remove(key: String) {
        if (settings != null) {
            settings.remove(key)
        } else {
            memoryFallback.remove(key)
        }
    }

    fun clearUser(uid: String) {
        val cleanUid = uid.trim()
        if (cleanUid.isEmpty()) return
        listOf(
            "user_profile_name_$cleanUid",
            "user_profile_bio_$cleanUid",
            "user_profile_username_$cleanUid",
            "user_profile_photo_$cleanUid",
            "user_profile_cover_$cleanUid",
            "blocked_users_$cleanUid",
            "reported_posts_$cleanUid"
        ).forEach(::remove)
    }

    fun getBlockedUsers(blockerUid: String): Set<String> {
        val cleanBlocker = blockerUid.trim()
        if (cleanBlocker.isEmpty()) return emptySet()
        val raw = getString("blocked_users_$cleanBlocker").orEmpty()
        return raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    fun addBlockedUser(blockerUid: String, blockedUid: String) {
        val cleanBlocker = blockerUid.trim()
        val cleanBlocked = blockedUid.trim()
        if (cleanBlocker.isEmpty() || cleanBlocked.isEmpty()) return
        val updated = getBlockedUsers(cleanBlocker) + cleanBlocked
        putString("blocked_users_$cleanBlocker", updated.joinToString(","))
    }

    fun getReportedPosts(reporterUid: String): Set<String> {
        val cleanReporter = reporterUid.trim()
        if (cleanReporter.isEmpty()) return emptySet()
        val raw = getString("reported_posts_$cleanReporter").orEmpty()
        return raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    fun addReportedPost(reporterUid: String, postId: String) {
        val cleanReporter = reporterUid.trim()
        val cleanPost = postId.trim()
        if (cleanReporter.isEmpty() || cleanPost.isEmpty()) return
        val updated = getReportedPosts(cleanReporter) + cleanPost
        putString("reported_posts_$cleanReporter", updated.joinToString(","))
    }
}

private fun JsonObject?.stringOrNull(vararg keys: String): String? {
    if (this == null) return null
    for (key in keys) {
        val value = (this[key] as? JsonPrimitive)?.contentOrNull?.trim()
        if (!value.isNullOrEmpty()) return value
    }
    return null
}

class SupabaseAuthRepository(
    private val supabaseClient: SupabaseClient,
    settings: Settings? = runCatching { Settings() }.getOrNull(),
    private val httpClient: HttpClient? = null
) : AuthRepository {
    private val localStore = LocalUserStore(settings)

    // This repository is an application singleton. Observe UID changes, not just logged-in booleans.
    private val pushScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    init {
        pushScope.launch {
            supabaseClient.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> status.session.user?.id?.let { PushNotificationBridge.updateUser(it) }
                    is SessionStatus.NotAuthenticated -> PushNotificationBridge.updateUser(null)
                    else -> Unit // Initializing/refresh errors do not mean logout.
                }
            }
        }
    }

    private fun nameKey(uid: String) = "user_profile_name_$uid"
    private fun bioKey(uid: String) = "user_profile_bio_$uid"
    private fun usernameKey(uid: String) = "user_profile_username_$uid"
    private fun photoKey(uid: String) = "user_profile_photo_$uid"
    private fun coverKey(uid: String) = "user_profile_cover_$uid"

    private val guestSessionChanges = MutableStateFlow<String?>(localStore.getString("active_guest_uid"))

    override val authenticationChanges: Flow<Boolean?> = combine(
        supabaseClient.auth.sessionStatus,
        guestSessionChanges
    ) { status, guestUid ->
        when {
            guestUid != null -> true
            status is SessionStatus.Authenticated -> status.session.user != null
            status is SessionStatus.NotAuthenticated -> false
            else -> null
        }
    }.distinctUntilChanged()

    private fun fixUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""
        if (url.startsWith("data:image")) return url
        val publicBase = AppConfig.cfPublicUrl.trim().trimEnd('/')
        val defaultBase = if (publicBase.startsWith("http://localhost") || publicBase.startsWith("http://127.0.0.1")) {
            publicBase
        } else {
            UrlSanitizer.CUSTOM_R2_BASE_URL
        }
        return UrlSanitizer.normalizeMediaUrl(url, defaultBase)
    }

    override val currentUserUid: String?
        get() = supabaseClient.auth.currentUserOrNull()?.id ?: localStore.getString("active_guest_uid")

    override val isUserLoggedIn: Boolean
        get() = currentUserUid != null

    override suspend fun awaitSessionInitialization() {
        try {
            supabaseClient.auth.awaitInitialization()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun getUserProfileFlow(uid: String): Flow<UserProfile?> = flow {
        emit(getUserProfile(uid))
    }

    override suspend fun getUserProfile(uid: String): UserProfile? {
        val cleanUid = uid.trim()
        if (cleanUid.isEmpty()) return null

        val remoteProfile = try {
            supabaseClient.postgrest["users"]
                .select { filter { eq("uid", cleanUid) } }
                .decodeSingleOrNull<UserProfile>()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }

        val localName = localStore.getString(nameKey(cleanUid))?.takeIf { it.isNotBlank() }
        val localBio = localStore.getString(bioKey(cleanUid))
        val localUsername = localStore.getString(usernameKey(cleanUid))?.takeIf { it.isNotBlank() }
        val hasLocalPhoto = localStore.hasKey(photoKey(cleanUid))
        val localPhoto = localStore.getString(photoKey(cleanUid))
        val hasLocalCover = localStore.hasKey(coverKey(cleanUid))
        val localCover = localStore.getString(coverKey(cleanUid))

        if (remoteProfile != null) {
            val resolvedPhoto = if (hasLocalPhoto) localPhoto else remoteProfile.photoUrl
            val resolvedCover = if (hasLocalCover) localCover else remoteProfile.coverPhotoUrl
            return remoteProfile.copy(
                name = localName ?: remoteProfile.name,
                bio = localBio ?: remoteProfile.bio,
                username = localUsername ?: remoteProfile.username,
                photoUrl = fixUrl(resolvedPhoto),
                coverPhotoUrl = fixUrl(resolvedCover)
            )
        }

        val currentUser = supabaseClient.auth.currentUserOrNull()
        if (currentUser != null && currentUser.id == cleanUid) {
            val meta = currentUser.userMetadata
            val metaName = meta.stringOrNull("full_name", "name", "display_name")
            val email = currentUser.email?.trim().orEmpty()
            val isAppleRelayEmail = email.endsWith("@privaterelay.appleid.com", ignoreCase = true) ||
                email.endsWith("@appleid.com", ignoreCase = true)
            val emailPrefix = if (isAppleRelayEmail) {
                null
            } else {
                email.substringBefore('@').trim().takeIf { it.isNotBlank() }
            }
            val derivedEmailName = emailPrefix?.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
            val resolvedName = localName ?: metaName ?: derivedEmailName ?: "Okur"
            val resolvedUsername = localUsername
                ?: meta.stringOrNull("user_name", "preferred_username", "username")
                ?: emailPrefix
                ?: "okur_${cleanUid.take(5)}"
            val resolvedBio = localBio ?: "Merhaba, ben bir KitapDuy okuruyum!"
            val resolvedPhoto = if (hasLocalPhoto) localPhoto else meta.stringOrNull("avatar_url", "picture")
            val resolvedCover = if (hasLocalCover) localCover else null

            return UserProfile(
                uid = cleanUid,
                name = resolvedName,
                username = resolvedUsername,
                bio = resolvedBio,
                photoUrl = fixUrl(resolvedPhoto),
                coverPhotoUrl = fixUrl(resolvedCover)
            )
        }

        if (cleanUid == localStore.getString("active_guest_uid")) {
            return UserProfile(
                uid = cleanUid,
                name = localName ?: "Misafir",
                username = localUsername ?: "misafir_${cleanUid.takeLast(5)}",
                bio = localBio ?: "Merhaba!",
                photoUrl = fixUrl(localPhoto),
                coverPhotoUrl = fixUrl(localCover)
            )
        }

        val latestPost = try {
            supabaseClient.postgrest["posts"]
                .select {
                    filter { eq("user_id", cleanUid) }
                    limit(1)
                }
                .decodeList<SupabasePost>()
                .firstOrNull()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            null
        }

        val resolvedName = localName
            ?: latestPost?.userName?.takeIf { it.isNotBlank() }
            ?: "Okur"
        val resolvedUsername = localUsername ?: "okur_${cleanUid.take(5)}"
        val resolvedBio = localBio ?: "Merhaba, ben bir KitapDuy okuruyum!"
        val resolvedPhoto = if (hasLocalPhoto) localPhoto else latestPost?.userPhoto
        val resolvedCover = if (hasLocalCover) localCover else null

        return UserProfile(
            uid = cleanUid,
            name = resolvedName,
            username = resolvedUsername,
            bio = resolvedBio,
            photoUrl = fixUrl(resolvedPhoto),
            coverPhotoUrl = fixUrl(resolvedCover),
            isPremium = latestPost?.isPremium ?: false
        )
    }

    override suspend fun signInWithEmail(email: String, password: String): Boolean {
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()
        return try {
            supabaseClient.auth.signInWith(io.github.jan.supabase.auth.providers.builtin.Email) {
                this.email = cleanEmail
                this.password = cleanPassword
            }
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String, name: String): Boolean {
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()
        val cleanName = name.trim().ifBlank { cleanEmail.substringBefore('@').ifBlank { "Okur" } }
        return try {
            supabaseClient.auth.signUpWith(io.github.jan.supabase.auth.providers.builtin.Email) {
                this.email = cleanEmail
                this.password = cleanPassword
            }
            val uid = supabaseClient.auth.currentUserOrNull()?.id
            if (uid != null) {
                val defaultBio = "Merhaba, ben yeni bir KitapDuy kullanicisiyim!"
                localStore.putString(nameKey(uid), cleanName)
                if (!localStore.hasKey(bioKey(uid))) {
                    localStore.putString(bioKey(uid), defaultBio)
                }
                try {
                    val newUser = UserProfile(uid = uid, name = cleanName, bio = defaultBio)
                    supabaseClient.postgrest["users"].insert(newUser)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun signInWithGoogle(): Boolean {
        return try {
            supabaseClient.auth.signInWith(io.github.jan.supabase.auth.providers.Google)
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
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
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun upsertUserRowIfMissing(cleanUid: String, buildFallback: (UserProfile) -> UserProfile): Boolean {
        val existing = supabaseClient.postgrest["users"]
            .select { filter { eq("uid", cleanUid) }; limit(1) }
            .decodeSingleOrNull<UserProfile>()
        if (existing != null) return false
        val base = getUserProfile(cleanUid) ?: UserProfile(uid = cleanUid)
        supabaseClient.postgrest["users"].upsert(buildFallback(base))
        return true
    }

    override suspend fun updateProfile(uid: String, name: String, bio: String, username: String?): Boolean {
        val cleanUid = uid.trim()
        if (cleanUid.isEmpty()) return false
        val cleanName = name.trim()
        val cleanBio = bio.trim()
        val cleanUsername = username?.trim()
        return try {
            localStore.putString(nameKey(cleanUid), cleanName)
            localStore.putString(bioKey(cleanUid), cleanBio)
            if (cleanUsername != null) {
                localStore.putString(usernameKey(cleanUid), cleanUsername)
            }
            try {
                val inserted = upsertUserRowIfMissing(cleanUid) { base ->
                    base.copy(
                        name = cleanName.ifBlank { base.name },
                        bio = cleanBio,
                        username = cleanUsername ?: base.username
                    )
                }
                if (!inserted) {
                    val updateMap = mapOf("name" to cleanName, "bio" to cleanBio, "username" to cleanUsername)
                    supabaseClient.postgrest["users"].update(updateMap) { filter { eq("uid", cleanUid) } }
                }
                if (cleanName.isNotEmpty()) {
                    supabaseClient.postgrest["posts"].update(mapOf("user_name" to cleanName)) { filter { eq("user_id", cleanUid) } }
                    supabaseClient.postgrest["feed_comments"].update(mapOf("user_name" to cleanName)) { filter { eq("user_id", cleanUid) } }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            CrossPlatformBridgeHelper.syncUserProfileToFirestore(
                httpClient = httpClient,
                uid = cleanUid,
                name = cleanName.takeIf { it.isNotEmpty() },
                username = cleanUsername,
                bio = cleanBio
            )
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun updateCoverPhoto(uid: String, photoUrl: String?): Boolean {
        val cleanUid = uid.trim()
        if (cleanUid.isEmpty() || cleanUid != currentUserUid) return false
        val normalizedCover = photoUrl?.let { fixUrl(it) }?.ifBlank { null }
        return try {
            localStore.putString(coverKey(cleanUid), normalizedCover.orEmpty())
            try {
                val inserted = upsertUserRowIfMissing(cleanUid) { base ->
                    base.copy(coverPhotoUrl = normalizedCover)
                }
                if (!inserted) {
                    supabaseClient.postgrest["users"].update(mapOf("cover_photo_url" to normalizedCover)) {
                        filter { eq("uid", cleanUid) }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            CrossPlatformBridgeHelper.syncUserProfileToFirestore(
                httpClient = httpClient,
                uid = cleanUid,
                coverPhotoUrl = normalizedCover,
                includeCoverPhotoUrl = true
            )
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun updateProfilePhoto(uid: String, photoUrl: String?): Boolean {
        val cleanUid = uid.trim()
        if (cleanUid.isEmpty()) return false
        val normalizedPhoto = photoUrl?.let { fixUrl(it) }?.ifBlank { null }
        return try {
            localStore.putString(photoKey(cleanUid), normalizedPhoto.orEmpty())
            try {
                val inserted = upsertUserRowIfMissing(cleanUid) { base ->
                    base.copy(photoUrl = normalizedPhoto)
                }
                if (!inserted) {
                    val updateMap = mapOf("photo_url" to normalizedPhoto)
                    supabaseClient.postgrest["users"].update(updateMap) { filter { eq("uid", cleanUid) } }
                }
                supabaseClient.postgrest["posts"].update(mapOf("user_photo" to normalizedPhoto)) { filter { eq("user_id", cleanUid) } }
                supabaseClient.postgrest["feed_comments"].update(mapOf("user_photo" to normalizedPhoto)) { filter { eq("user_id", cleanUid) } }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            CrossPlatformBridgeHelper.syncUserProfileToFirestore(
                httpClient = httpClient,
                uid = cleanUid,
                photoUrl = normalizedPhoto,
                includePhotoUrl = true
            )
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
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
                localStore.remove("active_guest_uid")
                guestSessionChanges.value = null
                val safeName = if (name.isBlank()) "Misafir" else name.trim()
                val guestUsername = "misafir_${uid.take(5)}"
                localStore.putString(nameKey(uid), safeName)
                localStore.putString(usernameKey(uid), guestUsername)
                if (!localStore.hasKey(bioKey(uid))) {
                    localStore.putString(bioKey(uid), "Merhaba!")
                }
                try {
                    val profile = UserProfile(uid = uid, name = safeName, username = guestUsername, bio = "Merhaba!")
                    supabaseClient.postgrest["users"].upsert(profile)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
                true
            } else {
                signInAnonymouslyLocalFallback(name)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Throwable) {
            e.printStackTrace()
            signInAnonymouslyLocalFallback(name)
        }
    }

    private fun signInAnonymouslyLocalFallback(name: String): Boolean {
        val guestUid = "guest_" + (100000..999999).random()
        val safeName = if (name.isBlank()) "Misafir" else name.trim()
        val guestUsername = "misafir_${guestUid.takeLast(5)}"
        localStore.putString("active_guest_uid", guestUid)
        localStore.putString(nameKey(guestUid), safeName)
        localStore.putString(usernameKey(guestUid), guestUsername)
        if (!localStore.hasKey(bioKey(guestUid))) {
            localStore.putString(bioKey(guestUid), "Merhaba!")
        }
        guestSessionChanges.value = guestUid
        return true
    }

    override suspend fun signOut() {
        localStore.remove("active_guest_uid")
        guestSessionChanges.value = null
        try {
            supabaseClient.auth.signOut()
        } catch (_: Throwable) {}
        check(currentUserUid == null) { "Çıkış tamamlanamadı. Lütfen tekrar deneyin." }
        PushNotificationBridge.updateUser(null)
    }

    /** Apple Guideline 5.1.1 - Uygulama ici hesap silme */
    override suspend fun deleteAccount(): Boolean {
        val uid = currentUserUid ?: return false
        return try {
            withTimeout(30_000) {
                // Never delete rows piecemeal from the device. The server verifies the JWT
                // and deletes only its owner, including Auth, provider tokens and stored media.
                val token = supabaseClient.auth.currentAccessTokenOrNull() ?: return@withTimeout false
                val confirmed = AccountDeletionClient.delete(uid, token)
                if (confirmed) {
                    localStore.remove("active_guest_uid")
                    guestSessionChanges.value = null
                    localStore.clearUser(uid)
                    supabaseClient.auth.clearSession()
                    PushNotificationBridge.updateUser(null)
                }
                confirmed
            }
        } catch (_: TimeoutCancellationException) {
            false
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Never log JWTs or server bodies. Keep the session until deletion is confirmed.
            false
        }
    }

    /** Engellenen kullanici UID listesi - yerel Settings ve varsa uzak tablo birlestirilir */
    override suspend fun getBlockedUsers(uid: String): List<String> {
        val cleanUid = uid.trim()
        if (cleanUid.isEmpty()) return emptyList()
        val localBlocked = localStore.getBlockedUsers(cleanUid)
        val remoteBlocked = try {
            supabaseClient.postgrest["blocked_users"]
                .select { filter { eq("blocker_uid", cleanUid) } }
                .decodeList<AuthBlockRecord>()
                .map { it.blockedUid }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            emptyList()
        }
        return (localBlocked + remoteBlocked).distinct()
    }
}

@kotlinx.serialization.Serializable
data class AuthBlockRecord(
    @kotlinx.serialization.SerialName("blocked_uid") val blockedUid: String
)
