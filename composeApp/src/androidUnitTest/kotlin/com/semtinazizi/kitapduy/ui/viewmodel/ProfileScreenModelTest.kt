package com.semtinazizi.kitapduy.ui.viewmodel

import com.semtinazizi.kitapduy.domain.repository.*
import com.semtinazizi.kitapduy.model.*
import java.lang.reflect.Proxy
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileScreenModelTest {
    private fun auth(uid: String? = "me") = Proxy.newProxyInstance(AuthRepository::class.java.classLoader,
        arrayOf(AuthRepository::class.java)) { _, method, _ ->
        if (method.name == "getCurrentUserUid") uid else null
    } as AuthRepository
    private fun books() = Proxy.newProxyInstance(BookRepository::class.java.classLoader,
        arrayOf(BookRepository::class.java)) { _, method, _ ->
        if (method.name == "getBooks") listOf(Book(id = "book", author = "Yazar", category = "Roman")) else null
    } as BookRepository

    @Test fun loggedOutProfileRequiresLoginInsteadOfFakeUser() = runTest {
        val model = ProfileScreenModel(auth(null), ProfileSocial(), books(), backgroundScope)
        model.loadProfileData(null)
        assertNull(model.targetUserProfile.value)
        assertNotNull(model.error.value)
        assertFalse(model.isLoading.value)
    }
    @Test fun shelfTypesArePreservedAndDnaCountsUniqueBooks() = runTest {
        val social = ProfileSocial().apply { shelves = listOf(
            SupabaseBookshelf(userId = "me", bookId = "book", shelfType = "READING"),
            SupabaseBookshelf(userId = "me", bookId = "book", shelfType = "FINISHED")) }
        val model = ProfileScreenModel(auth(), social, books(), backgroundScope)
        model.loadProfileData(null); runCurrent()
        assertEquals(2, model.userShelves.value.size)
        assertEquals(mapOf("Roman" to 1), model.readingDnaCategories.value)
        assertNull(model.error.value)
    }
    @Test fun failedProfileDataIsNotDisplayedAsZeroStatistics() = runTest {
        val social = ProfileSocial().apply { fail = true }
        val model = ProfileScreenModel(auth(), social, books(), backgroundScope)
        model.loadProfileData(null); runCurrent()
        assertNull(model.targetUserProfile.value)
        assertNotNull(model.error.value)
        assertFalse(model.isLoading.value)
    }
    @Test fun newerProfileCancelsPreviousRequest() = runTest {
        val social = ProfileSocial().apply { gate = CompletableDeferred() }
        val model = ProfileScreenModel(auth(), social, books(), backgroundScope)
        model.loadProfileData("old"); runCurrent()
        social.gate = null
        model.loadProfileData("new"); runCurrent()
        assertEquals("new", model.targetUserProfile.value?.uid)
        assertNull(model.error.value)
        assertFalse(model.isLoading.value)
    }
    @Test fun failedFollowDoesNotPretendToSucceed() = runTest {
        val social = ProfileSocial().apply { fail = true }
        val model = ProfileScreenModel(auth(), social, books(), backgroundScope)
        model.toggleFollow("other"); runCurrent()
        assertFalse(model.isFollowing.value)
        assertNotNull(model.error.value)
        assertFalse(model.followBusy.value)
    }
    @Test fun followListFailureHasRetryState() = runTest {
        val social = ProfileSocial().apply { fail = true }
        val model = ProfileScreenModel(auth(), social, books(), backgroundScope)
        model.fetchFollowList("me", true); runCurrent()
        assertNotNull(model.followListError.value)
        assertFalse(model.followListLoading.value)
    }
}

private class ProfileSocial : SocialRepository {
    var fail = false
    var gate: CompletableDeferred<Unit>? = null
    var shelves = emptyList<SupabaseBookshelf>()
    override suspend fun getUserProfile(userId: String): UserProfile { gate?.await(); return UserProfile(uid = userId, name = "Okur") }
    override suspend fun fetchUserPosts(userId: String): List<FeedItem> { if (fail) error("Offline"); return emptyList() }
    override suspend fun fetchUserShelves(userId: String) = shelves
    override suspend fun getFollowers(userId: String): List<SupabaseFollowRecord> { if (fail) error("Offline"); return emptyList() }
    override suspend fun getFollowing(userId: String) = emptyList<SupabaseFollowRecord>()
    override suspend fun isFollowing(followerId: String, followingId: String) = false
    override suspend fun follow(followerId: String, followingId: String) { if (fail) error("Offline") }
    override suspend fun unfollow(followerId: String, followingId: String) { if (fail) error("Offline") }
    override suspend fun searchUsers(query: String) = emptyList<UserProfile>()
    override suspend fun sendNotification(receiverId: String, message: String, type: String) {}
    override suspend fun reportPost(postId: String, reporterId: String, reason: String) = false
    override suspend fun blockUser(blockerId: String, blockedUid: String) = false
}
