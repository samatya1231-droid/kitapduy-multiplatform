package com.semtinazizi.kitapduy.util

import com.russhwolf.settings.Settings

class KmpBookProgressManager(private val settings: Settings = Settings()) {

    fun saveProgress(id: String, pos: Long) {
        if (pos > 0) {
            settings.putLong(id, pos)
        }
    }

    fun getHistoryIds(): List<String> = settings.keys
        .filter { it.startsWith("meta_") && it.endsWith("_title") }
        .map { it.removePrefix("meta_").removeSuffix("_title") }
        .filter { getProgress(it) > 0 }
        .sorted()

    fun getProgress(id: String): Long {
        return settings.getLong(id, 0L)
    }

    fun saveLastPlayedBook(id: String) {
        settings.putString("last_played_book_id", id)
    }

    fun getLastPlayedBook(): String? {
        return settings.getStringOrNull("last_played_book_id")
    }

    // 🌟 PODCAST VE DİĞER GEÇİCİ İÇERİKLER İÇİN METADATA SAKLAMA 🌟
    fun saveContentMetadata(id: String, title: String, author: String, coverUrl: String, audioUrl: String = "") {
        settings.putString("meta_${id}_title", title)
        settings.putString("meta_${id}_author", author)
        settings.putString("meta_${id}_cover", coverUrl)
        if (audioUrl.isNotBlank()) {
            settings.putString("meta_${id}_audio", audioUrl)
        }
    }

    fun getContentAudioUrl(id: String): String {
        return settings.getString("meta_${id}_audio", "")
    }

    fun getContentMetadata(id: String): Triple<String, String, String>? {
        val title = settings.getStringOrNull("meta_${id}_title") ?: return null
        val author = settings.getString("meta_${id}_author", "Bilinmeyen")
        val cover = settings.getString("meta_${id}_cover", "")
        return Triple(title, author, cover)
    }
}
