package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.semtinazizi.kitapduy.ui.viewmodel.AuthViewModel
import com.semtinazizi.kitapduy.ui.components.GoogleIcon
import com.semtinazizi.kitapduy.ui.components.AppleIcon
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

class WelcomeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val authViewModel = koinViewModel<AuthViewModel>()
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        var isLoading by remember { mutableStateOf(false) }
        var showGuestInput by remember { mutableStateOf(false) }
        var guestName by remember { mutableStateOf("") }

        val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
        val authError by authViewModel.error.collectAsState()
        val callbackError by com.semtinazizi.kitapduy.platform.AuthenticationMessages.error.collectAsState()
        val uriHandler = LocalUriHandler.current

        LaunchedEffect(callbackError) {
            callbackError?.let {
                snackbarHostState.showSnackbar(it)
                com.semtinazizi.kitapduy.platform.AuthenticationMessages.error.value = null
            }
        }

        LaunchedEffect(authError) {
            authError?.let {
                snackbarHostState.showSnackbar(it)
                authViewModel.clearError()
            }
        }

        LaunchedEffect(isLoggedIn) {
            if (isLoggedIn) {
                navigator.replaceAll(MainScreen())
            }
        }

        if (com.semtinazizi.kitapduy.platform.isSimulatorCiSmokeTest) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(1000)
                if (!isLoggedIn) {
                    authViewModel.signInAnonymously("Misafir Test")
                }
            }
        }

        val infiniteTransition = rememberInfiniteTransition(label = "zoom_transition")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(20000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "zoom"
        )

        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?q=80&w=1080",
                contentDescription = "Background",
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scale),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "KitapDuy",
                    color = Color(0xFFFFD700),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Binlerce Dünyaya\nKulak Verin.",
                    color = Color.White,
                    fontSize = 40.sp,
                    lineHeight = 44.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "En sevdiğin sesli kitapları, podcast'leri ve orijinal içerikleri kesintisiz keşfetmeye hemen başla.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Google Login
                Surface(
                    onClick = {
                        if (!isLoading) {
                            scope.launch {
                                isLoading = true
                                try {
                                    val url = authViewModel.getGoogleOAuthUrl()
                                    if (url.isNotEmpty()) com.semtinazizi.kitapduy.platform.openAuthenticationUrl(url)
                                } catch (e: Exception) { e.printStackTrace() }
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(GoogleIcon, contentDescription = "Google", tint = Color.Unspecified, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Google ile Devam Et", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Apple Login - iOS'ta native ASAuthorizationController kullanır (WebView değil)
                Surface(
                    onClick = {
                        if (!isLoading) {
                            isLoading = true
                            try {
                                com.semtinazizi.kitapduy.platform.triggerNativeAppleSignIn()
                            } catch (e: Exception) { e.printStackTrace() }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    color = Color.Black,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(AppleIcon, contentDescription = "Apple", tint = Color.Unspecified, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Apple ile Devam Et", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }


                Spacer(Modifier.height(12.dp))

                // Email Login
                Surface(
                    onClick = {
                        if (!isLoading) navigator.push(LoginScreen())
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    color = Color.White.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Email, contentDescription = "Email", tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("E-Posta ile Giriş Yap", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Guest Login
                AnimatedContent(
                    targetState = showGuestInput,
                    label = "GuestInputAnimation"
                ) { isShowingInput ->
                    if (isShowingInput) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            OutlinedTextField(
                                value = guestName,
                                onValueChange = { if (it.length <= 20) guestName = it },
                                placeholder = { Text("Adınız nedir?", color = Color.White.copy(alpha = 0.5f)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                    focusedContainerColor = Color.White.copy(alpha = 0.1f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedBorderColor = Color.White
                                )
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val finalName = if (guestName.isNotBlank()) guestName.trim() else "Misafir"
                                    if (!isLoading) {
                                        scope.launch {
                                            isLoading = true
                                            authViewModel.signInAnonymously(finalName)
                                            isLoading = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                                enabled = !isLoading
                            ) {
                                Text("Keşfetmeye Başla", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text(
                            text = "Misafir Olarak Keşfet",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showGuestInput = true }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
