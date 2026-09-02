package com.semtinazizi.kitapduy.ui.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.semtinazizi.kitapduy.data.SupabaseManager
import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.model.ChatMessage
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.decodeRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import io.github.jan.supabase.realtime.RealtimeChannel

class ChatScreenModel(
    private val authRepository: AuthRepository,
    private val chatRoomName: String = "☕ Genel"
) : ScreenModel {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var channel: RealtimeChannel? = null

    init {
        loadMessages()
        // Realtime plugin devre dışı - crash önleniyor
        // setupRealtimeSubscription()
    }

    private fun loadMessages() {
        screenModelScope.launch {
            _isLoading.value = true
            try {
                val result = SupabaseManager.postgrest.from("chat_messages")
                    .select {
                        filter {
                            eq("room", chatRoomName)
                        }
                        order("created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        limit(50)
                    }.decodeList<ChatMessage>()
                
                _messages.value = result
            } catch (e: Throwable) {
                _errorMessage.value = "Mesajlar yüklenemedi: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun setupRealtimeSubscription() {
        screenModelScope.launch {
            try {
                channel = SupabaseManager.client.channel("public:chat_messages")
                
                channel?.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "chat_messages"
                }?.onEach { action ->
                    val newMessage = action.decodeRecord<ChatMessage>()
                    if (newMessage.room == chatRoomName) {
                        // Insert new message at the top (since list is descending / reversed)
                        val currentList = _messages.value.toMutableList()
                        // Avoid duplicates if we just sent it
                        if (currentList.none { it.id == newMessage.id && it.id != null }) {
                            currentList.add(0, newMessage)
                            _messages.value = currentList
                        }
                    }
                }?.catch { e ->
                    _errorMessage.value = "Canlı bağlantı koptu: ${e.message}"
                }?.launchIn(screenModelScope)

                channel?.subscribe()
            } catch (e: Throwable) {
                _errorMessage.value = "Canlı sohbet başlatılamadı."
            }
        }
    }

    fun sendMessage(text: String) {
        val currentUid = authRepository.currentUserUid
        if (currentUid == null || text.isBlank()) return

        screenModelScope.launch {
            try {
                val userProfile = authRepository.getUserProfile(currentUid)
                val newMessage = ChatMessage(
                    userId = currentUid,
                    userName = userProfile?.name ?: "Anonim",
                    userPhoto = userProfile?.photoUrl,
                    message = text,
                    isPremium = userProfile?.hasPremium() ?: false,
                    room = chatRoomName,
                    createdAt = Clock.System.now().toString()
                )

                // Optimistic UI update
                val currentList = _messages.value.toMutableList()
                currentList.add(0, newMessage)
                _messages.value = currentList

                SupabaseManager.postgrest.from("chat_messages").insert(newMessage)
            } catch (e: Throwable) {
                _errorMessage.value = "Mesaj gönderilemedi."
                // Revert optimistic update
                loadMessages() 
            }
        }
    }

    override fun onDispose() {
        super.onDispose()
        screenModelScope.launch {
            channel?.unsubscribe()
        }
    }
}
