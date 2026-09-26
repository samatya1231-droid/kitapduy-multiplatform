package com.semtinazizi.kitapduy.platform

actual object PushNotificationManager {
    actual fun init(context: Any?) {
        PushNotificationBridge.requestPermission()
    }
}
