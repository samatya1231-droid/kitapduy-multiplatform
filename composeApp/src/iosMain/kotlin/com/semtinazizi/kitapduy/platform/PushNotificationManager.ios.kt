package com.semtinazizi.kitapduy.platform

import platform.UserNotifications.*
import platform.UIKit.UIApplication
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual object PushNotificationManager {
    actual fun init(context: Any?) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        
        // Push notification izinlerini iste
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        ) { granted, error ->
            if (granted) {
                // İzin verildiyse APNs cihaz tokenı almak için kaydol
                dispatch_async(dispatch_get_main_queue()) {
                    // UIApplication.sharedApplication.registerForRemoteNotifications()
                }
            } else {
                println("Push Notification izni reddedildi: $error")
            }
        }
    }
}
