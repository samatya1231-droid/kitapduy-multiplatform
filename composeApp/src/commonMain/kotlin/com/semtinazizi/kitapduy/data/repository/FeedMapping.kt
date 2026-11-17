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

internal fun mapSupabasePostToFeedItem(post: SupabasePost): FeedItem {
    val isBookPost = !post.bookId.isNullOrBlank()
    val isAutoStatus = post.comment?.contains("dinlemeye başladı \uD83C\uDFA7") == true
    val isQuotePost = post.startPos != null && !post.audioUrl.isNullOrBlank()
    val hasImage = !post.imageUrl.isNullOrBlank()

    val feedType = when {
        !post.pollOptions.isNullOrEmpty() || post.bookName == "Anket" -> FeedType.POLL
        isQuotePost -> FeedType.QUOTE
        hasImage && !isBookPost -> FeedType.IMAGE
        isBookPost && isAutoStatus -> FeedType.LISTEN
        !post.audioUrl.isNullOrBlank() -> FeedType.VOICE
        isBookPost -> FeedType.COMMENT
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
        timestamp = post.createdAt?.let { runCatching { kotlinx.datetime.Instant.parse(it).toEpochMilliseconds() }.getOrNull() } ?: 0L,
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
