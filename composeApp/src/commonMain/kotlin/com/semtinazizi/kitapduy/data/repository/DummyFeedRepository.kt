package com.semtinazizi.kitapduy.data.repository

import com.semtinazizi.kitapduy.domain.repository.FeedRepository
import com.semtinazizi.kitapduy.model.FeedItem

class DummyFeedRepository : FeedRepository {
    override suspend fun getFeedItems(excludeListen: Boolean, limit: Long, offset: Long): List<FeedItem> {
        return emptyList()
    }

    override suspend fun createPost(
        userId: String,
        userName: String,
        userPhoto: String?,
        bookId: String,
        bookName: String,
        comment: String,
        isPremium: Boolean,
        imageUrl: String?,
        audioUrl: String?,
        pollOptions: List<String>?
    ): Boolean {
        return true
    }
    
    override fun startRealtimeListening(onNewPost: (FeedItem) -> Unit) {}
    override fun stopRealtimeListening() {}
}
