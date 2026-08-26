import os

path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\WelcomeScreen.kt'
c = '''package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.painterResource
import kitapduy_multiplatform.composeapp.generated.resources.Res
import kitapduy_multiplatform.composeapp.generated.resources.ic_google_logo
import kitapduy_multiplatform.composeapp.generated.resources.ic_apple_logo

class WelcomeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val authViewModel = koinViewModel<AuthViewModel>()
        val scope = rememberCoroutineScope()
        var isLoading by remember { mutableStateOf(false) }
        val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
        val uriHandler = LocalUriHandler.current

        LaunchedEffect(isLoggedIn) {
            if (isLoggedIn) {
                navigator.replaceAll(MainScreen())
            }
        }

        // Cinematic zoom animation
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
            // 1. Cinematic Background Image
            AsyncImage(
                model = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?q=80&w=1080",
                contentDescription = "Background",
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scale),
                contentScale = ContentScale.Crop
            )

            // 2. Dark Gradient Overlay
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

            // 3. Bottom Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Brand Name
                Text(
                    text = "KitapDuy",
                    color = Color(0xFFFFD700), // BrandGold
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Impactful Typography
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
                    fontSize = 15.sp,
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
                                    if (url.isNotEmpty()) uriHandler.openUri(url)
                                } catch (e: Exception) { e.printStackTrace() }
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(Res.drawable.ic_google_logo),
                                    contentDescription = "Google",
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text("Google ile Devam Et", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Email Login
                Surface(
                    onClick = {
                        if (!isLoading) navigator.push(LoginScreen())
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Email, contentDescription = "Email", tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("E-Posta ile Giriş Yap", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
'''
with open(path, 'w', encoding='utf-8') as f:
    f.write(c)

print("Cinematic WelcomeScreen created!")
