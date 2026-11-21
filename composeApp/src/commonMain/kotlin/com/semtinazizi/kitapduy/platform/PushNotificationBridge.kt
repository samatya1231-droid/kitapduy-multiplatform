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

    fun resolveRoute(event: PushNotificationEvent): PushNotificationRoute {
        val normalizedType = event.socialType.trim().lowercase()
        val deepLink = event.deepLink.trim()
        val deepLinkUser = deepLink.substringAfter("kitapduy://user/", "")
            .substringBefore('?').substringBefore('/').trim()
        val deepLinkPost = deepLink.substringAfter("kitapduy://post/", "")
            .substringBefore('?').substringBefore('/').trim()
        val deepLinkBook = deepLink.substringAfter("kitapduy://book/", "")
            .substringBefore('?').substringBefore('/').trim()
        val targetId = event.postId.trim()
            .ifBlank { event.bookId.trim() }
            .ifBlank { deepLinkPost }
            .ifBlank { deepLinkBook }
            .ifBlank { deepLinkUser }

        if (normalizedType == "follow" || deepLinkUser.isNotBlank()) {
            val uid = targetId.ifBlank { deepLinkUser }
            return if (uid.isNotBlank()) PushNotificationRoute.UserProfile(uid) else PushNotificationRoute.Notifications
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
