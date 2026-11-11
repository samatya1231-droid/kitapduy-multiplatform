package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.util.root
import com.semtinazizi.kitapduy.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

class LoginScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val authViewModel = koinViewModel<AuthViewModel>()
        val coroutineScope = rememberCoroutineScope()

        var isLoginMode by remember { mutableStateOf(true) }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        val BrandGold = Color(0xFFFFD700)

        // Eğer kullanıcı zaten giriş yapmışsa direkt MainScreen'e yönlendir
        val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
        LaunchedEffect(isLoggedIn) {
            if (isLoggedIn) {
                navigator.root.replaceAll(MainScreen())
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // --- Animated Blobs Background ---
            val infiniteTransition = rememberInfiniteTransition(label = "bg_blobs")

            val blobOffset1 by infiniteTransition.animateValue(
                initialValue = IntOffset(-100, -100),
                targetValue = IntOffset(100, 100),
                typeConverter = IntOffset.VectorConverter,
                animationSpec = infiniteRepeatable(
                    animation = tween(8000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "offset1"
            )

            val blobOffset2 by infiniteTransition.animateValue(
                initialValue = IntOffset(200, 400),
                targetValue = IntOffset(-100, 600),
                typeConverter = IntOffset.VectorConverter,
                animationSpec = infiniteRepeatable(
                    animation = tween(12000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "offset2"
            )

            Box(
                modifier = Modifier
                    .offset(blobOffset1.x.dp, blobOffset1.y.dp)
                    .size(400.dp)
                    .background(BrandGold.copy(alpha = 0.08f), CircleShape)
                    .blur(100.dp)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(blobOffset2.x.dp, blobOffset2.y.dp)
                    .size(350.dp)
                    .background(Color(0xFF8B5CF6).copy(alpha = 0.06f), CircleShape)
                    .blur(100.dp)
            )
            // --- End of Animated Blobs ---

            IconButton(
                onClick = {
                    if (navigator.canPop) navigator.pop() else navigator.root.replaceAll(WelcomeScreen())
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Geri",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Glassmorphism Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            RoundedCornerShape(24.dp)
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isLoginMode) "Tekrar Merhaba!" else "Aramıza Katıl",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Serif,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (isLoginMode) "Kütüphanene erişmek için giriş yap" else "Edebiyatın büyüleyici dünyasına adım at",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                if (!isLoginMode) {
                    GlassTextField(
                        value = name,
                        onValueChange = { name = it },
                        hint = "Ad Soyad",
                        icon = Icons.Filled.Person,
                        BrandGold = BrandGold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                GlassTextField(
                    value = email,
                    onValueChange = { email = it },
                    hint = "E-posta",
                    icon = Icons.Filled.Email,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false
                    ),
                    BrandGold = BrandGold
                )

                Spacer(modifier = Modifier.height(16.dp))

                GlassTextField(
                    value = password,
                    onValueChange = { password = it },
                    hint = "Şifre",
                    icon = Icons.Filled.Lock,
                    isPassword = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        autoCorrectEnabled = false
                    ),
                    BrandGold = BrandGold
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        val cleanEmail = email.trim()
                        val cleanPassword = password.trim()
                        if (cleanEmail.isBlank() || cleanPassword.isBlank()) {
                            errorMessage = "Lütfen tüm alanları doldurun."
                            return@Button
                        }
                        
                        isLoading = true
                        errorMessage = null
                        
                        coroutineScope.launch {
                            val success = if (isLoginMode) {
                                authViewModel.signIn(cleanEmail, cleanPassword)
                            } else {
                                if (name.trim().isBlank()) {
                                    errorMessage = "Lütfen adınızı girin."
                                    isLoading = false
                                    return@launch
                                }
                                authViewModel.signUp(cleanEmail, cleanPassword, name.trim())
                            }
                            
                            isLoading = false
                            if (!success) {
                                errorMessage = "İşlem başarısız oldu. Lütfen bilgilerinizi kontrol edin."
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGold.copy(alpha = 0.9f)),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.Black
                        )
                    } else {
                        Text(
                            text = if (isLoginMode) "Giriş Yap" else "Kayıt Ol",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    Text(
                        "veya",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(onClick = {
                    isLoginMode = !isLoginMode
                    errorMessage = null
                }) {
                    Text(
                        text = if (isLoginMode) "Hesabın yok mu? Kayıt Ol" else "Zaten hesabın var mı? Giriş Yap",
                        color = BrandGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun GlassTextField(
        value: String,
        onValueChange: (String) -> Unit,
        hint: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        isPassword: Boolean = false,
        keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
        BrandGold: Color
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(hint, color = Color.Gray) },
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
                    RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            keyboardOptions = keyboardOptions,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                focusedBorderColor = BrandGold
            ),
            leadingIcon = { Icon(icon, null, tint = BrandGold.copy(alpha = 0.7f)) }
        )
    }
}
