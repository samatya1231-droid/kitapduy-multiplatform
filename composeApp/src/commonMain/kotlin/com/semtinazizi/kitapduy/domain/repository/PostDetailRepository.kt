package com.semtinazizi.kitapduy.domain.repository

import com.semtinazizi.kitapduy.model.Comment
import com.semtinazizi.kitapduy.model.FeedItem

interface PostDetailRepository {
    suspend fun getPost(id: String): FeedItem?
    suspend fun getComments(postId: String): List<Comment>
    suspend fun addComment(postId: String, text: String): Comment
    suspend fun editComment(postId: String, commentId: String, text: String): Comment
    suspend fun deleteComment(postId: String, commentId: String)
    suspend fun likeComment(postId: String, commentId: String): Comment
}
