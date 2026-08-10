package com.semtinazizi.kitapduy.model

data class Book(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val category: String = "",
    val coverUrl: String = "",
    val audioUrl: String = "",
    val description: String = "",
    val duration: Long = 0L,
    val listenCount: Long = 0L,
    val viewCount: Int = 0,
    val likeCount: Int = 0
)
