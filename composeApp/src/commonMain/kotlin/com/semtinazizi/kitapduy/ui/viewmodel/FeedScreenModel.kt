package com.semtinazizi.kitapduy.ui.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.semtinazizi.kitapduy.domain.repository.FeedRepository
import com.semtinazizi.kitapduy.data.repository.StorageRepository
import com.semtinazizi.kitapduy.model.FeedItem
import com.semtinazizi.kitapduy.model.FeedType
import com.semtinazizi.kitapduy.model.UserProfile
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class FeedScreenModel(
    private val feedRepository: FeedRepository,
    private val storageRepository: StorageRepository
) : ScreenModel {

    private val _generalFeedItems = MutableStateFlow<List<FeedItem>>(emptyList())
    val generalFeedItems: StateFlow<List<FeedItem>> = _generalFeedItems.asStateFlow()

    private val _listeningFeedItems = MutableStateFlow<List<FeedItem>>(emptyList())
    val listeningFeedItems: StateFlow<List<FeedItem>> = _listeningFeedItems.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadFeed()
        feedRepository.startRealtimeListening { newFeedItem ->
            if (newFeedItem.type == FeedType.LISTEN) {
                val current = _listeningFeedItems.value.toMutableList()
                current.add(0, newFeedItem)
                _listeningFeedItems.value = current
            } else {
                val current = _generalFeedItems.value.toMutableList()
                current.add(0, newFeedItem)
                _generalFeedItems.value = current
            }
        }
    }

    override fun onDispose() {
        feedRepository.stopRealtimeListening()
        super.onDispose()
    }

    fun loadFeed(isRefresh: Boolean = false) {
        if (isRefresh) {
            _isLoading.value = true
        }

        screenModelScope.launch {
            try {
                // Mısralar için dinlemeleri hariç tutarak son 50 öğeyi getir
                val mappedItems = feedRepository.getFeedItems(excludeListen = true)
                _generalFeedItems.value = mappedItems.filter { it.type != FeedType.LISTEN }
                
                // Etkinlik sekmesi için tüm öğeleri getir
                if (isRefresh || _listeningFeedItems.value.isEmpty()) {
                    val listeningItems = feedRepository.getFeedItems(excludeListen = false)
                    _listeningFeedItems.value = listeningItems.filter { it.type == FeedType.LISTEN }
                }

                _isLoading.value = false
            } catch (e: Throwable) {
                println("Feed yüklenirken hata oluştu: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun shareUserPost(text: String, userProfile: UserProfile?) {
        val user = userProfile ?: return
        screenModelScope.launch {
            _isLoading.value = true
            val success = feedRepository.createPost(
                userId = user.uid,
                userName = user.name.ifBlank { user.username ?: "Kullanıcı" },
                userPhoto = user.photoUrl,
                bookId = "",
                bookName = "",
                comment = text,
                isPremium = user.hasPremium(),
                imageUrl = null,
                audioUrl = null,
                pollOptions = null
            )
            if (success) loadFeed(true) else _isLoading.value = false
        }
    }

    fun sharePoetryPost(text: String, userProfile: UserProfile?) {
        val user = userProfile ?: return
        screenModelScope.launch {
            _isLoading.value = true
            val success = feedRepository.createPost(
                userId = user.uid,
                userName = user.name.ifBlank { user.username ?: "Kullanıcı" },
                userPhoto = user.photoUrl,
                bookId = "",
                bookName = "Şiir",
                comment = text,
                isPremium = user.hasPremium(),
                imageUrl = null,
                audioUrl = null,
                pollOptions = null
            )
            if (success) loadFeed(true) else _isLoading.value = false
        }
    }

    fun sharePoll(question: String, options: List<String>, userProfile: UserProfile?) {
        val user = userProfile ?: return
        screenModelScope.launch {
            _isLoading.value = true
            val success = feedRepository.createPost(
                userId = user.uid,
                userName = user.name.ifBlank { user.username ?: "Kullanıcı" },
                userPhoto = user.photoUrl,
                bookId = "",
                bookName = "",
                comment = question,
                isPremium = user.hasPremium(),
                imageUrl = null,
                audioUrl = null,
                pollOptions = options
            )
            if (success) loadFeed(true) else _isLoading.value = false
        }
    }

    fun shareImagePost(text: String, file: PlatformFile, userProfile: UserProfile?) {
        val user = userProfile ?: return
        screenModelScope.launch {
            _isLoading.value = true
            try {
                val fileBytes = file.readBytes()
                // Use substringAfterLast instead of extension, similar to AskidaViewModel
                val extension = file.name.substringAfterLast('.', "")
                val imageUrlResult = storageRepository.uploadImage(
                    "social_feed",
                    "post_${Clock.System.now().toEpochMilliseconds()}",
                    fileBytes,
                    extension
                )
                val imageUrl = imageUrlResult.getOrNull()
                
                val success = feedRepository.createPost(
                    userId = user.uid,
                    userName = user.name.ifBlank { user.username ?: "Kullanıcı" },
                    userPhoto = user.photoUrl,
                    bookId = "",
                    bookName = "",
                    comment = text,
                    isPremium = user.hasPremium(),
                    imageUrl = imageUrl,
                    audioUrl = null,
                    pollOptions = null
                )
                if (success) loadFeed(true) else _isLoading.value = false
            } catch (e: Throwable) {
                println("Resim yükleme/paylaşma hatası: ${e.message}")
                _isLoading.value = false
            }
        }
    }

    fun createDummyPost() {
        shareUserPost("Bu bir test gönderisidir!", null)
    }
}
