package com.semtinazizi.kitapduy.model

import kotlinx.serialization.Serializable

@Serializable
data class Podcast(
    val id: String = "",
    val name: String = "",
    val author: String = "",
    val rssUrl: String = "",
    val imageUrl: String = "",
    val category: String = ""
)

@Serializable
data class PodcastEpisode(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val audioUrl: String = "",
    val duration: String = "",
    val pubDate: String = "",
    val imageUrl: String? = null
)
