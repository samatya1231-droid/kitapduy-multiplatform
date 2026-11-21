package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.model.Book
import com.semtinazizi.kitapduy.ui.components.SmartImage
import com.semtinazizi.kitapduy.ui.theme.InterFamily
import com.semtinazizi.kitapduy.ui.theme.PlayfairDisplayFamily
import com.semtinazizi.kitapduy.ui.theme.SleekChampagne
import com.semtinazizi.kitapduy.ui.viewmodel.AuthViewModel
import com.semtinazizi.kitapduy.ui.viewmodel.HomeViewModel
import org.koin.compose.viewmodel.koinViewModel

private data class AdvisorMoodChip(
    val id: String,
    val label: String,
    val prompt: String
)

enum class AdvisorSender {
    USER,
    ADVISOR
}

data class AdvisorMessage(
    val id: String,
    val sender: AdvisorSender,
    val content: String,
    val intent: String? = null,
    val answerKind: String? = null,
    val recommendedBooks: List<Book> = emptyList(),
    val followUps: List<Pair<String, String>> = emptyList(),
    val analyzedTitle: String? = null
)

class LiteraryAdvisorScreenModel : ScreenModel {
    val messages = mutableStateListOf<AdvisorMessage>()
    val savedMessageIds = mutableStateListOf<String>()
    var currentInput by mutableStateOf("")
    var showDiscovery by mutableStateOf(true)
    var responseDepth by mutableStateOf("auto")
    var spoilerOverride by mutableStateOf<Boolean?>(null)
    var readingMessage by mutableStateOf<AdvisorMessage?>(null)
    var showInformation by mutableStateOf(false)
    var initialPromptHandled by mutableStateOf(false)
    var messageSeq by mutableStateOf(0)
        private set

    fun toggleSaveMessage(messageId: String) {
        if (messageId.isBlank()) return
        if (messageId in savedMessageIds) savedMessageIds.remove(messageId) else savedMessageIds.add(messageId)
    }

    fun submitPrompt(rawPrompt: String, explicitIntent: String?, catalog: List<Book>): Boolean {
        val cleanPrompt = rawPrompt.trim()
        if (cleanPrompt.isEmpty()) return false
        val previousTitle = messages.lastOrNull { it.sender == AdvisorSender.ADVISOR }?.analyzedTitle
        showDiscovery = false
        messageSeq += 1
        messages.add(
            AdvisorMessage(
                id = "u_$messageSeq",
                sender = AdvisorSender.USER,
                content = cleanPrompt,
                intent = explicitIntent
            )
        )
        val reply = buildAdvisorReply(
            prompt = cleanPrompt,
            explicitIntent = explicitIntent,
            catalog = catalog,
            depth = responseDepth,
            spoilerOverride = spoilerOverride,
            previousTitle = previousTitle
        )
        messageSeq += 1
        messages.add(reply.copy(id = "a_$messageSeq"))
        return true
    }

    fun startNewChat() {
        messages.clear()
        showDiscovery = true
        readingMessage = null
    }
}

private object KmpLiteraryAdvisorPolicy {
    val CONVERSATION_STARTERS = mapOf(
        "analysis" to AdvisorMoodChip(
            id = "analysis",
            label = "📖 Bir eseri konuşalım",
            prompt = "Bir eseri birlikte yorumlamak istiyorum. Spoiler vermeden önce hangi eser olduğunu sor."
        ),
        "brainstorm" to AdvisorMoodChip(
            id = "brainstorm",
            label = "✍️ Fikrimi geliştirelim",
            prompt = "Bir edebî fikir üzerine beyin fırtınası yapmak istiyorum. Önce konuyu sor, sonra farklı olasılıkları birlikte geliştirelim."
        ),
        "debate" to AdvisorMoodChip(
            id = "debate",
            label = "⚖️ Bir fikri tartışalım",
            prompt = "Edebî veya felsefi bir konuyu karşılıklı tartışmak istiyorum. Önce konuyu ve benim görüşümü sor; sonra güçlü karşı argümanlarla düşüncemi sınayalım."
        )
    )

    val DEFAULT_MOOD_CHIPS = listOf(
        AdvisorMoodChip(
            id = "melancholic",
            label = "🌧️ Melankolik & Yağmurlu",
            prompt = "Hüzünlü, yağmurlu bir günde içimi ısıtacak ve hüznüme tercüman olacak derin ve melankolik bir sesli kitap önerir misin?"
        ),
        AdvisorMoodChip(
            id = "philosophical",
            label = "☕ Dingin & Felsefi",
            prompt = "Kahvemi yudumlarken zihnimi dinlendirecek, dingin ve derin felsefi düşüncelere daldıracak bir sesli kitap tavsiyesi arıyorum."
        ),
        AdvisorMoodChip(
            id = "gripping",
            label = "⚡ Sürükleyici & Polisiye",
            prompt = "Elimden bırakamayacağım, heyecandan kalbimi hızlandıracak sürükleyici bir polisiye veya gizem kitabı dinlemek istiyorum."
        ),
        AdvisorMoodChip(
            id = "hopeful",
            label = "🌱 Umut Verici & İlham Dolu",
            prompt = "Ruhumu tazeleyecek, umut ve yaşama sevinci aşılayacak ilham dolu bir kitap tavsiye eder misin?"
        ),
        AdvisorMoodChip(
            id = "classic",
            label = "⏳ Klasik Zaman Yolculuğu",
            prompt = "Beni geçmiş yüzyılların edebi zarafetine götürecek, zamana meydan okumuş ölümsüz bir klasik öner."
        )
    )

