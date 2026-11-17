package com.semtinazizi.kitapduy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semtinazizi.kitapduy.model.PodcastEpisode
import com.semtinazizi.kitapduy.util.UrlSanitizer
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

    private val _channelImageUrl = MutableStateFlow<String?>(null)
    val channelImageUrl: StateFlow<String?> = _channelImageUrl.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadEpisodes(rssUrl: String, podcastId: String = "") {
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
                _channelImageUrl.value = extractChannelImage(responseText)
                val parsedEpisodes = parseEpisodesXml(responseText, podcastId)
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

    companion object {
        internal fun extractChannelImage(xmlContent: String): String? {
            val channelHeader = xmlContent.substringBefore("<item", missingDelimiterValue = xmlContent)
            val channelItunesImage = "<(?:itunes:)?image[^>]+href\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>"
                .toRegex(RegexOption.IGNORE_CASE)
                .find(channelHeader)?.groupValues?.get(1)?.trim()
            val channelImageBlock = "(?s)<image>(.*?)</image>"
                .toRegex(RegexOption.IGNORE_CASE)
                .find(channelHeader)?.groupValues?.get(1)
            val channelUrlImage = channelImageBlock?.let {
                "(?s)<url>(.*?)</url>".toRegex(RegexOption.IGNORE_CASE).find(it)?.groupValues?.get(1)?.trim()
            }
            return (channelItunesImage?.takeIf { it.isNotBlank() } ?: channelUrlImage?.takeIf { it.isNotBlank() })
                ?.let { UrlSanitizer.normalizeMediaUrl(it) }
        }

        internal fun parseEpisodesXml(xmlContent: String, podcastId: String = ""): List<PodcastEpisode> {
            val resultList = mutableListOf<PodcastEpisode>()
            val fallbackChannelImage = extractChannelImage(xmlContent)

            val itemRegex = "(?s)<item>(.*?)</item>".toRegex(RegexOption.IGNORE_CASE)
            val titleRegex = "(?s)<title>(.*?)</title>".toRegex(RegexOption.IGNORE_CASE)
            val descRegex1 = "(?s)<description><!\\[CDATA\\[(.*?)]]></description>".toRegex(RegexOption.IGNORE_CASE)
            val descRegex2 = "(?s)<description>(.*?)</description>".toRegex(RegexOption.IGNORE_CASE)
            val enclosureRegex = "<enclosure[^>]+url\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>".toRegex(RegexOption.IGNORE_CASE)
            val durationRegex = "<(?:itunes:)?duration>(.*?)</(?:itunes:)?duration>".toRegex(RegexOption.IGNORE_CASE)
            val pubDateRegex = "<pubDate>(.*?)</pubDate>".toRegex(RegexOption.IGNORE_CASE)
            val imageRegex = "<(?:itunes:)?image[^>]+href\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>".toRegex(RegexOption.IGNORE_CASE)

            val items = itemRegex.findAll(xmlContent)
            val cleanPodcastId = podcastId.trim()

            var count = 0
            for (itemMatch in items) {
                val itemContent = itemMatch.groupValues[1]

                val rawAudioUrl = enclosureRegex.find(itemContent)?.groupValues?.get(1)?.trim() ?: continue
                if (rawAudioUrl.isBlank()) continue
                val audioUrl = UrlSanitizer.normalizeMediaUrl(rawAudioUrl)

                val title = titleRegex.find(itemContent)?.groupValues?.get(1)
                    ?.replace("<![CDATA[", "")?.replace("]]>", "")?.trim()
                    ?: "Bölüm ${count + 1}"
                val desc = descRegex1.find(itemContent)?.groupValues?.get(1)?.trim()
                    ?: descRegex2.find(itemContent)?.groupValues?.get(1)?.trim()
                    ?: ""
                val duration = durationRegex.find(itemContent)?.groupValues?.get(1)?.trim() ?: ""
                val pubDate = pubDateRegex.find(itemContent)?.groupValues?.get(1)?.trim() ?: ""
                val imageUrl = imageRegex.find(itemContent)?.groupValues?.get(1)?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { UrlSanitizer.normalizeMediaUrl(it) }
                    ?: fallbackChannelImage

                val stableId = if (cleanPodcastId.isNotBlank()) {
                    "podcast_${cleanPodcastId}_EP_${audioUrl.hashCode()}"
                } else {
                    audioUrl
                }

                resultList.add(
                    PodcastEpisode(
                        id = stableId,
                        title = title,
                        description = desc,
                        audioUrl = audioUrl,
                        duration = duration,
                        pubDate = pubDate,
                        imageUrl = imageUrl
                    )
                )
                count++
                if (count >= 100) break
            }
            return resultList
        }

        internal fun extractCandidateAudioHashes(xmlContent: String): Map<String, Set<Int>> {
            val itemRegex = "(?s)<item>(.*?)</item>".toRegex(RegexOption.IGNORE_CASE)
            val enclosureRegex = "<enclosure[^>]+url\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>".toRegex(RegexOption.IGNORE_CASE)
            val map = mutableMapOf<String, MutableSet<Int>>()
            for (itemMatch in itemRegex.findAll(xmlContent)) {
                val rawUrl = enclosureRegex.find(itemMatch.groupValues[1])?.groupValues?.get(1)?.trim() ?: continue
                if (rawUrl.isBlank()) continue
                val normalized = UrlSanitizer.normalizeMediaUrl(rawUrl)
                val bucket = map.getOrPut(normalized) { mutableSetOf() }
                bucket.add(rawUrl.hashCode())
                bucket.add(normalized.hashCode())
            }
            return map
        }
    }
}
