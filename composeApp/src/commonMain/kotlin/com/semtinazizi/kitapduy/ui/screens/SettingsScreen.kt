package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.util.root
import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import com.semtinazizi.kitapduy.platform.AdsPrivacyBridge
import com.semtinazizi.kitapduy.platform.PushNotificationBridge

data class SettingsScreen(val isPremium: Boolean = false) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val authRepository = koinInject<AuthRepository>()
        val authViewModel = org.koin.compose.viewmodel.koinViewModel<com.semtinazizi.kitapduy.ui.viewmodel.AuthViewModel>()
        val coroutineScope = rememberCoroutineScope()
        val billing = koinInject<com.semtinazizi.kitapduy.platform.BillingManager>()
        val storePremium by billing.isPremium.collectAsState()
        val premium = isPremium || storePremium
        var themeDialog by remember { mutableStateOf(false) }
        var selectedTheme by remember { mutableStateOf(com.semtinazizi.kitapduy.util.KmpThemeManager.getActiveTheme()) }
        if (themeDialog) AlertDialog(onDismissRequest = { themeDialog = false }, title = { Text("Tema Seçimi") },
            text = { Column {
                listOf("Sleek", "Gece Mavisi", "Kahve Rengi", "Orman Yeşili", "Aura", "Aurora").forEach { theme ->
                    Row(Modifier.fillMaxWidth().clickable {
                        selectedTheme = theme
                        com.semtinazizi.kitapduy.util.KmpThemeManager.setTheme(theme)
                    }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedTheme == theme, onClick = null)
                        Text(theme)
                    }
                }
            } }, confirmButton = { TextButton(onClick = { themeDialog = false }) { Text("Kapat") } })
        val showAdsPrivacy by AdsPrivacyBridge.isRequired.collectAsState()
        val notificationsEnabled by PushNotificationBridge.enabled.collectAsState()
        var showNotificationOptions by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { PushNotificationBridge.refresh() }
        if (showNotificationOptions) {
            AlertDialog(onDismissRequest = { showNotificationOptions = false },
                title = { Text("Bildirimler") },
                text = { Text(if (notificationsEnabled == true) "KitapDuy bildirimlerini kapatmak istiyor musunuz?"
                    else "KitapDuy bildirimlerini açmak için bildirim izni verin.") },
                confirmButton = { TextButton(onClick = {
                    if (notificationsEnabled == true) PushNotificationBridge.disable()
                    else PushNotificationBridge.requestPermission()
                    showNotificationOptions = false
                }) { Text(if (notificationsEnabled == true) "Kapat" else "İzin ver") } },
                dismissButton = { TextButton(onClick = { showNotificationOptions = false }) { Text("Vazgeç") } })
        }
        var isSigningOut by remember { mutableStateOf(false) }
        var signOutError by remember { mutableStateOf<String?>(null) }
        signOutError?.let { message ->
            AlertDialog(onDismissRequest = { signOutError = null }, text = { Text(message) },
                confirmButton = { TextButton(onClick = { signOutError = null }) { Text("Tamam") } })
        }
        var showDeleteConfirmation by remember { mutableStateOf(false) }
        var isDeletingAccount by remember { mutableStateOf(false) }
        var deleteError by remember { mutableStateOf<String?>(null) }

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { if (!isDeletingAccount) showDeleteConfirmation = false },
                title = { Text("Hesabı Sil", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black) },
                text = {
                    Column {
                        Text(
                            "Hesabınızın ve ilişkili kişisel verilerinizin kalıcı olarak silinmesini onaylıyor musunuz? Bu işlem geri alınamaz. Mağaza aboneliğiniz otomatik iptal edilmez; aboneliğinizi ayrıca mağazadan yönetmelisiniz.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (deleteError != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(deleteError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isDeletingAccount = true
                            deleteError = null
                            coroutineScope.launch {
                                try {
                                    if (authRepository.deleteAccount()) {
                                        authViewModel.checkAuthStatus()
                                        showDeleteConfirmation = false
                                        navigator.root.replaceAll(WelcomeScreen())
                                    } else {
                                        deleteError = "Hesabın silindiği doğrulanamadı. Lütfen tekrar deneyin veya ${LegalContent.supportEmail} adresine yazın."
                                    }
                                } finally {
                                    isDeletingAccount = false
                                }
                            }
                        },
                        enabled = !isDeletingAccount,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4B4B)),
                        shape = RoundedCornerShape(50)
                    ) {
                        if (isDeletingAccount) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Evet, Sil", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { if (!isDeletingAccount) showDeleteConfirmation = false }) {
                        Text("Vazgeç", color = Color.Gray)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(28.dp)
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Ayarlar", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                if (premium) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.WorkspacePremium, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("Premium üyesiniz", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                                Text("Tüm özelliklerin tadını çıkarın!", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            }
                        }
                    }
                }

                SettingsSectionTitle("Görünüm")
                SettingsItem(icon = Icons.Filled.Palette, title = "Tema Seçimi", subtitle = "6 özel renk teması (Premium)") {
                    if (premium) themeDialog = true else navigator.root.push(SubscriptionPaywallScreen())
                }
                val darkMode = com.semtinazizi.kitapduy.util.KmpThemeManager.isDarkMode(false)
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Koyu tema", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                    Switch(checked = darkMode, onCheckedChange = {
                        com.semtinazizi.kitapduy.util.KmpThemeManager.setDarkMode(it)
                    })
                }
                SettingsSectionTitle("Uygulama")
                SettingsItem(icon = Icons.Filled.WorkspacePremium, title = "Premium", subtitle = "Planlar ve satın alımları geri yükleme") { navigator.root.push(SubscriptionPaywallScreen()) }
                SettingsItem(icon = Icons.Filled.BarChart, title = "İstatistikler") { navigator.push(StatisticsScreen()) }
                SettingsItem(icon = Icons.Filled.Language, title = "Uygulama Dili", subtitle = "Türkçe") { }
                SettingsItem(icon = Icons.Filled.Notifications, title = "Bildirimler", subtitle = when (notificationsEnabled) {
                    true -> "Açık"
                    false -> "Kapalı"
                    null -> "Kullanılamıyor"
                }) { if (notificationsEnabled != null) showNotificationOptions = true }
                SettingsItem(icon = Icons.Filled.Info, title = "Gizlilik Politikası") {
                    navigator.push(LegalScreen(LegalDocument.PRIVACY))
                }
                SettingsItem(icon = Icons.Filled.Description, title = "Kullanım Koşulları") {
                    navigator.push(LegalScreen(LegalDocument.TERMS))
                }
                if (showAdsPrivacy) {
                    SettingsItem(icon = Icons.Filled.PrivacyTip, title = "Reklam gizliliği tercihleri") {
                        AdsPrivacyBridge.openOptions()
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                SettingsSectionTitle("Hesap")
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = if (isSigningOut) "Çıkış yapılıyor…" else "Çıkış Yap",
                    iconTint = Color(0xFFFF4B4B),
                    titleColor = Color(0xFFFF4B4B)
                ) {
                    if (!isSigningOut && !isDeletingAccount) {
                        isSigningOut = true
                        coroutineScope.launch {
                            try {
                                authRepository.signOut()
                                authViewModel.checkAuthStatus()
                                navigator.root.replaceAll(WelcomeScreen())
                            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                            catch (e: Exception) { signOutError = "Çıkış yapılamadı. Bağlantınızı kontrol edip tekrar deneyin." }
                            finally { isSigningOut = false }
                        }
                    }
                }

                SettingsItem(
                    icon = Icons.Filled.DeleteForever,
                    title = "Hesabı Sil",
                    subtitle = "Hesabınız ve ilişkili veriler silinir",
                    iconTint = Color(0xFFFF4B4B),
                    titleColor = Color(0xFFFF4B4B)
                ) {
                    if (!isSigningOut) showDeleteConfirmation = true
                }

                Spacer(modifier = Modifier.height(40.dp))
                Text(text = "KitapDuy v1.0.1", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Black,
        fontSize = 14.sp,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, start = 8.dp),
        textAlign = TextAlign.Start
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = iconTint.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.padding(8.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, color = titleColor, fontSize = 16.sp)
                if (subtitle != null) Text(text = subtitle, color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}
