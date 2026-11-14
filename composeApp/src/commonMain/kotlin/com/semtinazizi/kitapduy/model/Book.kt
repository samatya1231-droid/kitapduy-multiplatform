package com.semtinazizi.kitapduy.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Book(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val category: String = "",
    @SerialName("cover_url") val coverUrl: String = "",
    @SerialName("audio_url") val audioUrl: String = "",
    val description: String = "",
    val duration: Long = 0L,
    @SerialName("listen_count") val listenCount: Long = 0L,
    @SerialName("view_count") val viewCount: Int = 0,
    @SerialName("like_count") val likeCount: Int = 0,
    val language: String = "tr",
    val tags: List<String> = emptyList()
)
