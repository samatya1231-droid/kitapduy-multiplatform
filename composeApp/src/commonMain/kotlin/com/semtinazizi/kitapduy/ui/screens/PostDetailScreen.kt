package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.model.Comment
import com.semtinazizi.kitapduy.ui.components.InteractiveFeedCard
import com.semtinazizi.kitapduy.ui.viewmodel.PostDetailScreenModel
import com.semtinazizi.kitapduy.ui.viewmodel.HomeViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

class PostDetailScreen(private val postId: String) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = koinScreenModel<PostDetailScreenModel>()
        val auth = koinInject<AuthRepository>()
        val home = koinViewModel<HomeViewModel>()
        val books by home.books.collectAsState()
        val post by model.post.collectAsState()
        val comments by model.comments.collectAsState()
        val loading by model.loading.collectAsState()
        val sending by model.sending.collectAsState()
        val error by model.error.collectAsState()
        val draft by model.draft.collectAsState()
        var editing by remember { mutableStateOf<Comment?>(null) }
        var editText by remember { mutableStateOf("") }
        var deleting by remember { mutableStateOf<Comment?>(null) }
        LaunchedEffect(postId) { model.load(postId) }
        editing?.let { comment -> AlertDialog(onDismissRequest = { editing = null }, title = { Text("Yorumu düzenle") },
            text = { Column {
                OutlinedTextField(editText, { if (it.length <= 2000) editText = it }, label = { Text("Yorum") }, modifier = Modifier.testTag("post-edit-comment-input"))
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { TextButton(enabled = editText.isNotBlank(), onClick = { model.edit(comment.id, editText) { editing = null } }) { Text("Kaydet") } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Vazgeç") } }) }
        deleting?.let { comment -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Yorumu sil?") },
            confirmButton = { TextButton(onClick = { model.delete(comment.id); deleting = null }) { Text("Sil") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Vazgeç") } }) }
        Scaffold(topBar = { TopAppBar(title = { Text("Gönderi") }, navigationIcon = {
            IconButton(onClick = { navigator.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
        }) }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                error?.let { message -> Column(Modifier.padding(16.dp)) {
                    Text(message, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { model.load(postId) }) { Text("Yeniden dene") }
                } }
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    post?.let { current -> item {
                        InteractiveFeedCard(current, onChanged = { model.load(postId) }, onDeleted = { navigator.pop() },
                            showComments = false, onOpenContent = {
                                books.firstOrNull { it.id == current.bookId }?.let { navigator.push(BookDetailScreen(it)) }
                            })
                    } }
                    item { Text("Yorumlar (${comments.size})", style = MaterialTheme.typography.titleMedium) }
                    if (comments.isEmpty() && !loading && post != null) item { Text("Henüz yorum yok. İlk yorumu sen yap!") }
                    items(comments, key = { it.id }) { comment ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(comment.userName, style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.clickable { navigator.push(UserProfileScreen(comment.userId)) })
                                Text(comment.text, Modifier.padding(vertical = 8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { model.like(comment.id) }, enabled = auth.currentUserUid != null) {
                                        Text((if (auth.currentUserUid in comment.likedBy) "Beğenildi" else "Beğen") + " (${comment.likedBy.size})")
                                    }
                                    if (comment.userId == auth.currentUserUid) {
                                        TextButton(onClick = { editing = comment; editText = comment.text }) { Text("Düzenle") }
                                        TextButton(onClick = { deleting = comment }) { Text("Sil") }
                                    }
                                }
                            }
                        }
                    }
                }
                if (auth.currentUserUid == null) Text("Yorum yazmak için giriş yapın.", Modifier.padding(16.dp))
                else Row(Modifier.fillMaxWidth().padding(12.dp).navigationBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(draft, { if (it.length <= 2000) model.draft.value = it }, Modifier.weight(1f).testTag("post-comment-input"),
                        label = { Text("Yorum yaz…") }, maxLines = 4, enabled = !sending)
                    TextButton(onClick = model::send, modifier = Modifier.testTag("post-comment-send"), enabled = !sending && draft.isNotBlank() && post != null) {
                        if (sending) CircularProgressIndicator(Modifier.size(20.dp)) else Text("Gönder")
                    }
                }
            }
        }
    }
}
