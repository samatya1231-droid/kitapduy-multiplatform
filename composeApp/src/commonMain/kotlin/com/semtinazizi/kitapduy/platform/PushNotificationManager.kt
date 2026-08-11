package com.semtinazizi.kitapduy.platform

/**
 * Ortak Push Bildirim yöneticisi.
 * Android'de Application Context, iOS'ta null bekleyebilir.
 */
expect object PushNotificationManager {
    fun init(context: Any? = null)
}
