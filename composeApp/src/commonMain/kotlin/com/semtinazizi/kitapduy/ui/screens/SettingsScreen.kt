package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.util.KmpThemeManager
import androidx.compose.foundation.isSystemInDarkTheme

class SettingsScreen(val isPremium: Boolean = false) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        
        var expanded by remember { mutableStateOf(false) }
        var currentTheme by remember { mutableStateOf(KmpThemeManager.getActiveTheme()) }
        val themes = listOf("Sleek", "Gece Mavisi", "Kahve Rengi", "Orman Yeşili", "Aura", "Aurora")
        
        val systemDefaultDark = isSystemInDarkTheme()
        var isDarkMode by remember { mutableStateOf(KmpThemeManager.isDarkMode(systemDefaultDark)) }
        var isSystemDefaultMode by remember { mutableStateOf(!KmpThemeManager.hasCustomDarkModeSetting()) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Ayarlar") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    Text(
                        text = "Görünüm Ayarları",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Karanlık Mod (Dark Mode) Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Karanlık Mod",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Uygulamanın genel aydınlık/karanlık temasını belirler.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { checked ->
                                isDarkMode = checked
                                KmpThemeManager.setDarkMode(checked)
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Uygulama Teması (Sadece Premium)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(modifier = Modifier.fillMaxWidth().wrapContentSize(androidx.compose.ui.Alignment.TopStart)) {
                        OutlinedTextField(
                            value = currentTheme,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = isPremium) { expanded = true },
                            trailingIcon = {
                                if (isPremium) {
                                    Icon(Icons.Filled.ArrowDropDown, "dropdown", Modifier.clickable { expanded = true })
                                } else {
                                    Icon(Icons.Filled.Lock, "premium locked", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            enabled = false // to make the whole area clickable via Modifier
                        )
                        DropdownMenu(
                            expanded = expanded && isPremium,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            themes.forEach { themeName ->
                                DropdownMenuItem(
                                    text = { Text(themeName) },
                                    onClick = {
                                        KmpThemeManager.setTheme(themeName)
                                        currentTheme = themeName
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    if (!isPremium) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Özel temaları kullanabilmek için Premium üye olmalısınız.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
