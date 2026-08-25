package com.semtinazizi.kitapduy.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Podcast(
    val id: String = "",
    val name: String = "",
    val author: String = "",
    @SerialName("rss_url") val rssUrl: String = "",
    @SerialName("image_url") val imageUrl: String = "",
    val category: String = ""
)

@Serializable
data class PodcastEpisode(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    @SerialName("audio_url") val audioUrl: String = "",
    val duration: String = "",
    @SerialName("pub_date") val pubDate: String = "",
    @SerialName("image_url") val imageUrl: String? = null
)
