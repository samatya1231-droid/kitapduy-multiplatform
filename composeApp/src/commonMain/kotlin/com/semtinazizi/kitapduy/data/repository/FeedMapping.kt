package com.semtinazizi.kitapduy.data.repository

import com.semtinazizi.kitapduy.model.*
import com.semtinazizi.kitapduy.platform.AppConfig
import com.semtinazizi.kitapduy.util.UrlSanitizer

internal fun resolveFeedUrl(url: String?): String {
    if (url.isNullOrBlank()) return ""
    val clean = url.trim()
    val resolved = if (
        clean.startsWith("http", ignoreCase = true) ||
        clean.startsWith("file:", ignoreCase = true) ||
        clean.startsWith("/")
    ) {
        clean
    } else {
        "${AppConfig.cfPublicUrl.trimEnd('/')}/${clean.trimStart('/')}"
    }
    return UrlSanitizer.normalizeMediaUrl(resolved)
}

internal fun parseSupabaseTimestamp(raw: String?): Long {
    if (raw.isNullOrBlank()) return 0L
    val clean = raw.trim().replace(" ", "T")
    val timePart = clean.substringAfter('T', "")
    val normalized = when {
        clean.endsWith("+00") -> clean.dropLast(3) + "Z"
        clean.endsWith("z") -> clean.dropLast(1) + "Z"
        Regex("[+-]\\d{4}$").containsMatchIn(timePart) ->
            clean.dropLast(2) + ":" + clean.takeLast(2)
        Regex("[+-]\\d{2}$").containsMatchIn(timePart) ->
            "$clean:00"
        !clean.endsWith("Z") &&
            !Regex("[+-]\\d{2}:\\d{2}$").containsMatchIn(timePart) &&
            clean.length >= 19 -> "${clean}Z"
        else -> clean
    }
    return runCatching { kotlinx.datetime.Instant.parse(normalized).toEpochMilliseconds() }.getOrDefault(0L)
}

internal fun mapSupabasePostToFeedItem(post: SupabasePost): FeedItem {
    val isBookPost = !post.bookId.isNullOrBlank()
    val isAutoStatus = post.comment?.contains("dinlemeye başladı", ignoreCase = true) == true
    val isQuotePost = !isAutoStatus && post.startPos != null && !post.audioUrl.isNullOrBlank()
    val hasImage = !post.imageUrl.isNullOrBlank()

    val feedType = when {
        !post.pollOptions.isNullOrEmpty() || post.bookName == "Anket" -> FeedType.POLL
        isAutoStatus -> FeedType.LISTEN
        isQuotePost -> FeedType.QUOTE
        hasImage && !isBookPost -> FeedType.IMAGE
        isBookPost -> FeedType.COMMENT
        !post.audioUrl.isNullOrBlank() -> FeedType.VOICE
        else -> FeedType.QUOTE
    }

    return FeedItem(
        id = post.id?.toString() ?: "",
        userId = post.userId ?: "",
        userName = post.userName ?: "Bilinmeyen Kullanıcı",
        userPhotoUrl = resolveFeedUrl(post.userPhoto),
        bookId = post.bookId ?: "",
        bookTitle = post.bookName ?: "",
        bookCoverUrl = if (isBookPost) resolveFeedUrl(post.imageUrl) else "",
        content = post.comment ?: "",
        likes = post.likes ?: 0,
        timestamp = parseSupabaseTimestamp(post.createdAt),
        likedBy = post.likedBy.orEmpty().split(",").filter { it.isNotBlank() },
        type = feedType,
        isPremium = post.isPremium ?: false,
        postImageUrl = if (!isBookPost) resolveFeedUrl(post.imageUrl).ifBlank { null } else null,
        audioUrl = resolveFeedUrl(post.audioUrl),
        commentCount = post.commentCount ?: 0,
        pollOptions = post.pollOptions ?: emptyList(),
        pollVotes = post.pollVotes ?: emptyMap(),
        pollVoters = post.pollVoters ?: emptyList(),
        startPos = post.startPos
    )
}
