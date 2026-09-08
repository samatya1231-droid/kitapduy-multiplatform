package com.semtinazizi.kitapduy.data

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccountDeletionClientTest {
    @Test fun acceptsOnlyConfirmedDeletionForTheSignedInUser() {
        assertTrue(AccountDeletionClient.confirmsDeletion(200, """{"deleted":true,"user_id":"alice"}""", "alice"))
    }

    @Test fun rejectsOtherUsersAndPendingDeletion() {
        assertFalse(AccountDeletionClient.confirmsDeletion(200, """{"deleted":true,"user_id":"bob"}""", "alice"))
        assertFalse(AccountDeletionClient.confirmsDeletion(200, """{"deleted":false,"user_id":"alice"}""", "alice"))
        assertFalse(AccountDeletionClient.confirmsDeletion(202, """{"deleted":true,"user_id":"alice"}""", "alice"))
        assertFalse(AccountDeletionClient.confirmsDeletion(500, """{"deleted":true,"user_id":"alice"}""", "alice"))
    }

    @Test fun failsClosedOnInvalidServerResponses() {
        for (body in listOf("", "<html>not found</html>", "null", "[]", "{}",
            """{"deleted":"true","user_id":"alice"}""",
            """{"deleted":true}""", """{"deleted":true,"user_id":null}""")) {
            assertFalse(AccountDeletionClient.confirmsDeletion(200, body, "alice"), body)
        }
        assertFalse(AccountDeletionClient.confirmsDeletion(200, """{"deleted":true,"user_id":""}""", ""))
    }
}
