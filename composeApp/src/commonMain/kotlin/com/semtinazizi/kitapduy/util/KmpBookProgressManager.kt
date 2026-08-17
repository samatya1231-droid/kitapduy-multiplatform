package com.semtinazizi.kitapduy.util

import com.russhwolf.settings.Settings

class KmpBookProgressManager {
    private val settings: Settings = Settings()

    fun saveProgress(id: String, pos: Long) {
        if (pos > 0) {
            settings.putLong(id, pos)
        }
    }

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
    fun saveContentMetadata(id: String, title: String, author: String, coverUrl: String) {
        settings.putString("meta_${id}_title", title)
        settings.putString("meta_${id}_author", author)
        settings.putString("meta_${id}_cover", coverUrl)
    }

    fun getContentMetadata(id: String): Triple<String, String, String>? {
        val title = settings.getStringOrNull("meta_${id}_title") ?: return null
        val author = settings.getString("meta_${id}_author", "Bilinmeyen")
        val cover = settings.getString("meta_${id}_cover", "")
        return Triple(title, author, cover)
    }
}