    fun fallbackFollowUps(intent: String?): List<Pair<String, String>> = when (intent) {
        "brainstorm" -> listOf(
            "Çatışmayı güçlendirelim" to "Bu fikrin merkezindeki çatışmayı üç farklı yönde geliştirelim.",
            "Karakteri derinleştirelim" to "Bu fikre uygun bir karakterin arzularını ve iç çelişkisini birlikte geliştirelim.",
            "Bir açılış deneyelim" to "Bu fikir için farklı tonlarda iki kısa açılış sahnesi öner."
        )
        "debate" -> listOf(
            "Karşı görüşü savun" to "Bu düşünceye karşı en güçlü itirazı kur; sonra birlikte değerlendirelim.",
            "Bir örnekle sınayalım" to "Bu düşünceyi somut bir edebî örnek üzerinden sınayalım.",
            "Soruyu derinleştir" to "Bu tartışmanın altında yatan varsayımı sorgulamak için bana bir soru sor."
        )
        "analysis" -> listOf(
            "Karakterin izini sürelim" to "Bu eserdeki karakterin seçimlerini ve iç çatışmasını inceleyelim.",
            "Temayı derinleştirelim" to "Bu eserdeki ana temanın anlatım biçimiyle ilişkisini derinleştir.",
            "Başka bir eser öner" to "Bu temaya yakın sesli kütüphanemizden bir kitap önerir misin?"
        )
        else -> listOf(
            "Daha kısa bir eser" to "Benzer duyguyu taşıyan daha kısa bir sesli kitap önerebilir misin?",
            "Eserleri karşılaştıralım" to "Bu eseri tema, atmosfer ve anlatım dili açısından başka bir klasikle karşılaştır.",
            "Nereden başlamalıyım?" to "Sesli kütüphaneden bugün başlamak için bana tek bir eser seç."
        )
    }
}

