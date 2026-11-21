package com.semtinazizi.kitapduy.data.repository

import com.russhwolf.settings.Settings
import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.domain.repository.SocialRepository
import com.semtinazizi.kitapduy.model.FeedItem
import com.semtinazizi.kitapduy.model.FeedType
import com.semtinazizi.kitapduy.model.SupabaseBookshelf
import com.semtinazizi.kitapduy.model.SupabaseFollowRecord
import com.semtinazizi.kitapduy.model.SupabaseInsertNotification
import com.semtinazizi.kitapduy.model.SupabasePost
import com.semtinazizi.kitapduy.model.UserProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import com.semtinazizi.kitapduy.platform.AppConfig
import com.semtinazizi.kitapduy.util.UrlSanitizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class SupabaseSocialRepository(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository,
    settings: Settings? = runCatching { Settings() }.getOrNull(),
    private val httpClient: io.ktor.client.HttpClient? = null
) : SocialRepository {
    private val localStore = LocalUserStore(settings)

    private fun fixUrl(url: String?): String {
        val publicBase = AppConfig.cfPublicUrl.trim().trimEnd('/')
        val defaultBase = if (publicBase.startsWith("http://localhost") || publicBase.startsWith("http://127.0.0.1")) {
            publicBase
        } else {
            UrlSanitizer.CUSTOM_R2_BASE_URL
        }
        return UrlSanitizer.normalizeMediaUrl(url, defaultBase)
    }

    override suspend fun getUserProfile(userId: String): UserProfile? {
        val cleanUid = userId.trim()
        if (cleanUid.isEmpty()) return null
        val isCurrentUser = cleanUid == authRepository.currentUserUid
        return coroutineScope {
            val remoteDeferred = async {
                try {
                    supabaseClient.postgrest["users"]
                        .select {
                            filter { eq("uid", cleanUid) }
                            limit(1)
                        }
                        .decodeSingleOrNull<UserProfile>()
                        ?.let {
                            it.copy(
                                photoUrl = fixUrl(it.photoUrl),
                                coverPhotoUrl = fixUrl(it.coverPhotoUrl)
                            )
                        }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Throwable) {
                    null
                }
            }
            val bridgeDeferred = async {
                CrossPlatformBridgeHelper.fetchBridgeUserProfile(httpClient, cleanUid)
            }

            val remoteProfile = remoteDeferred.await()
            val bridgeProfile = bridgeDeferred.await()
            val mergedRemote = mergeUserProfiles(remoteProfile, bridgeProfile)

            if (isCurrentUser) {
                val authProfile = runCatching { authRepository.getUserProfile(cleanUid) }.getOrNull()
                if (authProfile != null) {
                    return@coroutineScope mergeUserProfiles(authProfile, mergedRemote)
                }
            }

            if (mergedRemote != null) {
                return@coroutineScope mergedRemote
            }

            try {
                val post = supabaseClient.postgrest["posts"]
                    .select {
                        filter { eq("user_id", cleanUid) }
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        limit(1)
                    }
                    .decodeList<SupabasePost>()
                    .firstOrNull() ?: return@coroutineScope null
                UserProfile(
                    uid = cleanUid,
                    name = post.userName?.takeIf { it.isNotBlank() } ?: "KitapDuy Okuru",
                    username = "okur_${cleanUid.take(5)}",
                    bio = "Merhaba, ben bir KitapDuy okuruyum!",
                    photoUrl = fixUrl(post.userPhoto),
                    isPremium = post.isPremium ?: false
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                null
            }
        }
    }

    override suspend fun searchUsers(query: String): List<UserProfile> {
        val cleanQuery = query.trim().trimStart('@').trim()
        if (cleanQuery.isEmpty()) return emptyList()
        val blocked = authRepository.currentUserUid?.let { authRepository.getBlockedUsers(it).toSet() }.orEmpty()

        return coroutineScope {
            val remoteDeferred = async {
                try {
                    supabaseClient.postgrest["users"]
                        .select {
                            filter {
                                or {
                                    ilike("name", "%$cleanQuery%")
                                    ilike("username", "%$cleanQuery%")
                                }
                            }
                            limit(30)
                        }
                        .decodeList<UserProfile>()
                        .filter { it.uid.isNotBlank() && it.uid !in blocked }
                        .map {
                            it.copy(
                                photoUrl = fixUrl(it.photoUrl),
                                coverPhotoUrl = fixUrl(it.coverPhotoUrl)
                            )
                        }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Throwable) {
                    emptyList()
                }
            }

            val postsDeferred = async {
                try {
                    supabaseClient.postgrest["posts"]
                        .select {
                            filter {
                                ilike("user_name", "%$cleanQuery%")
                            }
                            order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                            limit(60)
                        }
                        .decodeList<SupabasePost>()
                        .filter { !it.userId.isNullOrBlank() && it.userId !in blocked }
                        .distinctBy { it.userId }
                        .map { post ->
                            val uid = post.userId!!.trim()
                            UserProfile(
                                uid = uid,
                                name = post.userName?.takeIf { it.isNotBlank() } ?: "Okur",
                                username = "okur_${uid.take(5)}",
                                bio = "Merhaba, ben bir KitapDuy okuruyum!",
                                photoUrl = fixUrl(post.userPhoto),
                                isPremium = post.isPremium ?: false
                            )
                        }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Throwable) {
                    emptyList()
                }
            }

            val bridgeDeferred = async {
                CrossPlatformBridgeHelper.searchBridgeUsers(httpClient, cleanQuery)
                    .filter { it.uid.isNotBlank() && it.uid !in blocked }
            }

            mergeUserSearchResults(
                query = cleanQuery,
                remoteUsers = remoteDeferred.await(),
                bridgeUsers = bridgeDeferred.await(),
                postAuthors = postsDeferred.await()
            )
        }
    }

    internal companion object {
        private val SYNTHETIC_BIOS = setOf(
            "Merhaba, ben bir KitapDuy okuruyum!",
            "Merhaba!"
        )

        private val GENERIC_NAMES = setOf(
            "Okur",
            "KitapDuy Okuru"
        )

        internal fun normalizeSearchText(raw: String): String {
            val sb = StringBuilder(raw.length)
            var lastWasSpace = true
            for (ch in raw.trim().trimStart('@')) {
                val mapped = when (ch) {
                    'İ', 'I', 'ı', 'î', 'Î' -> 'i'
                    'Ç', 'ç' -> 'c'
                    'Ğ', 'ğ' -> 'g'
                    'Ö', 'ö' -> 'o'
                    'Ş', 'ş' -> 's'
                    'Ü', 'ü' -> 'u'
                    'Â', 'â' -> 'a'
                    'Û', 'û' -> 'u'
                    '\u0307' -> continue
                    else -> ch.lowercaseChar()
                }
                if (mapped.isWhitespace()) {
                    if (!lastWasSpace) {
                        sb.append(' ')
                        lastWasSpace = true
                    }
                } else {
                    sb.append(mapped)
                    lastWasSpace = false
                }
            }
            return sb.toString().trim()
        }

        internal fun mergeUserProfiles(primary: UserProfile?, secondary: UserProfile?): UserProfile? {
            if (primary == null) return secondary
            if (secondary == null) return primary
            val primaryName = primary.name.trim()
            val secondaryName = secondary.name.trim()
            val resolvedName = when {
                primaryName.isNotBlank() && primaryName !in GENERIC_NAMES -> primaryName
                secondaryName.isNotBlank() && secondaryName !in GENERIC_NAMES -> secondaryName
                primaryName.isNotBlank() -> primaryName
                secondaryName.isNotBlank() -> secondaryName
                else -> "KitapDuy Okuru"
            }
            val primaryBio = primary.bio.trim()
            val secondaryBio = secondary.bio.trim()
            val resolvedBio = when {
                primaryBio.isNotBlank() && primaryBio !in SYNTHETIC_BIOS -> primaryBio
                secondaryBio.isNotBlank() && secondaryBio !in SYNTHETIC_BIOS -> secondaryBio
                primaryBio.isNotBlank() -> primaryBio
                else -> secondaryBio
            }
            val mergedBadges = (primary.badges + secondary.badges)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
            return primary.copy(
                name = resolvedName,
                username = primary.username?.takeIf { it.isNotBlank() } ?: secondary.username?.takeIf { it.isNotBlank() },
                bio = resolvedBio,
                photoUrl = primary.photoUrl?.takeIf { it.isNotBlank() } ?: secondary.photoUrl?.takeIf { it.isNotBlank() },
                coverPhotoUrl = primary.coverPhotoUrl?.takeIf { it.isNotBlank() } ?: secondary.coverPhotoUrl?.takeIf { it.isNotBlank() },
                badges = mergedBadges,
                premiumUntil = maxOf(primary.premiumUntil, secondary.premiumUntil),
                isPremium = primary.isPremium || secondary.isPremium,
                streak = maxOf(primary.streak, secondary.streak)
            )
        }

        fun mergeUserSearchResults(
            query: String,
            remoteUsers: List<UserProfile>,
            bridgeUsers: List<UserProfile>,
            postAuthors: List<UserProfile>
        ): List<UserProfile> {
            val mergedByUid = linkedMapOf<String, UserProfile>()
            // Priority: Supabase users -> Cross-platform bridge (Firestore/Android) -> Posts authors
            for (profile in remoteUsers) {
                if (profile.uid.isNotBlank()) mergedByUid[profile.uid] = profile
            }
            for (profile in bridgeUsers) {
                if (profile.uid.isBlank()) continue
                val existing = mergedByUid[profile.uid]
                mergedByUid[profile.uid] = mergeUserProfiles(existing, profile) ?: profile
            }
            for (author in postAuthors) {
                if (author.uid.isBlank()) continue
                val existing = mergedByUid[author.uid]
                if (existing == null) {
                    mergedByUid[author.uid] = author
                } else {
                    mergedByUid[author.uid] = mergeUserProfiles(existing, author) ?: existing
                }
            }
            val folded = normalizeSearchText(query)
            return mergedByUid.values.sortedWith(
                compareByDescending<UserProfile> {
                    folded.isNotEmpty() && (
                        normalizeSearchText(it.name).startsWith(folded) ||
                            normalizeSearchText(it.username.orEmpty()).startsWith(folded)
                        )
                }.thenByDescending { it.hasPremium() }
                    .thenBy { normalizeSearchText(it.name) }
            )
        }
    }

    override suspend fun fetchUserPosts(userId: String): List<FeedItem> {
        return try {
            val posts = supabaseClient.postgrest["posts"]
                .select {
                    filter { eq("user_id", userId) }
                    // Order cannot easily be parsed without raw string sometimes, but let's assume descending id
                }
                .decodeList<SupabasePost>()

            val currentUid = authRepository.currentUserUid
            val blocked = currentUid?.let { authRepository.getBlockedUsers(it).toSet() }.orEmpty()
            val reported = currentUid?.let { localStore.getReportedPosts(it) }.orEmpty()
            posts
                .filter { it.userId !in blocked && (it.id?.toString() ?: "") !in reported }
                .map(::mapSupabasePostToFeedItem)
                .sortedByDescending { it.id.toLongOrNull() ?: 0L }
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun fetchUserShelves(userId: String): List<SupabaseBookshelf> {
        return try {
            supabaseClient.postgrest["user_bookshelves"]
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<SupabaseBookshelf>()
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun getFollowers(userId: String): List<SupabaseFollowRecord> {
        return try {
            supabaseClient.postgrest["followers"]
                .select { filter { eq("following_id", userId) } }
                .decodeList<SupabaseFollowRecord>()
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun getFollowing(userId: String): List<SupabaseFollowRecord> {
        return try {
            supabaseClient.postgrest["followers"]
                .select { filter { eq("follower_id", userId) } }
                .decodeList<SupabaseFollowRecord>()
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun isFollowing(followerId: String, followingId: String): Boolean {
        return try {
            val result = supabaseClient.postgrest["followers"]
                .select {
                    filter {
                        eq("follower_id", followerId)
                        eq("following_id", followingId)
                    }
                }
                .decodeList<SupabaseFollowRecord>()
            result.isNotEmpty()
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun follow(followerId: String, followingId: String) {
        try {
            supabaseClient.postgrest["followers"].insert(SupabaseFollowRecord(followerId, followingId))
            val senderName = runCatching { authRepository.getUserProfile(followerId)?.name }
                .getOrNull()?.takeIf { it.isNotBlank() } ?: "Bir okur"
            CrossPlatformBridgeHelper.sendSocialNotification(
                supabaseClient = supabaseClient,
                httpClient = httpClient,
                receiverId = followingId,
                senderId = followerId,
                senderName = senderName,
                type = "follow",
                message = "$senderName seni takip etmeye başladı! 👋",
                targetId = followerId
            )
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun unfollow(followerId: String, followingId: String) {
        try {
            supabaseClient.postgrest["followers"].delete {
                filter {
                    eq("follower_id", followerId)
                    eq("following_id", followingId)
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun sendNotification(receiverId: String, message: String, type: String) {
        try {
            val senderId = authRepository.currentUserUid.orEmpty()
            val senderName = senderId.takeIf { it.isNotBlank() }
                ?.let { runCatching { authRepository.getUserProfile(it)?.name }.getOrNull() }
                ?.takeIf { it.isNotBlank() } ?: "Bir okur"
            CrossPlatformBridgeHelper.sendSocialNotification(
                supabaseClient = supabaseClient,
                httpClient = httpClient,
                receiverId = receiverId,
                senderId = senderId,
                senderName = senderName,
                type = type,
                message = message,
                targetId = if (type.trim().equals("follow", ignoreCase = true)) senderId.takeIf { it.isNotBlank() } else null
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Throwable) {
            println("Social Notification Error: ${e.message}")
        }
    }

    /** Apple Guideline 1.2 - Icerik raporlama */
    override suspend fun reportPost(postId: String, reporterId: String, reason: String): Boolean {
        val cleanPostId = postId.trim()
        val cleanReporterId = reporterId.trim()
        val cleanReason = reason.trim()
        if (cleanPostId.isEmpty() || cleanReporterId.isEmpty() || cleanReason.isEmpty()) return false
        localStore.addReportedPost(cleanReporterId, cleanPostId)
        return try {
            try {
                supabaseClient.postgrest["reports"].insert(ReportRecord(cleanPostId, cleanReporterId, cleanReason))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Throwable) {
                println("Report Error (saved locally): ${e.message}")
            }
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            false
        }
    }

    /** Apple Guideline 1.2 - Kullanici engelleme */
    override suspend fun blockUser(blockerId: String, blockedUid: String): Boolean {
        val cleanBlocker = blockerId.trim()
        val cleanBlocked = blockedUid.trim()
        if (cleanBlocker.isEmpty() || cleanBlocked.isEmpty() || cleanBlocker == cleanBlocked) return false
        localStore.addBlockedUser(cleanBlocker, cleanBlocked)
        return try {
            try {
                supabaseClient.postgrest["blocked_users"].upsert(SocialBlockRecord(cleanBlocker, cleanBlocked))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Throwable) {
                println("Block Error (saved locally): ${e.message}")
            }
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            false
        }
    }
}

@kotlinx.serialization.Serializable
data class ReportRecord(
    @kotlinx.serialization.SerialName("post_id") val postId: String,
    @kotlinx.serialization.SerialName("reporter_id") val reporterId: String,
    @kotlinx.serialization.SerialName("reason") val reason: String
)

@kotlinx.serialization.Serializable
data class SocialBlockRecord(
    @kotlinx.serialization.SerialName("blocker_id") val blockerId: String,
    @kotlinx.serialization.SerialName("blocked_id") val blockedId: String
)

