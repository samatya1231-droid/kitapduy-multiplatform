package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.util.root
import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.domain.repository.BookRepository
import com.semtinazizi.kitapduy.model.NotificationItem
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class NotificationScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val client = koinInject<SupabaseClient>()
        val auth = koinInject<AuthRepository>()
        val books = koinInject<BookRepository>()
        val scope = rememberCoroutineScope()
        var notes by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
        var loading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }
        var retry by remember { mutableIntStateOf(0) }
        var readError by remember { mutableStateOf(false) }
        var markingRead by remember { mutableStateOf(false) }
        var opening by remember { mutableStateOf(false) }
        LaunchedEffect(retry, auth.currentUserUid) {
            loading = true
            error = null
            readError = false
            notes = emptyList()
            try {
                val uid = auth.currentUserUid
                if (uid == null) error = "Bildirimlerini görmek için giriş yap."
                else notes = client.postgrest["notifications"].select {
                    filter { eq("receiver_id", uid) }
                    order("created_at", Order.DESCENDING)
                }.decodeList<NotificationItem>()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { error = "Bildirimler yüklenemedi. Tekrar dene." }
            finally { loading = false }
        }
        Scaffold(topBar = {
            TopAppBar(title = { Text("Bildirimler") },
                navigationIcon = { IconButton(onClick = { navigator.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") } },
                actions = { IconButton(enabled = !loading, onClick = { retry++ }) { Icon(Icons.Default.Refresh, "Yenile") } })
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (loading || opening) LinearProgressIndicator(Modifier.fillMaxWidth())
                error?.let { message ->
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(message)
                        TextButton(onClick = { if (auth.currentUserUid == null) navigator.root.push(LoginScreen()) else retry++ }) {
                            Text(if (auth.currentUserUid == null) "Giriş yap" else "Tekrar dene")
                        }
                    }
                }
                if (!loading && error == null && notes.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Henüz bir bildirim yok.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (notes.any { !it.isRead && it.id != null } && !loading) {
                    TextButton(enabled = !markingRead, onClick = {
                        val uid = auth.currentUserUid ?: return@TextButton
                        val unreadIds = notes.filter { !it.isRead }.mapNotNull { it.id }
                        markingRead = true
                        readError = false
                        scope.launch {
                            try {
                                client.postgrest["notifications"].update(mapOf("is_read" to true)) {
                                    filter { eq("receiver_id", uid); isIn("id", unreadIds) }
                                }
                                if (auth.currentUserUid == uid) notes = notes.map {
                                    if (it.id in unreadIds) it.copy(isRead = true) else it
                                }
                            } catch (cancelled: CancellationException) { throw cancelled }
                            catch (failure: Exception) { readError = true }
                            finally { markingRead = false }
                        }
                    }) { Text(if (markingRead) "Güncelleniyor…" else "Tümünü okundu işaretle") }
                }
                if (readError) Text("Okundu durumu kaydedilemedi. Tekrar dene.",
                    Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.error)
                LazyColumn {
                    items(notes) { note ->
                        ListItem(headlineContent = { Text(note.message) },
                            supportingContent = { if (note.timestamp > 0) Text(com.semtinazizi.kitapduy.ui.components.getRelativeTime(note.timestamp)) },
                            leadingContent = { Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary) },
                            colors = ListItemDefaults.colors(containerColor = if (note.isRead) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.clickable(enabled = !opening && !note.bookId.isNullOrBlank()) {
                                val target = note.bookId.orEmpty()
                                val uid = auth.currentUserUid
                                if (!note.isRead && note.id != null && !uid.isNullOrBlank()) {
                                    val noteId = note.id
                                    notes = notes.map { if (it.id == noteId) it.copy(isRead = true) else it }
                                    scope.launch {
                                        runCatching {
                                            client.postgrest["notifications"].update(mapOf("is_read" to true)) {
                                                filter { eq("receiver_id", uid); eq("id", noteId) }
                                            }
                                        }
                                    }
                                }
                                when (note.type.lowercase()) {
                                    "like", "comment", "reply", "poll_vote" -> navigator.push(PostDetailScreen(target))
                                    "follow" -> navigator.push(UserProfileScreen(target))
                                    else -> scope.launch {
                                        opening = true
                                        try {
                                            if (target.startsWith("podcast_")) {
                                                val resolved = books.resolvePodcastEpisode(target, fallbackTitle = note.message)
                                                if (resolved != null) {
                                                    navigator.push(PodcastDetailScreen(resolved.first, resolved.second))
                                                } else {
                                                    error = "Bu bildirimin podcast bölümü artık bulunamıyor."
                                                }
                                            } else {
                                                val book = books.getBookById(target) ?: books.getBooks().find { it.id == target }
                                                if (book != null) navigator.push(BookDetailScreen(book))
                                                else error = "Bu bildirimin içeriği artık bulunamıyor."
                                            }
                                        } catch (cancelled: CancellationException) { throw cancelled }
                                        catch (failure: Exception) { error = "İçerik açılamadı. Tekrar dene." }
                                        finally { opening = false }
                                    }
                                }
                            })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