class LiteraryAdvisorScreen(
    private val initialPrompt: String? = null
) : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val advisorModel = koinScreenModel<LiteraryAdvisorScreenModel>()
        val homeViewModel = koinViewModel<HomeViewModel>()
        val authViewModel = koinViewModel<AuthViewModel>()
        val books by homeViewModel.books.collectAsState()
        val userProfile by authViewModel.userProfile.collectAsState()
        val isPremium = userProfile?.hasPremium() == true

        val keyboardController = LocalSoftwareKeyboardController.current
        val listState = rememberLazyListState()

        val messages = advisorModel.messages

        val notebookColor = lerp(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface,
            0.35f
        )

        val submitPrompt: (String, String?) -> Unit = { rawPrompt, explicitIntent ->
            if (rawPrompt.trim().isNotEmpty()) {
                keyboardController?.hide()
                advisorModel.submitPrompt(rawPrompt, explicitIntent, books)
            }
        }

        LaunchedEffect(initialPrompt) {
            val seedPrompt = initialPrompt?.trim().orEmpty()
            if (!advisorModel.initialPromptHandled && seedPrompt.isNotEmpty() && messages.isEmpty()) {
                advisorModel.initialPromptHandled = true
                submitPrompt(seedPrompt, "recommendation")
            }
        }

        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.lastIndex)
            }
        }

        Scaffold(
            containerColor = notebookColor,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = { navigator.pop() },
                            modifier = Modifier.testTag("advisor-back")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = "Edebi Danışman",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = PlayfairDisplayFamily,
                                    fontSize = 20.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val userTurns = messages.count { it.sender == AdvisorSender.USER }
                            Text(
                                text = if (isPremium) "Bilge Sahaf • 👑 Premium Sınırsız ($userTurns/50)" else "Bilge Sahaf • Günlük Rehber ($userTurns/3)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        if (messages.isNotEmpty()) {
                            IconButton(
                                onClick = { advisorModel.startNewChat() },
                                modifier = Modifier.testTag("advisor-new-chat")
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Yeni sohbet")
                            }
                        }
                        IconButton(
                            onClick = { advisorModel.showInformation = true },
                            modifier = Modifier.testTag("advisor-info-button")
                        ) {
                            Icon(Icons.Filled.Info, contentDescription = "Bilgi")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = notebookColor)
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .background(notebookColor)
                        .navigationBarsPadding()
                ) {
                    AdvisorResponsePreferences(
                        depth = advisorModel.responseDepth,
                        spoilerOverride = advisorModel.spoilerOverride,
                        onDepth = { advisorModel.responseDepth = it },
                        onSpoiler = { advisorModel.spoilerOverride = it }
                    )
                    AdvisorInputBar(
                        text = advisorModel.currentInput,
                        onTextChange = { advisorModel.currentInput = it.take(600) },
                        onSend = {
                            val toSend = advisorModel.currentInput
                            advisorModel.currentInput = ""
                            submitPrompt(toSend, null)
                        }
                    )
                }
            }
        ) { innerPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("advisor-list"),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (messages.isEmpty()) {
                    item(key = "welcome") {
                        AdvisorWelcomeSection(
                            isPremium = isPremium,
                            onStart = { chip ->
                                submitPrompt(chip.prompt, chip.id)
                            },
                            onDiscovery = {
                                advisorModel.showDiscovery = !advisorModel.showDiscovery
                            },
                            onReadingRoute = {
                                submitPrompt(
                                    "Kütüphanemizdeki eserlerden başlangıç, derinleşme ve başyapıt adımlarından oluşan 3 kitaplık kişisel bir okuma rotası hazırlar mısın?",
                                    "reading_route"
                                )
                            },
                            onInformation = { advisorModel.showInformation = true }
                        )
                    }
                    if (advisorModel.showDiscovery) {
                        item(key = "mood-discovery") {
                            MoodQuickChipsSection(
                                onChipClick = { chip ->
                                    submitPrompt(chip.prompt, "recommendation")
                                }
                            )
                        }
                    }
                } else {
                    items(messages, key = { it.id }) { message ->
                        if (message.sender == AdvisorSender.USER) {
                            UserMessageBubble(message)
                        } else {
                            AdvisorMessageBubble(
                                message = message,
                                isSaved = message.id in advisorModel.savedMessageIds,
                                onToggleSave = { advisorModel.toggleSaveMessage(message.id) },
                                onBookClick = { book -> navigator.push(BookDetailScreen(book)) },
                                onRead = { advisorModel.readingMessage = message },
                                onFollowUp = { followUpPrompt ->
                                    submitPrompt(followUpPrompt, message.intent)
                                }
                            )
                        }
                    }
                }
            }
        }

        advisorModel.readingMessage?.let { activeMsg ->
            AlertDialog(
                onDismissRequest = { advisorModel.readingMessage = null },
                title = {
                    Text(
                        text = "Okuma Görünümü",
                        fontFamily = PlayfairDisplayFamily,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            text = parseBasicMarkdown(activeMsg.content),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = InterFamily,
                                fontSize = 16.sp,
                                lineHeight = 25.sp
                            )
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { advisorModel.readingMessage = null }) {
                        Text("Kapat")
                    }
                }
            )
        }

        if (advisorModel.showInformation) {
            AlertDialog(
                onDismissRequest = { advisorModel.showInformation = false },
                title = {
                    Text(
                        text = "Edebi Danışman Hakkında",
                        fontFamily = PlayfairDisplayFamily,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Edebi Danışman, KitapDuy sesli kitap arşivindeki eserleri ruh halinize, sevdiğiniz yazarlara ve okuma derinliğinize göre eşleştirir; karakter, tema ve fikir tartışmalarında size eşlik eder.",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                    )
                },
                confirmButton = {
                    TextButton(onClick = { advisorModel.showInformation = false }) {
                        Text("Anladım")
                    }
                }
            )
        }
    }
}

