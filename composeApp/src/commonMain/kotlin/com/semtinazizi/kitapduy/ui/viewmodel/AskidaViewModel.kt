package com.semtinazizi.kitapduy.ui.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.semtinazizi.kitapduy.data.SupabaseManager
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

class AskidaViewModel(
    private val authRepository: AuthRepository,
    private val storageRepository: com.semtinazizi.kitapduy.data.repository.StorageRepository
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
            } catch (e: Exception) {
                // Ignore or log
            }
        }
    }

    private fun fixUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""
        if (url.startsWith("http")) return url
        return "${com.semtinazizi.kitapduy.platform.AppConfig.cfPublicUrl.trimEnd('/')}/${url.trimStart('/')}"
    }

    fun loadAskidaBooks() {
        screenModelScope.launch {
            _isLoading.value = true
            try {
                val books = SupabaseManager.postgrest.from("askida_kitap")
                    .select {
                        filter { eq("status", "AVAILABLE") }
                        order("timestamp", order = Order.DESCENDING)
                    }.decodeList<AskidaKitap>()
                _availableBooks.value = books.map { it.copy(imageUrl = fixUrl(it.imageUrl)) }
            } catch (e: Exception) {
                // Hata durumu
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMyChats() {
        screenModelScope.launch {
            val currentUid = authRepository.currentUserUid ?: return@launch
            _isLoading.value = true
            try {
                val books = SupabaseManager.postgrest.from("askida_kitap")
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
                _myChats.value = books.map { it.copy(imageUrl = fixUrl(it.imageUrl)) }
            } catch (e: Exception) {
                // Hata durumu
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun claimBook(book: AskidaKitap) {
        screenModelScope.launch {
            val currentUid = authRepository.currentUserUid ?: return@launch
            try {
                val updatedBook = book.copy(status = "CLAIMED", claimerId = currentUid)
                
                // Optimistic UI Update
                _availableBooks.value = _availableBooks.value.filter { it.id != book.id }
                _myChats.value = listOf(updatedBook) + _myChats.value

                SupabaseManager.postgrest.from("askida_kitap")
                    .update(
                        {
                            set("status", "CLAIMED")
                            set("claimer_id", currentUid)
                        }
                    ) {
                        filter { eq("id", book.id) }
                    }
            } catch (e: Exception) {
                // Revert if failed
                loadAskidaBooks()
                loadMyChats()
            }
        }
    }

    fun donateBook(bookName: String, city: String, condition: String, note: String, category: String, imageBytes: ByteArray?, imageExtension: String?, onResult: (Boolean) -> Unit) {
        screenModelScope.launch {
            _isLoading.value = true
            val currentUid = authRepository.currentUserUid
            if (currentUid == null) {
                _isLoading.value = false
                onResult(false)
                return@launch
            }
            try {
                var uploadedUrl = ""
                if (imageBytes != null && imageExtension != null) {
                    val uploadResult = storageRepository.uploadImage("askida_kitap", "book_${currentUid}", imageBytes, imageExtension)
                    if (uploadResult.isSuccess) {
                        uploadedUrl = uploadResult.getOrNull() ?: ""
                    }
                }

                val userProfile = authRepository.getUserProfile(currentUid)
                val newBook = AskidaKitap(
                    id = Clock.System.now().toEpochMilliseconds().toString(),
                    bookName = bookName,
                    donorId = currentUid,
                    donorName = userProfile?.name ?: "Anonim",
                    donorNote = note,
                    category = category,
                    city = city,
                    condition = condition,
                    imageUrl = uploadedUrl,
                    timestamp = Clock.System.now().toEpochMilliseconds()
                )
                
                // Optimistic
                _availableBooks.value = listOf(newBook) + _availableBooks.value

                SupabaseManager.postgrest.from("askida_kitap").insert(newBook)
                onResult(true)
            } catch (e: Exception) {
                // Revert
                loadAskidaBooks()
                onResult(false)
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
                val messages = SupabaseManager.postgrest.from("askida_mesaj")
                    .select {
                        filter { eq("book_id", bookId) }
                        order("timestamp", order = Order.ASCENDING)
                    }.decodeList<AskidaMesaj>()
                _currentChatMessages.value = messages
            } catch (e: Exception) {
                // Hata
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
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun sendMessage(bookId: String, text: String) {
        screenModelScope.launch {
            val currentUid = authRepository.currentUserUid ?: return@launch
            try {
                val newMessage = AskidaMesaj(
                    id = Clock.System.now().toEpochMilliseconds().toString(),
                    bookId = bookId,
                    senderId = currentUid,
                    text = text,
                    timestamp = Clock.System.now().toEpochMilliseconds()
                )
                
                // Optimistic
                _currentChatMessages.value = _currentChatMessages.value + newMessage

                SupabaseManager.postgrest.from("askida_mesaj").insert(newMessage)
            } catch (e: Exception) {
                // Revert or reload
                loadChatMessages(bookId)
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
