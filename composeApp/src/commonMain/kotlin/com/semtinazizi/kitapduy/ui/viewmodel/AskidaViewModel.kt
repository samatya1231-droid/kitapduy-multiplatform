package com.semtinazizi.kitapduy.ui.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.semtinazizi.kitapduy.data.SupabaseManager
import com.semtinazizi.kitapduy.data.repository.CrossPlatformBridgeHelper
import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.model.AskidaKitap
import com.semtinazizi.kitapduy.model.AskidaMesaj
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.decodeRecord
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import com.semtinazizi.kitapduy.util.UrlSanitizer

class AskidaViewModel(
    private val authRepository: AuthRepository,
    private val storageRepository: com.semtinazizi.kitapduy.data.repository.StorageRepository,
    private val httpClient: io.ktor.client.HttpClient? = null
) : ScreenModel {

    private val _availableBooks = MutableStateFlow<List<AskidaKitap>>(emptyList())
    val availableBooks: StateFlow<List<AskidaKitap>> = _availableBooks.asStateFlow()

    private val _myChats = MutableStateFlow<List<AskidaKitap>>(emptyList())
    val myChats: StateFlow<List<AskidaKitap>> = _myChats.asStateFlow()

    private val _currentChatMessages = MutableStateFlow<List<AskidaMesaj>>(emptyList())
    val currentChatMessages: StateFlow<List<AskidaMesaj>> = _currentChatMessages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var booksChannel: RealtimeChannel? = null
    private var messagesChannel: RealtimeChannel? = null
    private var activeBookId: String? = null

    private val localFallbackBooks = mutableListOf<AskidaKitap>()
    private val localFallbackMessages = mutableMapOf<String, MutableList<AskidaMesaj>>()

    init {
        // Realtime plugin devre dışı - sadece fetch kullan
        // setupBooksRealtimeSubscription()
    }

    private fun setupBooksRealtimeSubscription() {
        screenModelScope.launch {
            try {
                booksChannel = SupabaseManager.client.channel("public:askida_kitap")
                
                booksChannel?.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "askida_kitap"
                }?.onEach { _ ->
                    // Re-fetch books on any change (insert/update/delete)
                    // For a production app with huge lists, we might update the list in-place
                    loadAskidaBooks()
                    loadMyChats()
                }?.catch {
                    // Log or handle error
                }?.launchIn(screenModelScope)

                booksChannel?.subscribe()
            } catch (e: Throwable) {
                // Ignore or log
            }
        }
    }

    private fun fixUrl(url: String?): String {
        val publicBase = com.semtinazizi.kitapduy.platform.AppConfig.cfPublicUrl.trim().trimEnd('/')
        val defaultBase = if (publicBase.startsWith("http://localhost") || publicBase.startsWith("http://127.0.0.1")) {
            publicBase
        } else {
            com.semtinazizi.kitapduy.util.UrlSanitizer.CUSTOM_R2_BASE_URL
        }
        return com.semtinazizi.kitapduy.util.UrlSanitizer.normalizeMediaUrl(url, defaultBase)
    }

    fun loadAskidaBooks() {
        screenModelScope.launch {
            _isLoading.value = true
            try {
                val books = if (!CrossPlatformBridgeHelper.isLocalFixture()) {
                    CrossPlatformBridgeHelper.fetchAskidaAvailable(httpClient)
                } else {
                    SupabaseManager.postgrest.from("askida_kitap")
                        .select {
                            filter { eq("status", "AVAILABLE") }
                            order("timestamp", order = Order.DESCENDING)
                        }.decodeList<AskidaKitap>()
                }.map { it.copy(imageUrl = fixUrl(it.imageUrl)) }
                val merged = (localFallbackBooks.filter { local ->
                    local.status == "AVAILABLE" && books.none { it.id == local.id }
                } + books).ifEmpty {
                    localFallbackBooks.filter { it.status == "AVAILABLE" }
                }
                _availableBooks.value = merged
            } catch (_: Throwable) {
                _availableBooks.value = localFallbackBooks.filter { it.status == "AVAILABLE" }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMyChats() {
        screenModelScope.launch {
            val currentUid = authRepository.currentUserUid ?: "local_guest"
            _isLoading.value = true
            try {
                val books = if (!CrossPlatformBridgeHelper.isLocalFixture()) {
                    CrossPlatformBridgeHelper.fetchAskidaMyChats(httpClient, currentUid)
                } else {
                    SupabaseManager.postgrest.from("askida_kitap")
                        .select {
                            filter {
                                // Donor = me OR Claimer = me
                                or {
                                    eq("donor_id", currentUid)
                                    eq("claimer_id", currentUid)
                                }
                            }
                            order("timestamp", order = Order.DESCENDING)
                        }.decodeList<AskidaKitap>()
                }.map { it.copy(imageUrl = fixUrl(it.imageUrl)) }
                val localChats = localFallbackBooks.filter {
                    it.status != "AVAILABLE" || it.donorId == currentUid || it.claimerId == currentUid
                }
                _myChats.value = (localChats.filter { l -> books.none { it.id == l.id } } + books)
            } catch (_: Throwable) {
                _myChats.value = localFallbackBooks.filter {
                    it.status != "AVAILABLE" || it.donorId == currentUid || it.claimerId == currentUid
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun claimBook(book: AskidaKitap) {
        screenModelScope.launch {
            val currentUid = authRepository.currentUserUid ?: "local_guest"
            val updatedBook = book.copy(status = "CLAIMED", claimerId = currentUid)

            val idx = localFallbackBooks.indexOfFirst { it.id == book.id }
            if (idx >= 0) {
                localFallbackBooks[idx] = updatedBook
            } else {
                localFallbackBooks.add(0, updatedBook)
            }
            _availableBooks.value = _availableBooks.value.filter { it.id != book.id }
            _myChats.value = listOf(updatedBook) + _myChats.value.filter { it.id != book.id }

            if (!CrossPlatformBridgeHelper.isLocalFixture()) {
                CrossPlatformBridgeHelper.claimAskida(httpClient, book.id, currentUid)
            } else {
                runCatching {
                    SupabaseManager.postgrest.from("askida_kitap")
                        .update(
                            {
                                set("status", "CLAIMED")
                                set("claimer_id", currentUid)
                            }
                        ) {
                            filter { eq("id", book.id) }
                        }
                }
            }
        }
    }

    fun donateBook(bookName: String, city: String, condition: String, note: String, category: String, imageBytes: ByteArray?, imageExtension: String?, onResult: (Boolean) -> Unit) {
        screenModelScope.launch {
            _isLoading.value = true
            val currentUid = authRepository.currentUserUid ?: "local_guest"
            try {
                var uploadedUrl = ""
                if (imageBytes != null && imageExtension != null) {
                    val uploadResult = runCatching {
                        storageRepository.uploadImage("askida_kitap", "book_${currentUid}", imageBytes, imageExtension)
                    }.getOrNull()
                    if (uploadResult?.isSuccess == true) {
                        uploadedUrl = uploadResult.getOrNull() ?: ""
                    }
                }

                val userProfile = runCatching { authRepository.getUserProfile(currentUid) }.getOrNull()
                val newBook = AskidaKitap(
                    id = Clock.System.now().toEpochMilliseconds().toString(),
                    bookName = bookName,
                    donorId = currentUid,
                    donorName = userProfile?.name ?: "Okur",
                    donorNote = note,
                    category = category,
                    city = city,
                    condition = condition,
                    imageUrl = uploadedUrl,
                    timestamp = Clock.System.now().toEpochMilliseconds()
                )

                localFallbackBooks.add(0, newBook)
                _availableBooks.value = listOf(newBook) + _availableBooks.value

                if (!CrossPlatformBridgeHelper.isLocalFixture()) {
                    CrossPlatformBridgeHelper.donateAskida(httpClient, newBook)
                } else {
                    runCatching {
                        SupabaseManager.postgrest.from("askida_kitap").insert(newBook)
                    }
                }
                onResult(true)
            } catch (_: Throwable) {
                onResult(true)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadChatMessages(bookId: String) {
        activeBookId = bookId
        setupMessagesRealtimeSubscription(bookId)

        screenModelScope.launch {
            try {
                val messages = if (!CrossPlatformBridgeHelper.isLocalFixture()) {
                    CrossPlatformBridgeHelper.fetchAskidaMessages(httpClient, bookId)
                } else {
                    SupabaseManager.postgrest.from("askida_mesaj")
                        .select {
                            filter { eq("book_id", bookId) }
                            order("timestamp", order = Order.ASCENDING)
                        }.decodeList<AskidaMesaj>()
                }
                val localMsgs = localFallbackMessages[bookId].orEmpty()
                val combined = (messages + localMsgs.filter { l -> messages.none { it.id == l.id } })
                    .sortedBy { it.timestamp }
                _currentChatMessages.value = combined
            } catch (_: Throwable) {
                _currentChatMessages.value = localFallbackMessages[bookId].orEmpty()
            }
        }
    }

    private fun setupMessagesRealtimeSubscription(bookId: String) {
        screenModelScope.launch {
            // Unsubscribe existing message channel if any
            messagesChannel?.unsubscribe()
            
            try {
                messagesChannel = SupabaseManager.client.channel("public:askida_mesaj:$bookId")
                
                messagesChannel?.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "askida_mesaj"
                }?.onEach { action ->
                    val newMessage = action.decodeRecord<AskidaMesaj>()
                    if (newMessage.bookId == activeBookId) {
                        val currentList = _currentChatMessages.value.toMutableList()
                        if (currentList.none { it.id == newMessage.id }) {
                            currentList.add(newMessage)
                            // Ascending list means add to the end
                            _currentChatMessages.value = currentList
                        }
                    }
                }?.launchIn(screenModelScope)

                messagesChannel?.subscribe()
            } catch (e: Throwable) {
                // Ignore
            }
        }
    }

    fun sendMessage(bookId: String, text: String) {
        screenModelScope.launch {
            val currentUid = authRepository.currentUserUid ?: "local_guest"
            val newMessage = AskidaMesaj(
                id = Clock.System.now().toEpochMilliseconds().toString(),
                bookId = bookId,
                senderId = currentUid,
                text = text,
                timestamp = Clock.System.now().toEpochMilliseconds()
            )

            localFallbackMessages.getOrPut(bookId) { mutableListOf() }.add(newMessage)
            _currentChatMessages.value = _currentChatMessages.value + newMessage

            if (!CrossPlatformBridgeHelper.isLocalFixture()) {
                CrossPlatformBridgeHelper.sendAskidaMessage(httpClient, newMessage)
            } else {
                runCatching {
                    SupabaseManager.postgrest.from("askida_mesaj").insert(newMessage)
                }
            }
        }
    }

    override fun onDispose() {
        super.onDispose()
        screenModelScope.launch {
            booksChannel?.unsubscribe()
            messagesChannel?.unsubscribe()
        }
    }
}
