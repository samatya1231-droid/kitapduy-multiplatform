package com.semtinazizi.kitapduy.platform

import android.content.Context
import com.onesignal.OneSignal

actual object PushNotificationManager {
    actual fun init(context: Any?) {
        val androidContext = context as? Context ?: return
        
        // OneSignal Initialization
        OneSignal.initWithContext(androidContext, "4bc4bc20-ea45-4c6f-9b22-e2dfca6beac5")
    }
}
