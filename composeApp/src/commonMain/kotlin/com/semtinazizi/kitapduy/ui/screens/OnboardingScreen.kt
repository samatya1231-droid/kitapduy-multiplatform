package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch

class OnboardingScreen : Screen {
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var name by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        val BrandGold = Color(0xFFFFD700)
        val pagerState = rememberPagerState(pageCount = { 4 })

        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "bg_blobs")
            
            val blobOffset1 by infiniteTransition.animateValue(
                initialValue = IntOffset(-100, -100),
                targetValue = IntOffset(100, 100),
                typeConverter = IntOffset.VectorConverter,
                animationSpec = infiniteRepeatable(animation = tween(8000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
                label = "offset1"
            )

            val blobOffset2 by infiniteTransition.animateValue(
                initialValue = IntOffset(200, 400),
                targetValue = IntOffset(-100, 600),
                typeConverter = IntOffset.VectorConverter,
                animationSpec = infiniteRepeatable(animation = tween(12000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
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

            Column(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    when (page) {
                        0 -> OnboardingSlide(
                            title = "KitapDuy",
                            subtitle = "Edebiyatın En Saf Hali",
                            description = "\"Kitaplar, başka bir dünyaya açılan kapılardır.\"",
                            icon = Icons.Filled.AutoAwesome,
                            color = BrandGold,
                            isFirstSlide = true
                        )
                        1 -> OnboardingSlide(
                            title = "Edebiyat Yolculuğu",
                            subtitle = "Seçkin Eserler ve Güncel Paylaşımlar",
                            description = "Sürekli büyüyen kütüphanemizde en özel eserleri keşfet, sesli paylaşımlarla edebiyatın keyfine var.",
                            icon = Icons.Filled.AutoStories,
                            color = Color(0xFF60A5FA)
                        )
                        2 -> OnboardingSlide(
                            title = "Arena & Topluluk",
                            subtitle = "Bilgini Sına, Düelloya Katıl",
                            description = "Edebiyat quizlerinde yarış, rozetler kazan ve okuma kulüplerinde sohbete başla.",
                            icon = Icons.Filled.EmojiEvents,
                            color = Color(0xFFF472B6)
                        )
                        3 -> AuthSlide(
                            name = name,
                            onNameChange = { name = it },
                            isLoading = isLoading,
                            onGoogleClick = {
                                // TODO: Platform spesifik URL açma veya Supabase OAuth
                            },
                            onAppleClick = {
                                // TODO: Apple Auth
                            },
                            onGuestClick = {
                                // TODO: Guest login
                                navigator.replace(MainScreen())
                            }
                        )
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(4) { index ->
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .size(width = if (isSelected) 24.dp else 8.dp, height = 8.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) BrandGold else Color.Gray.copy(alpha = 0.5f))
                                    .animateContentSize()
                            )
                        }
                    }

                    if (pagerState.currentPage < 3) {
                        TextButton(onClick = { scope.launch { pagerState.animateScrollToPage(3) } }) {
                            Text("Atla", color = Color.Gray)
                        }
                        
                        Button(
                            onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGold),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("İleri", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun OnboardingSlide(
        title: String,
        subtitle: String,
        description: String,
        icon: ImageVector,
        color: Color,
        isFirstSlide: Boolean = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "slide_anim")
            
            val offsetY by infiniteTransition.animateFloat(
                initialValue = -10f,
                targetValue = 10f,
                animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "offsetY"
            )
            
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.3f,
                animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "pulse"
            )
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "pulseAlpha"
            )

            Box(
                modifier = Modifier
                    .padding(top = 40.dp)
                    .offset(y = offsetY.dp)
                    .size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(pulseScale)
                        .background(color.copy(alpha = pulseAlpha), CircleShape)
                        .blur(30.dp)
                )
                
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape)
                        .border(1.5.dp, color.copy(alpha = 0.6f), CircleShape)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), Color.Transparent),
                                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                    end = androidx.compose.ui.geometry.Offset(300f, 300f)
                                )
                            )
                    )
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(60.dp), tint = color)
                }
            }

            Spacer(Modifier.height(56.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), RoundedCornerShape(24.dp))
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Serif
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text(
                        text = subtitle,
                        color = color,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = description,
                        color = Color.Gray,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AuthSlide(
        name: String,
        onNameChange: (String) -> Unit,
        isLoading: Boolean,
        onGoogleClick: () -> Unit,
        onAppleClick: () -> Unit,
        onGuestClick: () -> Unit
    ) {
        val BrandGold = Color(0xFFFFD700)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Hadi Başlayalım",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Serif
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Kütüphanene erişmek için bir yöntem seç",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            Surface(
                onClick = onGoogleClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.onSurface,
                shadowElevation = 8.dp,
                enabled = !isLoading
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.surface, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Google ile Devam Et", color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Surface(
                onClick = onAppleClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.onSurface,
                shadowElevation = 8.dp,
                enabled = !isLoading
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Apple ile Devam Et", color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Text("veya", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp))
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            }

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 20) onNameChange(it) },
                placeholder = { Text("Misafir Adı", color = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                    focusedBorderColor = BrandGold
                ),
                leadingIcon = { Icon(Icons.Filled.Person, null, tint = BrandGold.copy(alpha = 0.7f)) }
            )

            Spacer(Modifier.height(8.dp))
            
            Text(
                text = "⚠️ Satın alımlarınızın (Premium vb.) kayıt altında olması ve diğer cihazlarda da kullanılabilmesi için giriş yapmanız önerilir.",
                color = Color.Gray.copy(alpha = 0.8f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onGuestClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGold.copy(alpha = 0.9f)),
                enabled = !isLoading
            ) {
                Text("Misafir Olarak Başla", color = Color.Black, fontWeight = FontWeight.Black)
            }
        }
    }
}
