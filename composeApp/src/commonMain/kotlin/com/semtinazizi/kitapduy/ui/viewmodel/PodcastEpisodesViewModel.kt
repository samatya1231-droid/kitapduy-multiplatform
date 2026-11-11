package com.semtinazizi.kitapduy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semtinazizi.kitapduy.model.PodcastEpisode
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PodcastEpisodesViewModel(
    private val httpClient: HttpClient
) : ViewModel() {

    private val _episodes = MutableStateFlow<List<PodcastEpisode>>(emptyList())
    val episodes: StateFlow<List<PodcastEpisode>> = _episodes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadEpisodes(rssUrl: String) {
        println("PodcastViewModel: Loading episodes from $rssUrl")
        if (rssUrl.isBlank()) {
            _error.value = "Geçersiz RSS bağlantısı"
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            _isLoading.value = true
            _error.value = null

            try {
                val responseText = httpClient.get(rssUrl).bodyAsText()
                println("PodcastViewModel: RSS fetched, length=${responseText.length}")
                val parsedEpisodes = parseEpisodes(responseText)
                println("PodcastViewModel: Parsed ${parsedEpisodes.size} episodes")
                _episodes.value = parsedEpisodes
            } catch (e: Throwable) {
                println("PodcastViewModel: Error loading RSS: ${e.message}")
                _error.value = "Bölümler yüklenirken hata oluştu: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun parseEpisodes(xmlContent: String): List<PodcastEpisode> {
        val resultList = mutableListOf<PodcastEpisode>()
        
        val itemRegex = "(?s)<item>(.*?)</item>".toRegex(RegexOption.IGNORE_CASE)
        val titleRegex = "(?s)<title>(.*?)</title>".toRegex(RegexOption.IGNORE_CASE)
        val descRegex1 = "(?s)<description><!\\[CDATA\\[(.*?)]]></description>".toRegex(RegexOption.IGNORE_CASE)
        val descRegex2 = "(?s)<description>(.*?)</description>".toRegex(RegexOption.IGNORE_CASE)
        val enclosureRegex = "<enclosure[^>]+url\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>".toRegex(RegexOption.IGNORE_CASE)
        val durationRegex = "<(?:itunes:)?duration>(.*?)</(?:itunes:)?duration>".toRegex(RegexOption.IGNORE_CASE)
        val pubDateRegex = "<pubDate>(.*?)</pubDate>".toRegex(RegexOption.IGNORE_CASE)
        val imageRegex = "<(?:itunes:)?image[^>]+href\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>".toRegex(RegexOption.IGNORE_CASE)

        val items = itemRegex.findAll(xmlContent)
        
        var count = 0
        for (itemMatch in items) {
            val itemContent = itemMatch.groupValues[1]
            
            val title = titleRegex.find(itemContent)?.groupValues?.get(1)?.replace("<![CDATA[", "")?.replace("]]>", "")?.trim() ?: continue
            
            var desc = descRegex1.find(itemContent)?.groupValues?.get(1) ?: descRegex2.find(itemContent)?.groupValues?.get(1) ?: ""
            desc = desc.replace(Regex("<[^>]*>"), "")
            desc = desc.replace("<![CDATA[", "").replace("]]>", "").trim()
            
            val audioUrl = enclosureRegex.find(itemContent)?.groupValues?.get(1)?.trim() ?: continue
            val duration = durationRegex.find(itemContent)?.groupValues?.get(1)?.trim() ?: ""
            val pubDate = pubDateRegex.find(itemContent)?.groupValues?.get(1)?.trim() ?: ""
            val imageUrl = imageRegex.find(itemContent)?.groupValues?.get(1)?.trim()
            
            resultList.add(
                PodcastEpisode(
                    id = audioUrl,
                    title = title,
                    description = desc,
                    audioUrl = audioUrl,
                    duration = duration,
                    pubDate = pubDate,
                    imageUrl = imageUrl
                )
            )
            count++
            if (count >= 50) break // Sınırla
        }
        
        return resultList
    }
}
