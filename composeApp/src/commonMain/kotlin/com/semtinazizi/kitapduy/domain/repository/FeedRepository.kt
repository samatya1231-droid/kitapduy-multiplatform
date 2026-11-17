package com.semtinazizi.kitapduy.domain.repository

import com.semtinazizi.kitapduy.model.FeedItem
import com.semtinazizi.kitapduy.model.FeedType

interface FeedRepository {
    suspend fun getFeedItems(excludeListen: Boolean = false, limit: Long = 50, offset: Long = 0): List<FeedItem>
    suspend fun getListeningFeedItems(limit: Long = 50, offset: Long = 0): List<FeedItem> =
        getFeedItems(excludeListen = false, limit = limit, offset = offset).filter { it.type == FeedType.LISTEN }
    suspend fun createPost(
        userId: String,
        userName: String,
        userPhoto: String?,
        bookId: String,
        bookName: String,
        comment: String,
        isPremium: Boolean,
        imageUrl: String?,
        audioUrl: String?,
        pollOptions: List<String>? = null
    ): Boolean
    suspend fun createQuotePost(
        userId: String,
        userName: String,
        userPhoto: String?,
        bookId: String,
        bookName: String,
        comment: String,
        isPremium: Boolean,
        imageUrl: String?,
        audioUrl: String?,
        startPos: Long?
    ): Boolean = createPost(
        userId = userId,
        userName = userName,
        userPhoto = userPhoto,
        bookId = bookId,
        bookName = bookName,
        comment = comment,
        isPremium = isPremium,
        imageUrl = imageUrl,
        audioUrl = audioUrl,
        pollOptions = null
    )
    fun startRealtimeListening(onNewPost: (FeedItem) -> Unit)
    fun stopRealtimeListening()
}
