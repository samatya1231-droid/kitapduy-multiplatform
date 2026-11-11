package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.util.root
import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.model.FeedType
import com.semtinazizi.kitapduy.platform.BillingManager
import com.semtinazizi.kitapduy.ui.components.*
import com.semtinazizi.kitapduy.ui.theme.PlayfairDisplayFamily
import com.semtinazizi.kitapduy.ui.viewmodel.AuthViewModel
import com.semtinazizi.kitapduy.ui.viewmodel.ProfileScreenModel
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

class UserProfileScreen(val targetUserId: String? = null) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = koinScreenModel<ProfileScreenModel>()
        val auth = koinInject<AuthRepository>()
        val authModel = koinViewModel<AuthViewModel>()
        val billing = koinInject<BillingManager>()
        val scope = rememberCoroutineScope()
        val own = targetUserId == null || targetUserId == auth.currentUserUid
        val uid = targetUserId ?: auth.currentUserUid
        val profile by model.targetUserProfile.collectAsState()
        val storePremium by billing.isPremium.collectAsState()
        val premium = profile?.hasPremium() == true || (own && storePremium)
        val loading by model.isLoading.collectAsState()
        val error by model.error.collectAsState()
        val followers by model.followersCount.collectAsState()
        val following by model.followingCount.collectAsState()
        val follows by model.isFollowing.collectAsState()
        val followBusy by model.followBusy.collectAsState()
        val shelves by model.userShelves.collectAsState()
        val dna by model.readingDnaCategories.collectAsState()
        val authors by model.favoriteAuthors.collectAsState()
        val posts by model.userPosts.collectAsState()
        val quotePosts = remember(posts) {
            posts.filter { (it.type == FeedType.QUOTE || it.startPos != null) && it.content.isNotBlank() }
        }
        var tab by rememberSaveable(uid) { mutableIntStateOf(0) }
        var followDialog by remember { mutableStateOf<Boolean?>(null) }
        var zoomedImage by remember { mutableStateOf<String?>(null) }
        var uploading by remember { mutableStateOf(false) }
        var coverUpload by remember { mutableStateOf(false) }
        var feedback by remember { mutableStateOf<String?>(null) }
        val picker = rememberFilePickerLauncher(type = PickerType.Image, mode = PickerMode.Single) { file ->
            if (file != null) scope.launch {
                uploading = true
                feedback = null
                try {
                    val bytes = file.readBytes()
                    val extension = file.name.substringAfterLast('.', "jpg")
                    val done: (Boolean) -> Unit = { success ->
                        uploading = false
                        if (success) model.loadProfileData(targetUserId)
                        else feedback = "Fotoğraf kaydedilemedi. Tekrar dene."
                    }
                    if (coverUpload) authModel.updateCoverPicture(bytes, extension, done)
                    else authModel.updateProfilePicture(bytes, extension, done)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (failure: Exception) { uploading = false; feedback = "Fotoğraf okunamadı. Tekrar dene." }
            }
        }
        LaunchedEffect(uid) { model.loadProfileData(targetUserId) }
        zoomedImage?.let { image ->
            Dialog(onDismissRequest = { zoomedImage = null }) {
                Surface(shape = MaterialTheme.shapes.large) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        SmartImage(imageUrl = image, modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp), contentScale = ContentScale.Fit)
                        TextButton(onClick = { zoomedImage = null }) { Text("Kapat") }
                    }
                }
            }
        }
        followDialog?.let { isFollowers ->
            val users by model.followListUsers.collectAsState()
            val listLoading by model.followListLoading.collectAsState()
            val listError by model.followListError.collectAsState()
            AlertDialog(onDismissRequest = { followDialog = null },
                title = { Text(if (isFollowers) "Takipçiler" else "Takip edilenler") },
                text = {
                    when {
                        listLoading -> CircularProgressIndicator()
                        listError != null -> Column {
                            Text(listError.orEmpty())
                            TextButton(onClick = { uid?.let { model.fetchFollowList(it, isFollowers) } }) { Text("Tekrar dene") }
                        }
                        users.isEmpty() -> Text("Henüz kimse yok.")
                        else -> LazyColumn(Modifier.heightIn(max = 360.dp)) {
                            items(users, key = { it.getValue("uid") }) { user ->
                                ListItem(headlineContent = { Text(user.getValue("name")) },
                                    leadingContent = { SmartImage(imageUrl = user["photo"], isAvatar = true,
                                        fallbackText = user["name"].orEmpty(), modifier = Modifier.size(40.dp)) },
                                    modifier = Modifier.clickable {
                                        followDialog = null
                                        navigator.push(UserProfileScreen(user.getValue("uid")))
                                    })
                            }
                        }
                    }
                }, confirmButton = { TextButton(onClick = { followDialog = null }) { Text("Kapat") } })
        }
        Scaffold(topBar = {
            TopAppBar(title = { Text(if (own) "Okur köşem" else "Okur profili", fontFamily = PlayfairDisplayFamily) },
                navigationIcon = { if (!own) IconButton(onClick = { navigator.pop() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                } },
                actions = {
                    if (own) {
                        IconButton(onClick = { navigator.push(PremiumWrappedScreen()) }) {
                            Icon(Icons.Default.AutoAwesome, "Yıllık Özet", tint = androidx.compose.ui.graphics.Color(0xFFFFD700))
                        }
                        IconButton(onClick = { navigator.push(SettingsScreen(isPremium = premium)) }) {
                            Icon(Icons.Default.Settings, "Ayarlar")
                        }
                    }
                }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        }) { padding ->
            when {
                loading && profile == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                profile == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    ProfileEmptyCard("Okur köşen", error ?: "Profil yüklenemedi.",
                        if (uid == null) "Giriş yap" else "Tekrar dene", {
                            if (uid == null) navigator.root.push(LoginScreen()) else model.loadProfileData(targetUserId)
                        })
                }
                else -> LazyColumn(Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (loading) item("loading_indicator") {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp))
                    }
                    if (own || !profile?.coverPhotoUrl.isNullOrBlank()) item("cover") {
                        ProfileCoverCard(profile?.coverPhotoUrl, own, uploading, feedback,
                            onEdit = { coverUpload = true; picker.launch() },
                            onView = { zoomedImage = profile?.coverPhotoUrl })
                    }
                    item("identity") {
                        ProfileIdentityCard(profile?.name.orEmpty(), profile?.photoUrl, own,
                            if (premium) "Premium okur" else "KitapDuy okuru", followers, following, uploading,
                            onPhotoClick = { profile?.photoUrl?.takeIf { it.isNotBlank() }?.let { zoomedImage = it } },
                            onEditPhoto = { coverUpload = false; picker.launch() },
                            onFollowers = { followDialog = true; uid?.let { model.fetchFollowList(it, true) } },
                            onFollowing = { followDialog = false; uid?.let { model.fetchFollowList(it, false) } })
                        val badges = profile?.badges.orEmpty().filterNot { it.equals("premium", true) }.distinct()
                        if (badges.isNotEmpty()) LazyRow(contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(badges) { badge ->
                                val label = when (badge.lowercase()) {
                                    "admin" -> "👑 Yönetici"
                                    "bookworm" -> "📚 Kitap Kurdu"
                                    else -> "🎖️ Okur"
                                }
                                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant) {
                                    Text(label, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        if (!profile?.bio.isNullOrBlank()) Text(profile?.bio.orEmpty(), Modifier.padding(horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.padding(horizontal = 20.dp)) {
                            if (own) OutlinedButton(onClick = { navigator.push(ProfileEditScreen()) }) { Text("Profili düzenle") }
                            else OutlinedButton(enabled = !followBusy && auth.currentUserUid != null,
                                onClick = { uid?.let(model::toggleFollow) }) { Text(if (follows) "Takipten çık" else "Takip et") }
                        }
                        error?.let { Text(it, Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.error) }
                    }
                    item("bento_stats") {
                        val finishedCount = shelves.count { it.shelfType == "FINISHED" }.toString()
                        val streak = (profile?.streak ?: 0).toString()
                        ProfileBentoStats(
                            followersCount = followers.toString(),
                            followingCount = following.toString(),
                            booksFinished = finishedCount,
                            streakDays = streak,
                            onFollowersClick = { followDialog = true; uid?.let { model.fetchFollowList(it, true) } },
                            onFollowingClick = { followDialog = false; uid?.let { model.fetchFollowList(it, false) } },
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                    item("tabs") { ProfileContentTabs(own, tab) { tab = it } }
                    if (tab == 0) {
                        if (own) item("pride") {
                            TextButton(onClick = { navigator.push(PrideShelfScreen()) }, modifier = Modifier.padding(horizontal = 20.dp)) {
                                Text("Gurur rafım · ${shelves.count { it.shelfType == "FINISHED" }} kitap tamamlandı")
                            }
                        }
                        if (shelves.isEmpty()) item("empty") {
                            ProfileEmptyCard(if (own) "Bir sonraki hikâyeni seç" else "Kitaplığı henüz boş",
                                if (own) "Dinlemek istediğin kitapları burada biriktir." else "Bu okurun kitaplığı zamanla şekillenecek.",
                                if (own) "Kitapları keşfet" else null, { navigator.push(SearchScreen()) })
                        }
                        listOf("READING" to "Şu an dinliyorum", "FINISHED" to "Dinlediklerim", "WISHLIST" to "Dinleyeceklerim").forEach { (type, title) ->
                            val shelf = shelves.filter { it.shelfType == type }
                            if (shelf.isNotEmpty()) item(type) {
                                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(title, style = MaterialTheme.typography.titleMedium, fontFamily = PlayfairDisplayFamily)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items(shelf, key = { it.bookId }) { entry ->
                                            Column(Modifier.width(100.dp)) {
                                                BookOnShelf(entry) { model.bookFor(entry)?.let { navigator.push(BookDetailScreen(it)) } }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (dna.isNotEmpty()) item("dna") { ReadingDNACard(dna, if (own) "Okuma DNA’n" else "Okuma DNA’sı", Modifier.padding(horizontal = 20.dp)) }
                        if (authors.isNotEmpty()) item("authors") {
                            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(if (own) "Favori yazarların" else "Favori yazarları", style = MaterialTheme.typography.titleMedium, fontFamily = PlayfairDisplayFamily)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(authors) { (name, image) ->
                                        Column(Modifier.width(80.dp).clickable { navigator.push(AuthorDetailScreen(name)) }, horizontalAlignment = Alignment.CenterHorizontally) {
                                            SmartImage(imageUrl = image, isAvatar = true, fallbackText = name, modifier = Modifier.size(64.dp))
                                            Text(name, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                                        }
                                    }
                                }
                            }
                        }
                        if (quotePosts.isNotEmpty()) item("favorite_quotes") {
                            FavoriteQuotesGallery(
                                quotes = quotePosts,
                                onQuoteClick = { quote ->
                                    if (quote.id.isNotBlank()) {
                                        navigator.push(PostDetailScreen(quote.id))
                                    }
                                }
                            )
                        }
                        if (own && !premium) item("premium") {
                            PremiumUpgradeCard(modifier = Modifier.padding(horizontal = 20.dp), isPremium = false,
                                onClick = { navigator.root.push(SubscriptionPaywallScreen()) })
                        }
                    } else {
                        if (own) item("compose") {
                            OutlinedButton(onClick = { navigator.push(SocialFeedScreen(openComposer = true)) }, modifier = Modifier.padding(horizontal = 20.dp)) { Text("Bir düşünce paylaş") }
                        }
                        if (posts.isEmpty()) item("empty_posts") { ProfileEmptyCard("Henüz bir paylaşım yok", "Bir kitap, bir cümle, bir düşünce…") }
                        items(posts, key = { it.id }) { post ->
                            Box(Modifier.padding(horizontal = 20.dp)) {
                                InteractiveFeedCard(item = post, onChanged = { model.loadProfileData(targetUserId) })
                            }
                        }
                    }
                }
            }
        }
    }
}
