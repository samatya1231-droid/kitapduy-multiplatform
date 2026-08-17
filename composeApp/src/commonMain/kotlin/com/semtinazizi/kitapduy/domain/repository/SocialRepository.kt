package com.semtinazizi.kitapduy.domain.repository

import com.semtinazizi.kitapduy.model.FeedItem
import com.semtinazizi.kitapduy.model.SupabaseBookshelf
import com.semtinazizi.kitapduy.model.SupabaseFollowRecord
import com.semtinazizi.kitapduy.model.UserProfile

interface SocialRepository {
    suspend fun getUserProfile(userId: String): UserProfile?
    suspend fun searchUsers(query: String): List<UserProfile>
    suspend fun fetchUserPosts(userId: String): List<FeedItem>
    suspend fun fetchUserShelves(userId: String): List<SupabaseBookshelf>
    
    suspend fun getFollowers(userId: String): List<SupabaseFollowRecord>
    suspend fun getFollowing(userId: String): List<SupabaseFollowRecord>
    suspend fun isFollowing(followerId: String, followingId: String): Boolean
    
    suspend fun follow(followerId: String, followingId: String)
    suspend fun unfollow(followerId: String, followingId: String)
    
    suspend fun sendNotification(receiverId: String, message: String, type: String)
}
