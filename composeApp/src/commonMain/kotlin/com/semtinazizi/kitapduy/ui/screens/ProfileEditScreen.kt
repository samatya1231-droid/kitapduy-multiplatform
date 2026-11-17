package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.ui.components.SmartImage
import com.semtinazizi.kitapduy.ui.viewmodel.AuthViewModel
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

class ProfileEditScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val authViewModel = koinViewModel<AuthViewModel>()
        val userProfile by authViewModel.userProfile.collectAsState()
        val coroutineScope = rememberCoroutineScope()
        val focusManager = LocalFocusManager.current

        var name by remember { mutableStateOf(userProfile?.name ?: "") }
        var username by remember { mutableStateOf(userProfile?.username ?: "") }
        var bio by remember { mutableStateOf(userProfile?.bio ?: "") }
        var isLoading by remember { mutableStateOf(false) }
        var feedback by remember { mutableStateOf<String?>(null) }
        var isImageUploading by remember { mutableStateOf(false) }

        LaunchedEffect(userProfile?.uid) {
            userProfile?.let { profile ->
                name = profile.name
                username = profile.username.orEmpty()
                bio = profile.bio.orEmpty()
            }
        }

        val imagePicker = rememberFilePickerLauncher(
            type = PickerType.Image,
            mode = PickerMode.Single,
            title = "Profil Fotoğrafı Seç"
        ) { file ->
            file?.let { platformFile ->
                isImageUploading = true
                feedback = null
                coroutineScope.launch {
                    try {
                        val bytes = platformFile.readBytes()
                        val extension = platformFile.name.substringAfterLast('.', "jpg")
                        authViewModel.updateProfilePicture(bytes, extension) { success ->
                            isImageUploading = false
                            if (!success) feedback = "Fotoğraf kaydedilemedi. Tekrar dene."
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        isImageUploading = false
                        feedback = "Fotoğraf okunamadı. Tekrar dene."
                    }
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Profili Düzenle") },
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues).imePadding().verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Picture Section
                Box(contentAlignment = Alignment.Center) {
                    SmartImage(
                        imageUrl = userProfile?.photoUrl,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { if (!isImageUploading) imagePicker.launch() },
                        contentDescription = "Profil Fotoğrafı",
                        isAvatar = true,
                        fallbackText = userProfile?.name ?: ""
                    )
                    
                    if (isImageUploading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    // Düzenle Butonu
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-4).dp, y = (-4).dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { if (!isImageUploading) imagePicker.launch() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Düzenle",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (!userProfile?.photoUrl.isNullOrEmpty()) {
                    TextButton(
                        enabled = !isImageUploading,
                        onClick = {
                            isImageUploading = true
                            authViewModel.removeProfilePicture { isImageUploading = false }
                        }
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Fotoğrafı Kaldır")
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("İsim Soyisim") },
                    modifier = Modifier.fillMaxWidth().testTag("profile-name"),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    singleLine = true
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Kullanıcı Adı") },
                    modifier = Modifier.fillMaxWidth().testTag("profile-username"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Hakkımda (Biyografi)") },
                    modifier = Modifier.fillMaxWidth().testTag("profile-bio"),
                    minLines = 3,
                    maxLines = 5
                )

                feedback?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            val success = authViewModel.updateProfile(name, bio, username.takeIf { it.isNotBlank() })
                            isLoading = false
                            if (success) {
                                navigator.pop()
                            } else {
                                feedback = "Profil kaydedilemedi. Tekrar dene."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("profile-save"),
                    enabled = !isLoading && !isImageUploading && name.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Kaydet")
                    }
                }
            }
        }
    }
}
