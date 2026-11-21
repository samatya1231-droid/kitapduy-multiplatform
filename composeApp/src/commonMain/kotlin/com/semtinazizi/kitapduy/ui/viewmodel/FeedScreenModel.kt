package com.semtinazizi.kitapduy.ui.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.domain.repository.BookRepository
import com.semtinazizi.kitapduy.domain.repository.FeedRepository
import com.semtinazizi.kitapduy.data.repository.StorageRepository
import com.semtinazizi.kitapduy.model.Book
import com.semtinazizi.kitapduy.model.FeedItem
import com.semtinazizi.kitapduy.model.FeedType
import com.semtinazizi.kitapduy.model.PodcastEpisode
import com.semtinazizi.kitapduy.model.UserProfile
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

sealed interface FeedContentTarget {
    data class BookTarget(val book: Book) : FeedContentTarget
    data class PodcastTarget(val episode: PodcastEpisode, val podcastTitle: String) : FeedContentTarget
    data class NotFound(val message: String) : FeedContentTarget
}

class FeedScreenModel(
    private val feedRepository: FeedRepository,
    private val storageRepository: StorageRepository,
    private val actions: com.semtinazizi.kitapduy.data.repository.FeedActionsRepository,
    private val bookRepository: BookRepository? = null,
    private val authRepository: AuthRepository? = null
) : ScreenModel {

    companion object {
        private const val LIVE_WINDOW_MS = 24L * 60L * 60L * 1000L
        internal const val PAGE_SIZE = 50
        private val WHITESPACE_REGEX = Regex("\\s+")
        private val GENERIC_LISTENER_NAMES = setOf("okur", "kullanıcı", "kullanici", "isimsiz", "misafir")

        internal fun mergeRefreshedPage(
            freshPage: List<FeedItem>,
            existing: List<FeedItem>,
            pageSize: Int = PAGE_SIZE,
            hasMorePages: Boolean = false
        ): List<FeedItem> {
            if (freshPage.isEmpty()) return existing
            val distinctFresh = freshPage.distinctBy { it.id }
            if (distinctFresh.size < pageSize && !hasMorePages) {
                return distinctFresh
            }
            val freshIds = distinctFresh.map { it.id }.toSet()
            val minFreshTs = distinctFresh.minOf { it.timestamp }
            val minFreshNumericId = distinctFresh.mapNotNull { it.id.toLongOrNull() }.minOrNull()
            val olderTail = existing.filter { item ->
                if (item.id in freshIds) return@filter false
                val numericId = item.id.toLongOrNull()
                when {
                    minFreshTs > 0L && item.timestamp > 0L -> {
                        item.timestamp < minFreshTs ||
                            (item.timestamp == minFreshTs && minFreshNumericId != null && numericId != null && numericId < minFreshNumericId)
                    }
                    minFreshNumericId != null && numericId != null -> numericId < minFreshNumericId
                    else -> false
                }
            }
            return distinctFresh + olderTail
        }

        internal fun computeListeningRibbon(
            items: List<FeedItem>,
            nowMs: Long = Clock.System.now().toEpochMilliseconds()
        ): List<FeedItem> {
            val cutoff = nowMs - LIVE_WINDOW_MS
            val seenIds = mutableSetOf<String>()
            val seenNames = mutableSetOf<String>()
            return items
                .filter { it.type == FeedType.LISTEN && (it.userId.isNotBlank() || it.userName.isNotBlank()) && it.timestamp >= cutoff }
                .sortedByDescending { it.timestamp }
                .filter { item ->
                    val uid = item.userId.trim().lowercase()
                    val uname = item.userName.trim().replace(WHITESPACE_REGEX, " ").lowercase()
                    val isGenericName = uname.isBlank() || uname in GENERIC_LISTENER_NAMES
                    if (uid.isNotEmpty() && uid in seenIds) return@filter false
                    if (!isGenericName && uname in seenNames) return@filter false
                    if (uid.isNotEmpty()) seenIds.add(uid)
                    if (!isGenericName) seenNames.add(uname)
                    true
                }
        }
    }

    val actionError = MutableStateFlow<String?>(null)
    private val pendingActions = mutableSetOf<String>()
    private fun mutate(id: String, action: suspend () -> Unit) {
        if (!pendingActions.add(id)) return
        screenModelScope.launch {
            try { action(); loadFeed(true) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { actionError.value = e.message ?: "İşlem tamamlanamadı." }
            finally { pendingActions.remove(id) }
        }
    }
    fun toggleLike(id: String, reaction: Int) = mutate(id) { actions.toggleLike(id, reaction) }
    fun vote(id: String, option: Int) = mutate(id) { actions.vote(id, option) }
    fun deletePost(id: String) = mutate(id) { actions.deletePost(id) }

    private val _generalFeedItems = MutableStateFlow<List<FeedItem>>(emptyList())
    val generalFeedItems: StateFlow<List<FeedItem>> = _generalFeedItems.asStateFlow()

    private val _listeningFeedItems = MutableStateFlow<List<FeedItem>>(emptyList())
    val listeningFeedItems: StateFlow<List<FeedItem>> = _listeningFeedItems.asStateFlow()

    private val _listeningRibbonItems = MutableStateFlow<List<FeedItem>>(emptyList())
    val listeningRibbonItems: StateFlow<List<FeedItem>> = _listeningRibbonItems.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _canLoadMoreGeneral = MutableStateFlow(true)
    private val _canLoadMoreListening = MutableStateFlow(true)
    private val _canLoadMore = MutableStateFlow(true)
    val canLoadMore: StateFlow<Boolean> = _canLoadMore.asStateFlow()

    private val _unreadNotificationCount = MutableStateFlow(0)
    val unreadNotificationCount: StateFlow<Int> = _unreadNotificationCount.asStateFlow()

    private var generalOffset = 0L
    private var listeningOffset = 0L

    init {
        feedRepository.startRealtimeListening { newFeedItem ->
            if (newFeedItem.type == FeedType.LISTEN) {
                val updated = listOf(newFeedItem) + _listeningFeedItems.value.filterNot { it.id == newFeedItem.id }
                _listeningFeedItems.value = updated
                _listeningRibbonItems.value = computeListeningRibbon(updated)
            } else {
                val updated = listOf(newFeedItem) + _generalFeedItems.value.filterNot { it.id == newFeedItem.id }
                _generalFeedItems.value = updated
            }
        }
    }

    override fun onDispose() {
        feedRepository.stopRealtimeListening()
        super.onDispose()
    }

    suspend fun refreshFeedSilently() {
        if (_isLoading.value || _isLoadingMore.value) return
        try {
            coroutineScope {
                val generalDeferred = async { feedRepository.getFeedItems(excludeListen = true, limit = PAGE_SIZE.toLong(), offset = 0) }
                val listeningDeferred = async { feedRepository.getListeningFeedItems(limit = PAGE_SIZE.toLong(), offset = 0) }

                val freshGeneral = generalDeferred.await().filter { it.type != FeedType.LISTEN }
                val freshListening = listeningDeferred.await().filter { it.type == FeedType.LISTEN }

                if (freshGeneral.isNotEmpty()) {
                    _generalFeedItems.value = mergeRefreshedPage(
                        freshPage = freshGeneral,
                        existing = _generalFeedItems.value,
                        pageSize = PAGE_SIZE,
                        hasMorePages = _canLoadMoreGeneral.value || _generalFeedItems.value.size > PAGE_SIZE
                    )
                }
                if (freshListening.isNotEmpty()) {
                    _listeningFeedItems.value = mergeRefreshedPage(
                        freshPage = freshListening,
                        existing = _listeningFeedItems.value,
                        pageSize = PAGE_SIZE,
                        hasMorePages = _canLoadMoreListening.value || _listeningFeedItems.value.size > PAGE_SIZE
                    )
                    _listeningRibbonItems.value = computeListeningRibbon(freshListening)
                }
            }
            refreshUnreadNotificationsSilently()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
        }
    }

    private fun refreshUnreadNotificationsSilently() {
        screenModelScope.launch {
            try {
                _unreadNotificationCount.value = actions.unreadNotificationCount()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
            }
        }
    }

    fun loadFeed(isRefresh: Boolean = false) {
        if (isRefresh) {
            _isLoading.value = true
            _canLoadMoreGeneral.value = true
            _canLoadMoreListening.value = true
            _canLoadMore.value = true
            generalOffset = 0L
            listeningOffset = 0L
        }

        screenModelScope.launch {
            try {
                coroutineScope {
                    val generalDeferred = async { feedRepository.getFeedItems(excludeListen = true, limit = PAGE_SIZE.toLong(), offset = 0) }
                    val listeningDeferred = async { feedRepository.getListeningFeedItems(limit = PAGE_SIZE.toLong(), offset = 0) }

                    val generalRaw = generalDeferred.await()
                    val listeningRaw = listeningDeferred.await()

                    generalOffset = generalRaw.size.toLong()
                    listeningOffset = listeningRaw.size.toLong()
                    _canLoadMoreGeneral.value = generalRaw.size >= PAGE_SIZE
                    _canLoadMoreListening.value = listeningRaw.size >= PAGE_SIZE
                    _canLoadMore.value = _canLoadMoreGeneral.value || _canLoadMoreListening.value

                    _generalFeedItems.value = generalRaw.filter { it.type != FeedType.LISTEN }
                    val listeningOnly = listeningRaw.filter { it.type == FeedType.LISTEN }
                    _listeningFeedItems.value = listeningOnly
                    _listeningRibbonItems.value = computeListeningRibbon(listeningOnly)
                }
                refreshUnreadNotificationsSilently()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                println("Feed yüklenirken hata oluştu: ${e.message}")
                if (_generalFeedItems.value.isEmpty() && _listeningFeedItems.value.isEmpty()) {
                    actionError.value = "Akış yüklenemedi. Lütfen tekrar deneyin."
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshListeningStreamSilently() {
        screenModelScope.launch {
            try {
                val latest = feedRepository.getListeningFeedItems(limit = PAGE_SIZE.toLong(), offset = 0)
                    .filter { it.type == FeedType.LISTEN }
                if (latest.isNotEmpty()) {
                    _listeningFeedItems.value = mergeRefreshedPage(
                        freshPage = latest,
                        existing = _listeningFeedItems.value,
                        pageSize = PAGE_SIZE,
                        hasMorePages = _canLoadMoreListening.value || _listeningFeedItems.value.size > PAGE_SIZE
                    )
                    _listeningRibbonItems.value = computeListeningRibbon(latest)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                // Silent background refresh should never disrupt reading UX
            }
        }
    }

    fun loadMore(isListeningTab: Boolean = false) {
        if (_isLoading.value || _isLoadingMore.value) return
        if (isListeningTab && !_canLoadMoreListening.value) return
        if (!isListeningTab && !_canLoadMoreGeneral.value) return
        val currentItems = if (isListeningTab) _listeningFeedItems.value else _generalFeedItems.value
        if (currentItems.isEmpty()) return

        screenModelScope.launch {
            _isLoadingMore.value = true
            try {
                if (isListeningTab) {
                    val raw = feedRepository.getListeningFeedItems(limit = 20, offset = listeningOffset)
                    listeningOffset += raw.size.toLong()
                    if (raw.size < 20) {
                        _canLoadMoreListening.value = false
                    }
                    val filtered = raw.filter { it.type == FeedType.LISTEN }
                    val existingIds = _listeningFeedItems.value.map { it.id }.toSet()
                    val uniqueNew = filtered.filterNot { it.id in existingIds }
                    if (uniqueNew.isEmpty() && raw.size < 20) {
                        _canLoadMoreListening.value = false
                    } else if (uniqueNew.isNotEmpty()) {
                        val updated = _listeningFeedItems.value + uniqueNew
                        _listeningFeedItems.value = updated
                        _listeningRibbonItems.value = computeListeningRibbon(updated)
                    }
                } else {
                    val raw = feedRepository.getFeedItems(excludeListen = true, limit = 20, offset = generalOffset)
                    generalOffset += raw.size.toLong()
                    if (raw.size < 20) {
                        _canLoadMoreGeneral.value = false
                    }
                    val filtered = raw.filter { it.type != FeedType.LISTEN }
                    val existingIds = _generalFeedItems.value.map { it.id }.toSet()
                    val uniqueNew = filtered.filterNot { it.id in existingIds }
                    if (uniqueNew.isEmpty() && raw.size < 20) {
                        _canLoadMoreGeneral.value = false
                    } else if (uniqueNew.isNotEmpty()) {
                        _generalFeedItems.value = _generalFeedItems.value + uniqueNew
                    }
                }
                _canLoadMore.value = if (isListeningTab) _canLoadMoreListening.value else _canLoadMoreGeneral.value
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                println("Feed loadMore hatası: ${e.message}")
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    suspend fun resolveContentTarget(item: FeedItem): FeedContentTarget {
        val repo = bookRepository ?: return FeedContentTarget.NotFound("İçerik şu anda açılamıyor.")
        val targetId = item.bookId.trim()
        if (targetId.isBlank()) return FeedContentTarget.NotFound("Bağlı bir kitap bulunamadı.")
        if (targetId.startsWith("podcast_")) {
            val resolved = repo.resolvePodcastEpisode(
                bookId = targetId,
                fallbackTitle = item.bookTitle,
                fallbackCover = item.bookCoverUrl,
                fallbackAudioUrl = item.audioUrl.orEmpty(),
                fallbackAuthor = item.userName
            )
            return if (resolved != null) {
                FeedContentTarget.PodcastTarget(resolved.first, resolved.second)
            } else {
                FeedContentTarget.NotFound("Bu podcast bölümü bulunamadı.")
            }
        }
        val direct = repo.getBookById(targetId)
            ?: repo.getBooks().firstOrNull { it.id == targetId }
        return if (direct != null) {
            FeedContentTarget.BookTarget(direct)
        } else {
            FeedContentTarget.NotFound("Bu kitap artık katalogda bulunamıyor.")
        }
    }

    private suspend fun resolvePostingUser(provided: UserProfile?): UserProfile? {
        if (provided != null && provided.uid.isNotBlank()) return provided
        val uid = authRepository?.currentUserUid?.trim()?.takeIf { it.isNotBlank() }
        if (uid != null) {
            val loaded = try {
                authRepository.getUserProfile(uid)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                null
            }
            return loaded ?: UserProfile(uid = uid, name = "KitapDuy Okuru")
        }
        actionError.value = "Paylaşım yapmak için lütfen giriş yapın."
        return null
    }

    fun shareUserPost(text: String, userProfile: UserProfile?) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            actionError.value = "Paylaşmak için bir metin yazın."
            return
        }
        screenModelScope.launch {
            val user = resolvePostingUser(userProfile) ?: return@launch
            _isLoading.value = true
            val success = feedRepository.createPost(
                userId = user.uid,
                userName = user.name.ifBlank { user.username ?: "Kullanıcı" },
                userPhoto = user.photoUrl,
                bookId = "",
                bookName = "",
                comment = cleanText,
                isPremium = user.hasPremium(),
                imageUrl = null,
                audioUrl = null,
                pollOptions = null
            )
            if (success) {
                loadFeed(true)
            } else {
                actionError.value = "Gönderi paylaşılamadı. Lütfen tekrar deneyin."
                _isLoading.value = false
            }
        }
    }

    fun sharePoetryPost(text: String, userProfile: UserProfile?) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            actionError.value = "Paylaşmak için bir mısra yazın."
            return
        }
        screenModelScope.launch {
            val user = resolvePostingUser(userProfile) ?: return@launch
            _isLoading.value = true
            val success = feedRepository.createPost(
                userId = user.uid,
                userName = user.name.ifBlank { user.username ?: "Kullanıcı" },
                userPhoto = user.photoUrl,
                bookId = "",
                bookName = "Şiir",
                comment = cleanText,
                isPremium = user.hasPremium(),
                imageUrl = null,
                audioUrl = null,
                pollOptions = null
            )
            if (success) {
                loadFeed(true)
            } else {
                actionError.value = "Mısra paylaşılamadı. Lütfen tekrar deneyin."
                _isLoading.value = false
            }
        }
    }

    fun sharePoll(question: String, options: List<String>, userProfile: UserProfile?) {
        val cleanQuestion = question.trim()
        val cleanOptions = options.map { it.trim() }.filter { it.isNotBlank() }
        if (cleanQuestion.isBlank()) {
            actionError.value = "Anket sorusu boş bırakılamaz."
            return
        }
        if (cleanOptions.size < 2) {
            actionError.value = "Anket için en az 2 seçenek girmelisiniz."
            return
        }
        screenModelScope.launch {
            val user = resolvePostingUser(userProfile) ?: return@launch
            _isLoading.value = true
            val success = feedRepository.createPost(
                userId = user.uid,
                userName = user.name.ifBlank { user.username ?: "Kullanıcı" },
                userPhoto = user.photoUrl,
                bookId = "",
                bookName = "Anket",
                comment = cleanQuestion,
                isPremium = user.hasPremium(),
                imageUrl = null,
                audioUrl = null,
                pollOptions = cleanOptions
            )
            if (success) {
                loadFeed(true)
            } else {
                actionError.value = "Anket paylaşılamadı. Lütfen tekrar deneyin."
                _isLoading.value = false
            }
        }
    }

    fun shareImagePost(text: String, file: PlatformFile, userProfile: UserProfile?) {
        screenModelScope.launch {
            val user = resolvePostingUser(userProfile) ?: return@launch
            _isLoading.value = true
            try {
                val fileBytes = file.readBytes()
                val extension = file.name.substringAfterLast('.', "")
                uploadAndCreateImagePost(text.trim(), fileBytes, extension, user)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                actionError.value = e.message ?: "Fotoğraf yüklenirken bir hata oluştu."
                _isLoading.value = false
            }
        }
    }

    internal suspend fun uploadAndCreateImagePost(
        text: String,
        fileBytes: ByteArray,
        extension: String,
        user: UserProfile
    ): Boolean {
        _isLoading.value = true
        return try {
            val imageUrlResult = storageRepository.uploadImage(
                "feed_images",
                "post_${Clock.System.now().toEpochMilliseconds()}",
                fileBytes,
                extension
            )
            imageUrlResult.fold(
                onSuccess = { uploadedUrl ->
                    val success = feedRepository.createPost(
                        userId = user.uid,
                        userName = user.name.ifBlank { user.username ?: "Kullanıcı" },
                        userPhoto = user.photoUrl,
                        bookId = "",
                        bookName = "",
                        comment = text,
                        isPremium = user.hasPremium(),
                        imageUrl = uploadedUrl,
                        audioUrl = null,
                        pollOptions = null
                    )
                    if (success) {
                        loadFeed(true)
                        true
                    } else {
                        actionError.value = "Görsel gönderi paylaşılamadı. Lütfen tekrar deneyin."
                        _isLoading.value = false
                        false
                    }
                },
                onFailure = { err ->
                    actionError.value = err.message ?: "Fotoğraf yüklenemedi. Lütfen tekrar deneyin."
                    _isLoading.value = false
                    false
                }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            actionError.value = e.message ?: "Fotoğraf yüklenirken bir hata oluştu."
            _isLoading.value = false
            false
        }
    }
}
