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
}
