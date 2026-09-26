package com.semtinazizi.kitapduy.ui.components

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.data.repository.FeedActionsRepository
import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.domain.repository.SocialRepository
import com.semtinazizi.kitapduy.model.FeedItem
import com.semtinazizi.kitapduy.ui.screens.*
import com.semtinazizi.kitapduy.ui.viewmodel.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** One set of actions for the feed, profile and post detail. */
@Composable
fun InteractiveFeedCard(item: FeedItem, onChanged: () -> Unit, onDeleted: () -> Unit = onChanged,
                        showComments: Boolean = true, onOpenContent: (() -> Unit)? = null) {
    val navigator = LocalNavigator.currentOrThrow
    val actions = koinInject<FeedActionsRepository>()
    val social = koinInject<SocialRepository>()
    val auth = koinInject<AuthRepository>()
    val authModel = koinViewModel<AuthViewModel>()
    val profile by authModel.userProfile.collectAsState()
    val audio = koinInject<GlobalAudioController>()
    val audioBook by audio.currentBook.collectAsState()
    val playing by audio.isPlaying.collectAsState()
    val position by audio.currentPosition.collectAsState()
    val scope = rememberCoroutineScope()
    var pending by remember(item.id) { mutableStateOf(false) }
    var error by remember(item.id) { mutableStateOf<String?>(null) }
    var image by remember(item.id) { mutableStateOf<String?>(null) }
    var confirmDelete by remember(item.id) { mutableStateOf(false) }
    fun perform(action: suspend () -> Unit) {
        if (pending) return
        pending = true
        scope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "İşlem tamamlanamadı." }
            finally { pending = false }
        }
    }
    error?.let { message -> AlertDialog(onDismissRequest = { error = null }, text = { Text(message) },
        confirmButton = { TextButton(onClick = { error = null }) { Text("Tamam") } }) }
    image?.let { url -> AlertDialog(onDismissRequest = { image = null },
        text = { coil3.compose.AsyncImage(url, "Gönderi fotoğrafı", Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(onClick = { image = null }) { Text("Kapat") } }) }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Gönderiyi sil?") },
        text = { Text("Bu gönderi kalıcı olarak silinecek.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; perform { actions.deletePost(item.id); onDeleted() } }) { Text("Sil") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Vazgeç") } })
    FeedCard(item = item, currentUserId = profile?.uid ?: auth.currentUserUid,
        isAdmin = profile?.badges?.contains("admin") == true,
        onClick = { if (onOpenContent != null) onOpenContent() else navigator.push(PostDetailScreen(item.id)) },
        onUserClick = { navigator.push(UserProfileScreen(item.userId)) },
        onImageClick = { image = it }, onDeletePost = { confirmDelete = true },
        onToggleLike = { id, _, reaction -> perform { actions.toggleLike(id, reaction); onChanged() } },
        onVotePoll = { id, option -> perform { actions.vote(id, option); onChanged() } },
        onPlayAudio = { url, start ->
            if (audioBook?.id == "feed_" + item.id) { if (!playing) audio.togglePlayPause() }
            else audio.playFeedAudio(item, url, start ?: 0L)
        },
        onPauseAudio = { if (playing && audioBook?.id == "feed_" + item.id) audio.togglePlayPause() },
        isAudioPlaying = playing && audioBook?.id == "feed_" + item.id,
        isAudioLoading = false, audioProgress = if (audioBook?.id == "feed_" + item.id) position else 0f,
        showCommentSection = showComments,
        onReportPost = { id, reason -> perform {
            val uid = auth.currentUserUid ?: error("Lütfen giriş yapın.")
            check(social.reportPost(id, uid, reason)) { "Şikayet iletilemedi." }
        } },
        onBlockUser = { id -> perform {
            val uid = auth.currentUserUid ?: error("Lütfen giriş yapın.")
            check(social.blockUser(uid, id)) { "Kullanıcı engellenemedi." }
            onDeleted()
        } })
}
