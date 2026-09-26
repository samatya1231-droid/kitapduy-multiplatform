package com.semtinazizi.kitapduy.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    var enableNotifications: (() -> Unit)? = null
    var disableNotifications: (() -> Unit)? = null
    var refreshPermission: (() -> Unit)? = null

    fun configureIdentity(handler: (String?) -> Unit) = identity.configure(handler)
    fun updateUser(userId: String?) = identity.updateUser(userId)
    fun updatePermission(enabled: Boolean) { enabledState.value = enabled }
    fun refresh() { refreshPermission?.invoke() }
    fun requestPermission() { enableNotifications?.invoke() }
    fun disable() { disableNotifications?.invoke() }
}
