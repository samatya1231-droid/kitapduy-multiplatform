package com.semtinazizi.kitapduy.model

import com.semtinazizi.kitapduy.ui.components.getRelativeTime
import kotlin.test.*

class NotificationTimeTest {
    @Test fun parsesSupabaseTimestampWithOffsetAndFraction() {
        val utc = NotificationItem(createdAt = "2026-09-28T09:00:00.123Z")
        val local = NotificationItem(createdAt = "2026-09-28T12:00:00.123+03:00")
        val postgres = NotificationItem(createdAt = "2026-09-28 09:00:00.123+00")
        assertTrue(utc.timestamp > 0)
        assertEquals(utc.timestamp, local.timestamp)
        assertEquals(utc.timestamp, postgres.timestamp)
    }
    @Test fun missingOrMalformedTimeDoesNotPretendToBeRecent() {
        assertEquals("", getRelativeTime(NotificationItem().timestamp))
        assertEquals("", getRelativeTime(NotificationItem(createdAt = "bad").timestamp))
    }
    @Test fun relativeTimeHandlesBoundariesAndClockSkew() {
        val now = 1_000_000_000L
        assertEquals("Az önce", getRelativeTime(now + 10_000, now))
        assertEquals("1 dakika önce", getRelativeTime(now - 60_000, now))
        assertEquals("1 saat önce", getRelativeTime(now - 3_600_000, now))
        assertEquals("1 gün önce", getRelativeTime(now - 86_400_000, now))
    }
}