@Composable
private fun AdvisorWelcomeSection(
    isPremium: Boolean,
    onStart: (AdvisorMoodChip) -> Unit,
    onDiscovery: () -> Unit,
    onReadingRoute: () -> Unit,
    onInformation: () -> Unit
) {
    val starters = KmpLiteraryAdvisorPolicy.CONVERSATION_STARTERS
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Icon(
            imageVector = Icons.Filled.LocalLibrary,
            contentDescription = null,
            tint = SleekChampagne,
            modifier = Modifier.size(30.dp)
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Bir fikrin peşinden\nbirlikte gidelim.",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = PlayfairDisplayFamily,
                fontWeight = FontWeight.Normal
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Eserleri konuşalım, düşünceleri tartışalım, hikâyene yeni bir yön bulalım.",
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AdvisorIntentCard(
                title = "Bir eseri konuşalım",
                description = "Tema, karakter ve anlatım",
                icon = Icons.Filled.MenuBook,
                modifier = Modifier.weight(1f).testTag("advisor-intent-analysis"),
                onClick = { starters["analysis"]?.let(onStart) }
            )
            AdvisorIntentCard(
                title = "Fikrimi geliştirelim",
                description = "Öykü, kurgu ve yazma",
                icon = Icons.Filled.Edit,
                modifier = Modifier.weight(1f).testTag("advisor-intent-brainstorm"),
                onClick = { starters["brainstorm"]?.let(onStart) }
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AdvisorIntentCard(
                title = "Bir fikri tartışalım",
                description = "Felsefe ve farklı bakışlar",
                icon = Icons.Filled.QuestionAnswer,
                modifier = Modifier.weight(1f).testTag("advisor-intent-debate"),
                onClick = { starters["debate"]?.let(onStart) }
            )
            AdvisorIntentCard(
                title = "Kitap keşfedelim",
                description = "İlgi alanına göre öneriler",
                icon = Icons.Filled.Explore,
                modifier = Modifier.weight(1f).testTag("advisor-intent-discover"),
                onClick = onDiscovery
            )
        }

        Spacer(Modifier.height(16.dp))
        Surface(
            onClick = onReadingRoute,
            shape = RoundedCornerShape(16.dp),
            color = SleekChampagne.copy(alpha = 0.12f),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("advisor-reading-route")
                .semantics(mergeDescendants = true) {}
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = SleekChampagne,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Kişisel okuma rotan", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "İlgine ve zamanına uygun bir plan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!isPremium) {
                    Text(
                        text = "Hazırla",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        TextButton(
            onClick = onInformation,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
            modifier = Modifier.heightIn(min = 44.dp).testTag("advisor-archive-about")
        ) {
            Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(7.dp))
            Text("Edebi danışman ve arşiv hakkında", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun AdvisorIntentCard(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 118.dp)
            .semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = SleekChampagne, modifier = Modifier.size(21.dp))
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MoodQuickChipsSection(
    onChipClick: (AdvisorMoodChip) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ruh Halime Göre",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = PlayfairDisplayFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "• Hızlı Tavsiye",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontStyle = FontStyle.Italic,
                    fontSize = 12.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(KmpLiteraryAdvisorPolicy.DEFAULT_MOOD_CHIPS, key = { it.id }) { chip ->
                Surface(
                    onClick = { onChipClick(chip) },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .testTag("advisor-mood-${chip.id}")
                        .semantics(mergeDescendants = true) {}
                ) {
                    Text(
                        text = chip.label,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun UserMessageBubble(message: AdvisorMessage) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp),
            color = SleekChampagne,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = message.content,
                color = Color(0xFF1E1710),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFamily,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun AdvisorMessageBubble(
    message: AdvisorMessage,
    isSaved: Boolean = false,
    onToggleSave: () -> Unit = {},
    onBookClick: (Book) -> Unit,
    onRead: () -> Unit,
    onFollowUp: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val followUps = remember(message.followUps, message.intent) {
        message.followUps.ifEmpty { KmpLiteraryAdvisorPolicy.fallbackFollowUps(message.intent) }
    }
    Column(
        modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = "Edebi Danışman",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            message.answerKind?.let { label ->
                Text(
                    text = "· $label",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background, 0.2f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = parseBasicMarkdown(message.content),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFamily,
                    lineHeight = 25.sp,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(
                onClick = onRead,
                modifier = Modifier.heightIn(min = 44.dp).testTag("advisor-reading-view")
            ) {
                Icon(Icons.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("Okuma görünümü", style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onToggleSave) {
                Icon(
                    imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = if (isSaved) "Kaydedildi" else "Yanıtı kaydet",
                    modifier = Modifier.size(19.dp),
                    tint = if (isSaved) SleekChampagne else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { clipboard.setText(AnnotatedString(message.content)) }) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Yanıtı kopyala",
                    modifier = Modifier.size(19.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (message.recommendedBooks.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(end = 12.dp)
            ) {
                items(message.recommendedBooks, key = { it.id }) { book ->
                    AudiobookRecommendationCard(
                        book = book,
                        onClick = { onBookClick(book) }
                    )
                }
            }
        }

        if (followUps.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(followUps, key = { _, item -> item.first }) { index, (label, prompt) ->
                    SuggestionChip(
                        onClick = { onFollowUp(prompt) },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.heightIn(min = 44.dp).testTag("advisor-followup-$index")
                    )
                }
            }
        }
    }
}

@Composable
private fun AudiobookRecommendationCard(
    book: Book,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, SleekChampagne.copy(alpha = 0.7f)),
        shadowElevation = 2.dp,
        modifier = Modifier
            .widthIn(min = 240.dp, max = 310.dp)
            .testTag("advisor-rec-${book.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 54.dp, height = 76.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (book.coverUrl.isNotBlank()) {
                    SmartImage(
                        imageUrl = book.coverUrl,
                        contentDescription = book.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Headphones,
                        contentDescription = null,
                        tint = SleekChampagne,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = PlayfairDisplayFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (book.author.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontStyle = FontStyle.Italic,
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SleekChampagne,
                        contentColor = Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .testTag("advisor-rec-listen-${book.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Headphones,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Bu Kitabı Dinle",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun AdvisorResponsePreferences(
    depth: String,
    spoilerOverride: Boolean?,
    onDepth: (String) -> Unit,
    onSpoiler: (Boolean?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(listOf("brief" to "Kısa", "auto" to "Doğal", "deep" to "Ayrıntılı"), key = { it.first }) { (value, label) ->
            FilterChip(
                selected = depth == value,
                onClick = { onDepth(value) },
                label = { Text(label) },
                modifier = Modifier.testTag("advisor-depth-$value")
            )
        }
        item(key = "spoilers") {
            FilterChip(
                selected = spoilerOverride != null,
                onClick = {
                    onSpoiler(
                        when (spoilerOverride) {
                            null -> false
                            false -> true
                            true -> null
                        }
                    )
                },
                label = {
                    Text(
                        when (spoilerOverride) {
                            null -> "Spoiler: otomatik"
                            false -> "Spoilersız"
                            true -> "Spoiler izni"
                        }
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (spoilerOverride == true) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.testTag("advisor-spoiler")
            )
        }
    }
}

@Composable
private fun AdvisorInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("advisor-input"),
                placeholder = {
                    Text(
                        text = "Edebiyat, felsefe, kitap önerisi… Aklında ne var?",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                shape = RoundedCornerShape(24.dp),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (text.isNotBlank()) onSend() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SleekChampagne,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                )
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = onSend,
                enabled = text.isNotBlank(),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = SleekChampagne,
                    contentColor = Color.Black,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .size(46.dp)
                    .testTag("advisor-send")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gönder")
            }
        }
    }
}

internal fun parseBasicMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    val cleanText = text.replace(
        Regex("""\[(?:BOOK_REC|BOOK|KITAP|KITAP_REC|SOURCE):[^\]]*]""", RegexOption.IGNORE_CASE),
        ""
    ).trim()
    val regex = Regex("""\*\*(.+?)\*\*|(?<!\*)\*(?!\*|\s)([^*\n]+?)(?<!\s)\*(?!\*)""")
    var lastIndex = 0
    for (match in regex.findAll(cleanText)) {
        if (match.range.first > lastIndex) {
            append(cleanText.substring(lastIndex, match.range.first))
        }
        val boldGroup = match.groups[1]
        if (boldGroup != null) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(boldGroup.value)
            }
        } else {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                append(match.groupValues[2])
            }
        }
        lastIndex = match.range.last + 1
    }
    if (lastIndex < cleanText.length) {
        append(cleanText.substring(lastIndex))
    }
}

private fun normalizeAdvisorText(input: String): String =
    KmpLiteraryAdvisorKnowledge.normalize(input)

private const val ICIMIZDEKI_SEYTAN = "İçimizdeki Şeytan"
private val analysisRequest = Regex("\\b(?:anlat\\w*|ozet\\w*|incele\\w*|yorumla\\w*|analiz\\w*|konusu|konus\\w*|hakkinda|kimdir|nedir|degerlendir\\w*)\\b")
private val recommendationRequest = Regex("\\b(?:oner\\w*|tavsiye|ne dinlesem|kitap sec)\\b")
private val previousWorkReference = Regex("\\b(?:bu|o) (?:esere?|eserin|eserdeki|romana?|romanin|kitaba?|kitabin)\\b")

private fun mentionsTitle(normalizedPrompt: String, title: String): Boolean {
    val normalizedTitle = normalizeAdvisorText(title)
    if (normalizedTitle.length < 3) return false
    return Regex("\\b${Regex.escape(normalizedTitle)}(?:ya|ye|yi|yu|dan|den|tan|ten|da|de|ta|te|nin|nun|in|un|i|u|a|e)?\\b")
        .containsMatchIn(normalizedPrompt)
}

private fun buildWorkReply(
    normalizedPrompt: String,
    catalog: List<Book>,
    previousTitle: String?,
    depth: String,
    spoilerNote: String
): AdvisorMessage {
    val catalogBooks = catalog.filter { !it.category.equals("Podcast", ignoreCase = true) }
    val titles = (catalogBooks.map { it.title } + KmpLiteraryAdvisorKnowledge.allKnownWorkTitles + ICIMIZDEKI_SEYTAN)
        .distinctBy(::normalizeAdvisorText)
        .filter { mentionsTitle(normalizedPrompt, it) }
        .sortedByDescending { it.length }
    val matchedMp = KmpLiteraryAdvisorKnowledge.findMasterpieceAnalysis(normalizedPrompt, titles.firstOrNull(), depth)
    val title = titles.firstOrNull()
        ?: matchedMp?.title
        ?: previousTitle?.takeIf { previousWorkReference.containsMatchIn(normalizedPrompt) }
    val selectedBook = catalogBooks.firstOrNull { normalizeAdvisorText(it.title) == title?.let(::normalizeAdvisorText) }
    val extraMp = KmpLiteraryAdvisorKnowledge.findMasterpieceAnalysis(normalizedPrompt, selectedBook?.title ?: title, depth)

    val recs = if (selectedBook != null) {
        listOf(selectedBook)
    } else if (extraMp != null && extraMp.suggestedTags.isNotEmpty()) {
        findCatalogMatches(extraMp.suggestedTags.joinToString(" "), catalog, limit = 2, allowFallback = false)
    } else {
        emptyList()
    }

    val synopsis = when {
        title == null -> "İncelemek istediğin eseri belirleyemedim. Tam adını ve biliyorsan yazarını yazar mısın?"
        selectedBook != null -> {
            val heading = "**${selectedBook.title}** — ${selectedBook.author}"
            val description = selectedBook.description.trim()
            val catalogBlock = if (description.isEmpty()) heading else "$heading\n\nKatalog tanıtımı\n\n$description"
            if (extraMp != null) {
                "$catalogBlock\n\n${KmpLiteraryAdvisorKnowledge.formatMasterpieceReply(extraMp, depth)}"
            } else {
                catalogBlock
            }
        }
        extraMp != null -> KmpLiteraryAdvisorKnowledge.formatMasterpieceReply(extraMp, depth)
        else -> "**$title** için ayrıntılı bir tanıtım bulamadım. İncelemek istediğin bölümden kısa bir parça paylaşabilirsin."
    }
    return AdvisorMessage(
        id = "",
        sender = AdvisorSender.ADVISOR,
        content = synopsis + spoilerNote,
        intent = "analysis",
        answerKind = "Eser hakkında",
        recommendedBooks = recs,
        analyzedTitle = title
    )
}

private fun findCatalogMatches(
    query: String,
    catalog: List<Book>,
    limit: Int = 2,
    allowFallback: Boolean = true
): List<Book> {
    val audiobooks = catalog.filter {
        !it.category.trim().equals("Podcast", ignoreCase = true) && it.id != "local_ci_book"
    }.ifEmpty { catalog }

    val q = normalizeAdvisorText(query)
    val tokens = q.split(' ').filter { it.length >= 3 }
    if (!Regex("\\bgibi\\b").containsMatchIn(q)) {
        val exact = audiobooks.filter { mentionsTitle(q, it.title) }
            .sortedByDescending { it.title.length }
        if (exact.isNotEmpty()) return exact.take(limit)
    }

    fun matchBook(predicate: (Book, String) -> Boolean): List<Book> =
        audiobooks.filter { book ->
            val searchable = normalizeAdvisorText(
                "${book.title} ${book.author} ${book.category} ${book.tags.joinToString(" ")} ${book.description}"
            )
            predicate(book, searchable)
        }.sortedByDescending { it.viewCount }.take(limit)

    // Exact literary matches first
    val targeted = when {
        q.contains("sherlock") || q.contains("kizil dosya") || q.contains("arthur conan") ->
            matchBook { _, s -> s.contains("sherlock") || s.contains("kizil dosya") }
        q.contains("polisiye") || q.contains("gizem") || q.contains("cinayet") || q.contains("dedektif") || q.contains("surukleyici") ->
            matchBook { _, s -> s.contains("polisiye") || s.contains("sherlock") || s.contains("arthur conan") || s.contains("arsen lupen") || s.contains("arsen") }
        q.contains("melankolik") || q.contains("yagmur") || q.contains("huzun") || q.contains("sabahattin ali") || q.contains("kurk mantolu") ->
            matchBook { _, s -> s.contains("kurk mantolu") || s.contains("sabahattin ali") || s.contains("kuyucakli") || s.contains("icimizdeki seytan") || s.contains("beyaz geceler") }
        q.contains("felsefe") || q.contains("dingin") || q.contains("schopenhauer") || q.contains("marcus") || q.contains("epiktetos") ->
            matchBook { _, s -> s.contains("felsefe") || s.contains("kendime dusunceler") || s.contains("schopenhauer") || s.contains("sokrates") }
        q.contains("umut") || q.contains("ilham") || q.contains("kucuk prens") || q.contains("marti") ->
            matchBook { _, s -> s.contains("kucuk prens") || s.contains("marti") || s.contains("kisisel gelisim") }
        q.contains("dostoyevski") || q.contains("suc ve ceza") || q.contains("budala") || q.contains("karamazov") ->
            matchBook { _, s -> s.contains("dostoyevski") || s.contains("suc ve ceza") || s.contains("budala") }
        q.contains("orwell") || q.contains("hayvan ciftligi") || q.contains("1984") ->
            matchBook { _, s -> s.contains("orwell") || s.contains("hayvan ciftligi") || s.contains("1984") }
        q.contains("klasik") || q.contains("zaman yolculugu") ->
            matchBook { _, s -> s.contains("klasik") || s.contains("dostoyevski") || s.contains("tolstoy") }
        q.contains("turk") ->
            matchBook { _, s -> s.contains("turk edebiyati") || s.contains("sabahattin ali") || s.contains("omer seyfettin") || s.contains("refik halid") }
        else -> emptyList()
    }
    if (targeted.isNotEmpty()) return targeted

    if (tokens.isNotEmpty()) {
        val scored = audiobooks.mapNotNull { book ->
            val titleNorm = normalizeAdvisorText(book.title)
            val authorNorm = normalizeAdvisorText(book.author)
            val restNorm = normalizeAdvisorText("${book.category} ${book.tags.joinToString(" ")} ${book.description}")
            var score = 0
            for (t in tokens) {
                if (titleNorm.contains(t)) score += 6
                if (authorNorm.contains(t)) score += 5
                if (restNorm.contains(t)) score += 2
            }
            if (score > 0) book to score else null
        }.sortedWith(compareByDescending<Pair<Book, Int>> { it.second }.thenByDescending { it.first.viewCount })
            .map { it.first }
            .take(limit)
        if (scored.isNotEmpty()) return scored
    }

    if (!allowFallback) return emptyList()
    return audiobooks.sortedByDescending { it.viewCount }.take(limit)
}

private fun buildAdvisorReply(
    prompt: String,
    explicitIntent: String?,
    catalog: List<Book>,
    depth: String,
    spoilerOverride: Boolean?,
    previousTitle: String?
): AdvisorMessage {
    val q = normalizeAdvisorText(prompt)

    val spoilerNote = when (spoilerOverride) {
        false -> "\n\n*(Spoilersız mod: Olay örgüsünün sonunu ve sürpriz düğümleri özellikle saklı tuttum.)*"
        true -> "\n\n*(Spoiler izni açık: İstersen eserin finalini ve kırılma anlarını doğrudan konuşabiliriz.)*"
        null -> ""
    }

    if (explicitIntent == "analysis" && q.contains("hangi eser")) {
        return AdvisorMessage(
            id = "",
            sender = AdvisorSender.ADVISOR,
            content = "Memnuniyetle aziz dostum. Birlikte incelemek istediğin eserin adını yazar mısın? İstersen spoilersız kalarak karakterlerin iç dünyasını, ana çatışmayı veya yazarın anlatım kurgusunu adım adım çözümleyebiliriz.$spoilerNote",
            intent = "analysis",
            answerKind = "Edebî yorum",
            followUps = listOf(
                "Suç ve Ceza'yı konuşalım" to "Suç ve Ceza romanını karakter ve tema açısından spoilersız değerlendirelim.",
                "Kürk Mantolu Madonna" to "Kürk Mantolu Madonna'daki anlatıcı ve Maria Puder karakterini inceleyelim."
            )
        )
    }

    if (explicitIntent == "brainstorm") {
        return AdvisorMessage(
            id = "",
            sender = AdvisorSender.ADVISOR,
            content = "Bir hikâye tohumunu birlikte filizlendirmek sahaf köşemin en keyifli sohbetidir. Aklındaki karakteri, mekânı ya da tek cümlelik fikri paylaşır mısın? Önce temel çatışmayı netleştirelim, sonra sahne ve gerilim olasılıklarını çoğaltalım.",
            intent = "brainstorm",
            answerKind = "Fikir geliştirme"
        )
    }

    if (explicitIntent == "debate") {
        return AdvisorMessage(
            id = "",
            sender = AdvisorSender.ADVISOR,
            content = "Harika bir fikir jimnastiği yapalım dostum. Masaya hangi soruyu koymak istersin ve senin ilk tezin nedir? Sen görüşünü söylediğinde ben karşıt bir pencereden en güçlü argümanı getirip düşünceni beraber sınayalım.",
            intent = "debate",
            answerKind = "Edebî tartışma"
        )
    }

    if (explicitIntent == "reading_route" || q.contains("okuma rota")) {
        val routeBooks = buildList {
            findCatalogMatches("kucuk prens kurk mantolu", catalog, limit = 1).firstOrNull()?.let(::add)
            findCatalogMatches("sherlock holmes polisiye", catalog, limit = 1).firstOrNull()?.let { if (none { b -> b.id == it.id }) add(it) }
            findCatalogMatches("suc ve ceza dostoyevski", catalog, limit = 1).firstOrNull()?.let { if (none { b -> b.id == it.id }) add(it) }
            if (size < 3) {
                for (b in findCatalogMatches("", catalog, limit = 5)) {
                    if (size >= 3) break
                    if (none { it.id == b.id }) add(b)
                }
            }
        }
        val stepsText = if (routeBooks.size >= 3) {
            """
            1. **Isınma Durağı:** *${routeBooks[0].title}* (${routeBooks[0].author}) — Dilin ritmine ve sessiz düşünce akışına kolayca girmen için.
            2. **Tempo & Merak:** *${routeBooks[1].title}* (${routeBooks[1].author}) — Zihnini canlı tutacak keskin bir gözlem ve olay örgüsü için.
            3. **Derin Başyapıt:** *${routeBooks[2].title}* (${routeBooks[2].author}) — İnsan ruhunun katmanlarını ağır ağır sindirmek için.
            """.trimIndent()
        } else {
            "Sesli kitap arşivimizden temposu yavaş yavaş derinleşen seçkin duraklar belirledim:"
        }
        return AdvisorMessage(
            id = "",
            sender = AdvisorSender.ADVISOR,
            content = "Aziz dostum, zihnini yormadan merakını büyütecek **3 duraklı kişisel okuma rotanı** sahaf raflarımızdan senin için çıkardım:\n\n$stepsText$spoilerNote",
            intent = "reading_route",
            answerKind = "Okuma rotası",
            recommendedBooks = routeBooks
        )
    }

    val mentionsKnownWork = catalog.any { mentionsTitle(q, it.title) } ||
        KmpLiteraryAdvisorKnowledge.allKnownWorkTitles.any { mentionsTitle(q, it) } ||
        KmpLiteraryAdvisorKnowledge.findMasterpieceAnalysis(q, null, depth) != null
    val isPureAnalysisAsk = (explicitIntent == "analysis" || analysisRequest.containsMatchIn(q)) &&
        explicitIntent != "recommendation" &&
        !recommendationRequest.containsMatchIn(q)
    val asksAboutWork = isPureAnalysisAsk || previousWorkReference.containsMatchIn(q) || mentionsKnownWork
    val pendingWorkReply = if (asksAboutWork && explicitIntent != "recommendation" && !recommendationRequest.containsMatchIn(q)) {
        buildWorkReply(q, catalog, previousTitle, depth, spoilerNote)
    } else {
        null
    }
    if (pendingWorkReply != null && pendingWorkReply.analyzedTitle != null) {
        return pendingWorkReply
    }

    val conceptMatch = KmpLiteraryAdvisorKnowledge.findConceptReply(q, depth)
    if (conceptMatch != null && explicitIntent != "recommendation") {
        val (concept, bodyText) = conceptMatch
        val queryForCatalog = if (concept.recommendedAuthors.isNotEmpty()) {
            "$prompt ${concept.recommendedAuthors.joinToString(" ")}"
        } else {
            prompt
        }
        val conceptRecs = findCatalogMatches(queryForCatalog, catalog, limit = 2)
        return AdvisorMessage(
            id = "",
            sender = AdvisorSender.ADVISOR,
            content = "$bodyText$spoilerNote",
            intent = explicitIntent ?: "analysis",
            answerKind = concept.categoryLabel,
            recommendedBooks = conceptRecs
        )
    }

    if (isPureAnalysisAsk && pendingWorkReply != null) {
        return pendingWorkReply
    }

    val recs = findCatalogMatches(prompt, catalog, limit = 2)
    val lead = recs.firstOrNull()
    val leadDesc = lead?.let { "\"**${it.title}**\" (*${it.author}*)" } ?: "sesli kütüphanemizden seçkin bir klasik"

    val body = when {
        q.contains("polisiye") || q.contains("sherlock") || q.contains("kizil dosya") || q.contains("gizem") || q.contains("surukleyici") ->
            when (depth) {
                "brief" -> "Soluk soluğa ilerleyen bir zekâ oyunu arıyorsan sahaf rafımdan hiç tereddüt etmeden $leadDesc eserini uzatırım. Her ipucu bir sonraki kapıyı aralar."
                "deep" -> "Kıymetli okur, gerçek polisiye yalnızca suçluyu bulmak değil, insan aklının ayrıntılardaki izleri nasıl okuduğuna tanıklık etmektir. $leadDesc, keskin gözlem gücü, Viktorya dönemi atmosferi ve mantık örgüsüyle kulaklığı taktığın andan itibaren zamanı unutturur. Sesli anlatımın temposu, merak duygusunu ilk dakikalardan zirveye taşır."
                else -> "Zihninin keskin bir maceraya ve gizem perdesini aralamaya susadığını hissediyorum aziz dostum. Bu atmosferde $leadDesc, adım adım örülen ipuçları ve sürükleyici anlatımıyla eşsiz bir başlangıç olur."
            }

        q.contains("melankolik") || q.contains("yagmur") || q.contains("huzun") || q.contains("kurk mantolu") ->
            when (depth) {
                "brief" -> "Yağmurlu bir günün dingin hüznüne en yakışan yoldaş $leadDesc olacaktır. Sessizce dinleyip iç dünyanda yankısını bulabilirsin."
                "deep" -> "İnsan kalbi bazen yüksek sesli kahramanlıklardan çok, yarım kalmış bir cümlenin veya içine çekilmiş bir ruhun sessizliğinde kendi suretini görür. $leadDesc, incelikli duygusal derinliği ve yalın lirizmiyle melankoliyi kırgınlığa değil, sıcacık bir iç hesaplaşmaya dönüştürür."
                else -> "Hüzünlü ve yağmurlu bir saatte içini ısıtacak en samimi sahaf önerim $leadDesc. Sözcüklerin sakin ritmi kalbine ağır ağır dokunacaktır."
            }

        q.contains("felsefe") || q.contains("dingin") ->
            "Aziz dostum, kahven eşliğinde düşünceleri demlemek istediğinde felsefi klasikler zihnindeki gürültüyü susturur. $leadDesc eserini dinlerken her bölümün üzerine birkaç dakika durup düşünmeni öneririm."

        q.contains("umut") || q.contains("ilham") ->
            "Kıymetli okur, ruhunu tazeleyecek ve insana yeniden başlama cesareti verecek sıcacık bir anlatı olarak $leadDesc eserini seçtim. Yalın ama derin hikâyesi gününe ferahlık katacaktır."

        else ->
            when (depth) {
                "brief" -> "İsteğin üzerine kütüphanemizden tam sana göre $leadDesc eserini çıkardım. Aşağıdaki karttan hemen dinlemeye başlayabilirsin."
                "deep" -> "Aziz dostum, paylaştığın merak doğrultusunda sesli kitap arşivimizdeki en uyumlu eserleri taradım. Özellikle $leadDesc; üslubu, tematik derinliği ve seslendirme ritmiyle şu anki arayışına güçlü bir karşılık veriyor. İstersen bu eser üzerinden karakter analizi de yapabiliriz."
                else -> "Sahaf raflarımızda arayışına en iyi eşlik edecek seçkiyi hazırladım aziz dostum. Başta $leadDesc olmak üzere aşağıdaki eserlerle keyifli bir dinleme yolculuğuna çıkabilirsin."
            }
    }

    return AdvisorMessage(
        id = "",
        sender = AdvisorSender.ADVISOR,
        content = "$body$spoilerNote",
        intent = explicitIntent ?: "recommendation",
        answerKind = "Sahaf tavsiyesi",
        recommendedBooks = recs
    )
}
