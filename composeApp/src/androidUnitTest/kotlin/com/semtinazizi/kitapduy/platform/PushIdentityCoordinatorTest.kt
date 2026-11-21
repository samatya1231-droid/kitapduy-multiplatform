package com.semtinazizi.kitapduy.platform

import kotlin.test.*

class PushIdentityCoordinatorTest {
    @Test fun unresolvedSessionDoesNotLogOutSavedIdentity() {
        val calls = mutableListOf<String?>()
        PushIdentityCoordinator().configure { calls.add(it) }
        assertTrue(calls.isEmpty())
    }

    @Test fun restoredSessionReplaysWhenNativeSdkAttaches() {
        val coordinator = PushIdentityCoordinator()
        coordinator.updateUser("user-a")
        val calls = mutableListOf<String?>()
        coordinator.configure { calls.add(it) }
        assertEquals(listOf<String?>("user-a"), calls)
    }

    @Test fun tokenRefreshDoesNotRepeatLoginButAccountSwitchDoes() {
        val coordinator = PushIdentityCoordinator()
        val calls = mutableListOf<String?>()
        coordinator.configure { calls.add(it) }
        coordinator.updateUser("user-a")
        coordinator.updateUser("user-a")
        coordinator.updateUser("user-b")
        coordinator.updateUser(null)
        coordinator.updateUser(null)
        assertEquals(listOf("user-a", "user-b", null), calls)
    }

    @Test fun logoutBeforeSdkSetupDoesNotReplayPreviousUser() {
        val coordinator = PushIdentityCoordinator()
        coordinator.updateUser("user-a")
        coordinator.updateUser(null)
        val calls = mutableListOf<String?>()
        coordinator.configure { calls.add(it) }
        assertEquals(listOf<String?>(null), calls)
    }

    @Test fun bridgeDoesNotClaimEnabledUntilPlatformReportsIt() {
        assertNull(PushNotificationBridge.enabled.value)
        PushNotificationBridge.requestPermission()
        assertNull(PushNotificationBridge.enabled.value)
        PushNotificationBridge.updatePermission(false)
        assertEquals(false, PushNotificationBridge.enabled.value)
        PushNotificationBridge.updatePermission(true)
        assertEquals(true, PushNotificationBridge.enabled.value)
    }

    @Test fun notificationClickBridgeQueuesConsumesAndResolvesRoutes() {
        val initialRev = PushNotificationBridge.incomingNotificationRevision.value
        PushNotificationBridge.onForegroundNotificationReceived()
        assertEquals(initialRev + 1L, PushNotificationBridge.incomingNotificationRevision.value)

        PushNotificationBridge.onNotificationClicked(
            socialType = "FOLLOW",
            postId = "android_user_42",
            bookId = null,
            deepLink = "kitapduy://post/android_user_42"
        )
        val followEvent = assertNotNull(PushNotificationBridge.consumePendingNotification())
        assertNull(PushNotificationBridge.pendingNotification.value)
        assertEquals(PushNotificationRoute.UserProfile("android_user_42"), PushNotificationBridge.resolveRoute(followEvent))

        assertEquals(
            PushNotificationRoute.PostDetail("12345"),
            PushNotificationBridge.resolveRoute(
                PushNotificationEvent(socialType = "COMMENT", postId = "12345", deepLink = "kitapduy://post/12345")
            )
        )
        assertEquals(
            PushNotificationRoute.Book("0mVxipNVFmBxtJNs9O0J"),
            PushNotificationBridge.resolveRoute(
                PushNotificationEvent(socialType = "LIKE", postId = "0mVxipNVFmBxtJNs9O0J")
            )
        )
        assertEquals(
            PushNotificationRoute.Podcast("podcast_111hz_ep1"),
            PushNotificationBridge.resolveRoute(
                PushNotificationEvent(socialType = "REPLY", bookId = "podcast_111hz_ep1")
            )
        )
        assertEquals(
            PushNotificationRoute.Notifications,
            PushNotificationBridge.resolveRoute(PushNotificationEvent(deepLink = "kitapduy://notifications"))
        )
    }
}
