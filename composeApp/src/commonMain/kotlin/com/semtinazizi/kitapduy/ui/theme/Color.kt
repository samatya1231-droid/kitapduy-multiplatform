package com.semtinazizi.kitapduy.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Premium Palette
val PremiumDarkBackground = Color(0xFF121212)
val PremiumCopper = Color(0xFFB87333)
val PremiumBronze = Color(0xFFCD7F32)
val PremiumIvory = Color(0xFFFFFFF0)
val PremiumPaper = Color(0xFFFDFBF7)
val PremiumNavy = Color(0xFF0A192F)

// Brand Palette (Now mapped to Sleek Theme)
var BrandDarkBackground by mutableStateOf(Color(0xFF0F0F11))
var BrandCardSurface by mutableStateOf(Color(0xFF1C1C1E))
var BrandGold by mutableStateOf(Color(0xFFE8D3B9))
var BrandAccent by mutableStateOf(Color(0xFFB08D6A))
var SoftOrange by mutableStateOf(Color(0xFFB08D6A))

// UI Modernization Palette (Mapped to Sleek Theme)
var DeepNight by mutableStateOf(Color(0xFF0F0F11))
var SoftGold by mutableStateOf(Color(0xFFE8D3B9))
var MutedSand by mutableStateOf(Color(0xFFB8A99A))
var PaperWhite by mutableStateOf(Color(0xFFF5F5F5))
var EditorialGray by mutableStateOf(Color(0xFF8E8E93))

// New Premium Accents (Mapped to Sleek Theme)
var PremiumGold by mutableStateOf(Color(0xFFE8D3B9))
var PremiumDeepBlue by mutableStateOf(Color(0xFF0F0F11))
var PremiumSurface by mutableStateOf(Color(0xFF1C1C1E))
var PremiumOnSurface by mutableStateOf(Color(0xFFEBEBF5))
var PremiumAccent by mutableStateOf(Color(0xFFE8D3B9))

// Sleek Storytel-like Premium Palette
var SleekDarkBackground by mutableStateOf(Color(0xFF0F0F11)) 
var SleekSurface by mutableStateOf(Color(0xFF1C1C1E))
var SleekChampagne by mutableStateOf(Color(0xFFE2B639)) // Daha canlı, premium sıcak altın
var SleekBronze by mutableStateOf(Color(0xFFC47D3B)) // Daha zengin bronz/bakır
var SleekOnSurface by mutableStateOf(Color(0xFFEBEBF5))
var SleekGray by mutableStateOf(Color(0xFF8E8E93))
