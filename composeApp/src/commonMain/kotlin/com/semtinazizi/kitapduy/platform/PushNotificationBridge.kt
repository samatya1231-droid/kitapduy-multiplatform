package com.semtinazizi.kitapduy.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PushNotificationEvent(
    val socialType: String = "",
    val postId: String = "",
    val bookId: String = "",
    val deepLink: String = ""
)

sealed interface PushNotificationRoute {
    data class UserProfile(val userId: String) : PushNotificationRoute
    data class PostDetail(val postId: String) : PushNotificationRoute
    data class Book(val bookId: String) : PushNotificationRoute
    data class Podcast(val podcastId: String) : PushNotificationRoute
    data object Notifications : PushNotificationRoute
}

/** Used on the main thread. Unknown/restoring sessions must not log out a saved identity. */
class PushIdentityCoordinator {
    private var resolved = false
    private var userId: String? = null
    private var handler: ((String?) -> Unit)? = null

    fun configure(handler: (String?) -> Unit) {
        this.handler = handler
        if (resolved) handler(userId)
    }

    fun updateUser(userId: String?) {
        val normalized = userId?.takeIf { it.isNotBlank() }
        if (resolved && this.userId == normalized) return
        resolved = true
        this.userId = normalized
        handler?.invoke(normalized)
    }
}

/** Swift owns the SDK; the shared UI only observes actual permission/subscription state. */
object PushNotificationBridge {
    private val identity = PushIdentityCoordinator()
    private val enabledState = MutableStateFlow<Boolean?>(null)
    val enabled = enabledState.asStateFlow()
    private val pendingNotificationState = MutableStateFlow<PushNotificationEvent?>(null)
    val pendingNotification = pendingNotificationState.asStateFlow()
    private val incomingNotificationRevisionState = MutableStateFlow(0L)
    val incomingNotificationRevision = incomingNotificationRevisionState.asStateFlow()

    var requestInitialNotifications: (() -> Unit)? = null
    var enableNotifications: (() -> Unit)? = null
    var disableNotifications: (() -> Unit)? = null
    var refreshPermission: (() -> Unit)? = null

    fun configureIdentity(handler: (String?) -> Unit) = identity.configure(handler)
    fun updateUser(userId: String?) = identity.updateUser(userId)
    fun updatePermission(enabled: Boolean) { enabledState.value = enabled }
    fun refresh() { refreshPermission?.invoke() }
    fun requestInitialPermission() { requestInitialNotifications?.invoke() }
    fun requestPermission() { enableNotifications?.invoke() }
    fun disable() { disableNotifications?.invoke() }

    fun onForegroundNotificationReceived() {
        incomingNotificationRevisionState.value = incomingNotificationRevisionState.value + 1L
    }

    fun onNotificationClicked(
        socialType: String?,
        postId: String?,
        bookId: String?,
        deepLink: String?
    ) {
        pendingNotificationState.value = PushNotificationEvent(
            socialType = socialType.orEmpty().trim(),
            postId = postId.orEmpty().trim(),
            bookId = bookId.orEmpty().trim(),
            deepLink = deepLink.orEmpty().trim()
        )
    }

    fun consumePendingNotification(): PushNotificationEvent? {
        val current = pendingNotificationState.value
        pendingNotificationState.value = null
        return current
    }

    private fun extractDeepLinkSegment(deepLink: String, prefix: String): String {
        val idx = deepLink.indexOf(prefix, ignoreCase = true)
        if (idx < 0) return ""
        val raw = deepLink.substring(idx + prefix.length)
            .substringBefore('?')
            .substringBefore('#')
            .substringBefore('/')
            .trim()
        return decodeUriComponent(raw)
    }

    internal fun decodeUriComponent(value: String): String {
        if (!value.contains('%')) return value
        val bytes = ByteArray(value.encodeToByteArray().size)
        var outIdx = 0
        var i = 0
        while (i < value.length) {
            if (value[i] == '%' && i + 2 < value.length) {
                val hi = value[i + 1].digitToIntOrNull(16)
                val lo = value[i + 2].digitToIntOrNull(16)
                if (hi != null && lo != null) {
                    bytes[outIdx++] = ((hi shl 4) or lo).toByte()
                    i += 3
                    continue
                }
            }
            val encoded = value[i].toString().encodeToByteArray()
            encoded.copyInto(bytes, outIdx)
            outIdx += encoded.size
            i++
        }
        return runCatching { bytes.decodeToString(0, outIdx) }.getOrDefault(value)
    }

    fun resolveRoute(event: PushNotificationEvent): PushNotificationRoute {
        val normalizedType = event.socialType.trim().lowercase()
        val deepLink = event.deepLink.trim()
        if (normalizedType == "chat_like" || normalizedType == "invite" || normalizedType == "evening_reminder") {
            return PushNotificationRoute.Notifications
        }
        if (deepLink.equals("kitapduy://notifications", ignoreCase = true) ||
            deepLink.startsWith("kitapduy://notifications?", ignoreCase = true)
        ) {
            return PushNotificationRoute.Notifications
        }

        val deepLinkUser = extractDeepLinkSegment(deepLink, "kitapduy://user/")
        val deepLinkPost = extractDeepLinkSegment(deepLink, "kitapduy://post/")
        val deepLinkBook = extractDeepLinkSegment(deepLink, "kitapduy://book/")
        val deepLinkPodcast = extractDeepLinkSegment(deepLink, "kitapduy://podcast/")
        val targetId = event.postId.trim()
            .ifBlank { event.bookId.trim() }
            .ifBlank { deepLinkPost }
            .ifBlank { deepLinkPodcast }
            .ifBlank { deepLinkBook }
            .ifBlank { deepLinkUser }

        if (normalizedType == "follow" || deepLinkUser.isNotBlank()) {
            val uid = deepLinkUser.ifBlank { targetId }
            return if (uid.isNotBlank()) PushNotificationRoute.UserProfile(uid) else PushNotificationRoute.Notifications
        }
        if (deepLinkPodcast.isNotBlank()) {
            return PushNotificationRoute.Podcast(deepLinkPodcast)
        }
        if (targetId.isBlank()) return PushNotificationRoute.Notifications
        if (targetId.startsWith("podcast_")) return PushNotificationRoute.Podcast(targetId)
        if (normalizedType == "book" || normalizedType == "new_book" || deepLinkBook.isNotBlank()) {
            val resolvedBookId = deepLinkBook.ifBlank { targetId }
            return PushNotificationRoute.Book(resolvedBookId)
        }
        return if (targetId.toLongOrNull() != null) {
            PushNotificationRoute.PostDetail(targetId)
        } else {
            PushNotificationRoute.Book(targetId)
        }
    }
}
