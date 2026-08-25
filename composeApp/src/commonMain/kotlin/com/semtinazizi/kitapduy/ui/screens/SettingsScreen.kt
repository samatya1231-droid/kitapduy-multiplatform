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
import com.semtinazizi.kitapduy.data.SupabaseManager
import kotlinx.coroutines.launch

data class SettingsScreen(val isPremium: Boolean = false) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val coroutineScope = rememberCoroutineScope()
        var showDeleteConfirmation by remember { mutableStateOf(false) }

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Hesabı Sil 🗑️", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black) },
                text = { Text("Hesabınızı silmek istediğinize emin misiniz? Bu işlem geri alınamaz ve tüm verileriniz kalıcı olarak silinecektir.", color = Color.LightGray) },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmation = false
                            // TODO: Implement delete account in KMP
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4B4B)),
                        shape = RoundedCornerShape(50)
                    ) { Text("Evet, Sil", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) { Text("Vazgeç", color = Color.Gray) }
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

                // Premium Banner
                if (isPremium) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD700).copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(40.dp))
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("Premium Üyesiniz \uD83D\uDC51", fontWeight = FontWeight.Black, color = Color(0xFFFFD700), fontSize = 18.sp)
                                Text("Tüm özelliklerin tadını çıkarın!", color = Color.LightGray, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // General Settings
                SettingsSectionTitle("Genel")
                SettingsItem(icon = Icons.Filled.Language, title = "Uygulama Dili", subtitle = "Türkçe") { }
                SettingsItem(icon = Icons.Filled.Notifications, title = "Bildirimler", subtitle = "Açık") { }

                Spacer(modifier = Modifier.height(24.dp))

                // Account
                SettingsSectionTitle("Hesap")
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = "Çıkış Yap",
                    iconTint = Color(0xFFFF4B4B),
                    titleColor = Color(0xFFFF4B4B)
                ) {
                    coroutineScope.launch {
                        try {
                            SupabaseManager.auth.signOut()
                            navigator.replaceAll(LoginScreen())
                        } catch (e: Exception) {
                            // Handle error
                        }
                    }
                }
                
                SettingsItem(
                    icon = Icons.Filled.DeleteForever,
                    title = "Hesabı Sil",
                    iconTint = Color(0xFFFF4B4B),
                    titleColor = Color(0xFFFF4B4B)
                ) {
                    showDeleteConfirmation = true
                }

                Spacer(modifier = Modifier.height(40.dp))
                
                Text(
                    text = "KitapDuy v2.0.0",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp, start = 8.dp),
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.padding(8.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, color = titleColor, fontSize = 16.sp)
                if (subtitle != null) {
                    Text(text = subtitle, color = Color.Gray, fontSize = 13.sp)
                }
            }
        }
    }
}
