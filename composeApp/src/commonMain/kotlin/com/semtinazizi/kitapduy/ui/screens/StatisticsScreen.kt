package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.domain.repository.BookRepository
import com.semtinazizi.kitapduy.domain.repository.SocialRepository
import com.semtinazizi.kitapduy.ui.theme.PlayfairDisplayFamily
import kotlinx.coroutines.CancellationException
import org.koin.compose.koinInject

class StatisticsScreen : Screen {
    @Composable override fun Content() { ReadingSummaryContent(false) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReadingSummaryContent(premium: Boolean) {
    val navigator = LocalNavigator.currentOrThrow
    val auth = koinInject<AuthRepository>()
    val social = koinInject<SocialRepository>()
    val catalog = koinInject<BookRepository>()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var finished by remember { mutableIntStateOf(0) }
    var categories by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(retry, auth.currentUserUid) {
        loading = true
        error = null
        try {
            val uid = auth.currentUserUid
            if (uid == null) error = "Kitaplık özetini görmek için giriş yap."
            else {
                val shelf = social.fetchUserShelves(uid).filter { it.shelfType == "FINISHED" }.distinctBy { it.bookId }
                val books = catalog.getBooks().associateBy { it.id }
                finished = shelf.size
                categories = shelf.groupingBy { books[it.bookId]?.category?.takeIf { it.isNotBlank() } ?: "Diğer" }.eachCount()
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { error = "Kitaplık özeti yüklenemedi. Tekrar dene." }
        finally { loading = false }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text(if (premium) "Premium Analiz" else "İstatistikler", fontFamily = PlayfairDisplayFamily) },
            navigationIcon = { IconButton(onClick = { navigator.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") } })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item { Text("Senin sesli kitap yolculuğun", style = MaterialTheme.typography.headlineMedium, fontFamily = PlayfairDisplayFamily) }
            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            else if (error != null) item {
                Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { if (auth.currentUserUid == null) navigator.push(LoginScreen()) else retry++ }) {
                    Text(if (auth.currentUserUid == null) "Giriş yap" else "Tekrar dene")
                }
            } else {
                item {
                    OutlinedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("KİTAPLIK ÖZETİ", style = MaterialTheme.typography.labelLarge)
                            Text(finished.toString(), style = MaterialTheme.typography.displayMedium)
                            Text("Tamamlanan kitap", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (categories.isNotEmpty()) item {
                    Text("Türlere göre bitirilenler", style = MaterialTheme.typography.titleLarge)
                    categories.entries.sortedByDescending { it.value }.forEach { (name, count) ->
                        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(name); Text("$count kitap")
                        }
                        LinearProgressIndicator(progress = { count.toFloat() / finished.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    }
                }
                item {
                    Text("Dinleme süresi ve günlük aktivite özeti bu hesap için henüz kullanılamıyor.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    Button(onClick = { navigator.push(SearchScreen()) }, modifier = Modifier.fillMaxWidth()) { Text("Kitapları keşfet") }
                }
            }
        }
    }
}
