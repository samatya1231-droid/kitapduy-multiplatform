package com.semtinazizi.kitapduy.domain.repository

import com.semtinazizi.kitapduy.model.FeedItem

interface FeedRepository {
    suspend fun getFeedItems(excludeListen: Boolean = false): List<FeedItem>
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
    fun startRealtimeListening(onNewPost: (FeedItem) -> Unit)
    fun stopRealtimeListening()
}
