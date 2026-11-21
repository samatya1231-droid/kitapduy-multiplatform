package com.semtinazizi.kitapduy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semtinazizi.kitapduy.model.PodcastEpisode
import com.semtinazizi.kitapduy.util.RssTextCleaner
import com.semtinazizi.kitapduy.util.UrlSanitizer
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PodcastEpisodesViewModel(
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _episodes = MutableStateFlow<List<PodcastEpisode>>(emptyList())
    val episodes: StateFlow<List<PodcastEpisode>> = _episodes.asStateFlow()

    private val _channelImageUrl = MutableStateFlow<String?>(null)
    val channelImageUrl: StateFlow<String?> = _channelImageUrl.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var loadJob: Job? = null
    private var activeRequestKey: String? = null

    fun loadEpisodes(rssUrl: String, podcastId: String = "", forceRefresh: Boolean = false) {
        val cleanUrl = rssUrl.trim()
        val cleanPodcastId = podcastId.trim()
        val requestKey = "$cleanPodcastId|$cleanUrl"

        if (!forceRefresh && activeRequestKey == requestKey && (_episodes.value.isNotEmpty() || _isLoading.value)) {
            return
        }

        loadJob?.cancel()
        activeRequestKey = requestKey
        _episodes.value = emptyList()
        _channelImageUrl.value = null
        _error.value = null

        println("PodcastViewModel: Loading episodes from $cleanUrl")
        if (cleanUrl.isBlank()) {
            _isLoading.value = false
            _error.value = "Geçersiz RSS bağlantısı."
            return
        }

        _isLoading.value = true
        loadJob = viewModelScope.launch(ioDispatcher) {
            try {
                val response = httpClient.get(cleanUrl) {
                    header(HttpHeaders.UserAgent, RSS_USER_AGENT)
                    header(HttpHeaders.Accept, RSS_ACCEPT_HEADER)
                }
                if (!response.status.isSuccess()) {
                    println("PodcastViewModel: RSS HTTP error ${response.status.value} for $cleanUrl")
                    if (activeRequestKey == requestKey) {
                        _episodes.value = emptyList()
                        _error.value = "Podcast yayın akışı yüklenemedi (HTTP ${response.status.value})."
                    }
                    return@launch
                }

                val responseText = response.bodyAsText()
                println("PodcastViewModel: RSS fetched, length=${responseText.length}")
                val channelImage = extractChannelImage(responseText)
                val parsedEpisodes = parseEpisodesXml(responseText, cleanPodcastId)
                println("PodcastViewModel: Parsed ${parsedEpisodes.size} episodes")

                if (activeRequestKey != requestKey) return@launch

                if (parsedEpisodes.isEmpty() && !looksLikeRssOrAtom(responseText)) {
                    _episodes.value = emptyList()
                    _error.value = "Podcast yayın akışı okunamadı. Lütfen daha sonra tekrar deneyin."
                    return@launch
                }

                _channelImageUrl.value = channelImage
                _episodes.value = parsedEpisodes
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Throwable) {
                println("PodcastViewModel: Error loading RSS: ${e.message}")
                if (activeRequestKey == requestKey) {
                    _episodes.value = emptyList()
                    _error.value = "Bölümler yüklenirken hata oluştu. Lütfen bağlantınızı kontrol edip tekrar deneyin."
                }
            } finally {
                if (activeRequestKey == requestKey) {
                    _isLoading.value = false
                }
            }
        }
    }

    companion object {
        internal const val RSS_USER_AGENT =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) KitapDuy/1.1"
        internal const val RSS_ACCEPT_HEADER =
            "application/rss+xml, application/atom+xml, application/xml, text/xml;q=0.9, */*;q=0.5"

        private val ITEM_REGEX = "(?s)<(?:item|entry)(?:\\s[^>]*)?>(.*?)</(?:item|entry)>".toRegex(RegexOption.IGNORE_CASE)
        private val TITLE_REGEX = "(?s)<title(?:\\s[^>]*)?>(.*?)</title>".toRegex(RegexOption.IGNORE_CASE)
        private val DESC_REGEX = "(?s)<description(?:\\s[^>]*)?>(.*?)</description>".toRegex(RegexOption.IGNORE_CASE)
        private val SUMMARY_REGEX = "(?s)<(?:itunes:summary|summary|content:encoded|itunes:subtitle)(?:\\s[^>]*)?>(.*?)</(?:itunes:summary|summary|content:encoded|itunes:subtitle)>".toRegex(RegexOption.IGNORE_CASE)
        private val ENCLOSURE_URL_REGEX = "<enclosure[^>]+\\burl\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>".toRegex(RegexOption.IGNORE_CASE)
        private val LINK_TAG_REGEX = "<link\\b[^>]+>".toRegex(RegexOption.IGNORE_CASE)
        private val ATTR_HREF_REGEX = "\\bhref\\s*=\\s*['\"]([^'\"]+)['\"]".toRegex(RegexOption.IGNORE_CASE)
        private val ATTR_TYPE_REGEX = "\\btype\\s*=\\s*['\"]([^'\"]+)['\"]".toRegex(RegexOption.IGNORE_CASE)
        private val DURATION_REGEX = "(?s)<(?:itunes:)?duration(?:\\s[^>]*)?>(.*?)</(?:itunes:)?duration>".toRegex(RegexOption.IGNORE_CASE)
        private val PUB_DATE_REGEX = "(?s)<(?:pubDate|published)(?:\\s[^>]*)?>(.*?)</(?:pubDate|published)>".toRegex(RegexOption.IGNORE_CASE)
        private val ITEM_IMAGE_REGEX = "<(?:itunes:image|media:thumbnail|image)[^>]+\\b(?:href|url)\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>".toRegex(RegexOption.IGNORE_CASE)
        private val CHANNEL_ITUNES_IMAGE_REGEX = "<(?:itunes:)?image[^>]+\\bhref\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>".toRegex(RegexOption.IGNORE_CASE)
        private val CHANNEL_IMAGE_BLOCK_REGEX = "(?s)<image(?:\\s[^>]*)?>(.*?)</image>".toRegex(RegexOption.IGNORE_CASE)
        private val CHANNEL_URL_REGEX = "(?s)<url(?:\\s[^>]*)?>(.*?)</url>".toRegex(RegexOption.IGNORE_CASE)

        internal fun looksLikeRssOrAtom(body: String): Boolean {
            if (body.isBlank()) return false
            return body.contains("<rss", ignoreCase = true) ||
                body.contains("<channel", ignoreCase = true) ||
                body.contains("<feed", ignoreCase = true)
        }

        private fun normalizeXmlUrl(rawUrl: String): String {
            val (unwrapped, _) = RssTextCleaner.unwrapCdata(rawUrl)
            val decoded = RssTextCleaner.decodeXmlEntities(unwrapped).trim()
            if (decoded.isBlank()) return ""
            return UrlSanitizer.normalizeMediaUrl(decoded)
        }

        private fun extractRawAudioUrl(itemContent: String): String? {
            val enclosureUrl = ENCLOSURE_URL_REGEX.find(itemContent)?.groupValues?.get(1)?.trim()
            if (!enclosureUrl.isNullOrBlank()) return enclosureUrl

            for (linkMatch in LINK_TAG_REGEX.findAll(itemContent)) {
                val tag = linkMatch.value
                val href = ATTR_HREF_REGEX.find(tag)?.groupValues?.get(1)?.trim() ?: continue
                if (href.isBlank()) continue
                val type = ATTR_TYPE_REGEX.find(tag)?.groupValues?.get(1)?.trim().orEmpty()
                if (type.contains("audio", ignoreCase = true) ||
                    href.contains(".mp3", ignoreCase = true) ||
                    href.contains(".m4a", ignoreCase = true) ||
                    href.contains(".aac", ignoreCase = true) ||
                    href.contains(".ogg", ignoreCase = true)
                ) {
                    return href
                }
            }
            return null
        }

        internal fun extractChannelImage(xmlContent: String): String? {
            val itemIdx = xmlContent.indexOf("<item", ignoreCase = true)
            val entryIdx = xmlContent.indexOf("<entry", ignoreCase = true)
            val cutoff = listOf(itemIdx, entryIdx).filter { it >= 0 }.minOrNull() ?: xmlContent.length
            val channelHeader = xmlContent.substring(0, cutoff)

            val channelItunesImage = CHANNEL_ITUNES_IMAGE_REGEX
                .find(channelHeader)?.groupValues?.get(1)?.trim()
            val channelImageBlock = CHANNEL_IMAGE_BLOCK_REGEX
                .find(channelHeader)?.groupValues?.get(1)
            val channelUrlImage = channelImageBlock?.let {
                CHANNEL_URL_REGEX.find(it)?.groupValues?.get(1)?.trim()
            }
            val rawCandidate = channelItunesImage?.takeIf { it.isNotBlank() }
                ?: channelUrlImage?.takeIf { it.isNotBlank() }
                ?: return null
            return normalizeXmlUrl(rawCandidate).takeIf { it.isNotBlank() }
        }

        internal fun parseEpisodesXml(xmlContent: String, podcastId: String = ""): List<PodcastEpisode> {
            val resultList = mutableListOf<PodcastEpisode>()
            val fallbackChannelImage = extractChannelImage(xmlContent)
            val cleanPodcastId = podcastId.trim()

            var count = 0
            for (itemMatch in ITEM_REGEX.findAll(xmlContent)) {
                val itemContent = itemMatch.groupValues[1]

                val rawAudioUrl = extractRawAudioUrl(itemContent) ?: continue
                val audioUrl = normalizeXmlUrl(rawAudioUrl)
                if (audioUrl.isBlank()) continue

                val rawTitle = TITLE_REGEX.find(itemContent)?.groupValues?.get(1).orEmpty()
                val title = RssTextCleaner.cleanTitle(rawTitle).ifBlank { "Bölüm ${count + 1}" }

                val rawDesc = DESC_REGEX.find(itemContent)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
                    ?: SUMMARY_REGEX.find(itemContent)?.groupValues?.get(1).orEmpty()
                val desc = RssTextCleaner.cleanHtmlDescription(rawDesc)

                val rawDuration = DURATION_REGEX.find(itemContent)?.groupValues?.get(1).orEmpty()
                val duration = RssTextCleaner.decodeXmlEntities(RssTextCleaner.unwrapCdata(rawDuration).first).trim()

                val rawPubDate = PUB_DATE_REGEX.find(itemContent)?.groupValues?.get(1).orEmpty()
                val pubDate = RssTextCleaner.decodeXmlEntities(RssTextCleaner.unwrapCdata(rawPubDate).first).trim()

                val imageUrl = ITEM_IMAGE_REGEX.find(itemContent)?.groupValues?.get(1)?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { normalizeXmlUrl(it) }
                    ?.takeIf { it.isNotBlank() }
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
            }
            return resultList
        }

        internal fun extractCandidateAudioHashes(xmlContent: String): Map<String, Set<Int>> {
            val map = mutableMapOf<String, MutableSet<Int>>()
            for (itemMatch in ITEM_REGEX.findAll(xmlContent)) {
                val rawUrl = extractRawAudioUrl(itemMatch.groupValues[1]) ?: continue
                if (rawUrl.isBlank()) continue
                val decoded = RssTextCleaner.decodeXmlEntities(RssTextCleaner.unwrapCdata(rawUrl).first).trim()
                val normalized = UrlSanitizer.normalizeMediaUrl(decoded)
                if (normalized.isBlank()) continue
                val bucket = map.getOrPut(normalized) { mutableSetOf() }
                bucket.add(rawUrl.hashCode())
                bucket.add(decoded.hashCode())
                bucket.add(normalized.hashCode())
            }
            return map
        }
    }
}
