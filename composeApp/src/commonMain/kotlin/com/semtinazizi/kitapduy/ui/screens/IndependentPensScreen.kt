package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.model.PoetryItem
import com.semtinazizi.kitapduy.data.SupabaseManager
import com.semtinazizi.kitapduy.ui.viewmodel.AuthViewModel
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

class IndependentPensScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val authViewModel = koinViewModel<AuthViewModel>()
        val userProfile by authViewModel.userProfile.collectAsState()
        val coroutineScope = rememberCoroutineScope()

        var poems by remember { mutableStateOf<List<PoetryItem>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var showAddDialog by remember { mutableStateOf(false) }
        
        var selectedPoem by remember { mutableStateOf<PoetryItem?>(null) }

        val loadPoems = {
            coroutineScope.launch {
                isLoading = true
                try {
                    val result = SupabaseManager.client.from("poems").select {
                        order("created_at", order = Order.DESCENDING)
                    }.decodeList<PoetryItem>()
                    poems = result
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isLoading = false
                }
            }
        }

        LaunchedEffect(Unit) {
            loadPoems()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Kalemler (Þiirler)", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Þiir Ekle")
                }
            }
        ) { paddingValues ->
            if (isLoading && poems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (poems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text("Henüz hiç þiir paylaþýlmamýþ. Ýlk sen ol!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(poems) { poem ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedPoem = poem
                            },
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp).animateContentSize()) {
                                Text(
                                    text = poem.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = poem.author,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text(
                                    text = poem.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "${poem.likes}", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            if (showAddDialog) {
                var newTitle by remember { mutableStateOf("") }
                var newContent by remember { mutableStateOf("") }
                var isSubmitting by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { if (!isSubmitting) showAddDialog = false },
                    title = { Text("Yeni Þiir Ekle") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newTitle,
                                onValueChange = { newTitle = it },
                                label = { Text("Baþlýk") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newContent,
                                onValueChange = { newContent = it },
                                label = { Text("Þiiriniz") },
                                modifier = Modifier.fillMaxWidth().height(150.dp),
                                maxLines = 10
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newTitle.isNotBlank() && newContent.isNotBlank() && userProfile != null) {
                                    isSubmitting = true
                                    coroutineScope.launch {
                                        try {
                                            val newPoem = PoetryItem(
                                                title = newTitle,
                                                author = userProfile?.name ?: "Anonim",
                                                userId = userProfile?.uid ?: "",
                                                userPhotoUrl = userProfile?.photoUrl,
                                                content = newContent
                                            )
                                            SupabaseManager.client.from("poems").insert(newPoem)
                                            showAddDialog = false
                                            loadPoems()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        } finally {
                                            isSubmitting = false
                                        }
                                    }
                                }
                            },
                            enabled = !isSubmitting && newTitle.isNotBlank() && newContent.isNotBlank()
                        ) {
                            if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            else Text("Paylaþ")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddDialog = false }, enabled = !isSubmitting) {
                            Text("Ýptal")
                        }
                    }
                )
            }
            
            if (selectedPoem != null) {
                AlertDialog(
                    onDismissRequest = { selectedPoem = null },
                    title = { 
                        Column {
                            Text(selectedPoem!!.title, fontWeight = FontWeight.Bold)
                            Text(selectedPoem!!.author, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    text = {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            item {
                                Text(selectedPoem!!.content)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { selectedPoem = null }) {
                            Text("Kapat")
                        }
                    }
                )
            }
        }
    }
}
