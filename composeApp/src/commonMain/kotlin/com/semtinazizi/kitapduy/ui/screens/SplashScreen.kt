package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

import com.semtinazizi.kitapduy.ui.theme.SleekDarkBackground
import com.semtinazizi.kitapduy.ui.theme.SleekChampagne

class SplashScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val authViewModel = koinViewModel<AuthViewModel>()

        LaunchedEffect(Unit) {
            // Animasyon veya min. bekleme süresi
            delay(1500)
            
            // Supabase auth oturumunu kontrol et
            val isLoggedIn = authViewModel.checkAuthStatus()
            
            if (isLoggedIn) {
                navigator.replace(MainScreen())
            } else {
                // Ayarlardan "isFirstLaunch" kontrolü de eklenebilir, şimdilik hep WelcomeScreen'e gidiyor.
                navigator.replace(WelcomeScreen())
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SleekDarkBackground),
            contentAlignment = Alignment.Center
        ) {
            // Eski logoyu temsil etmesi için şimdilik text/shape kullanabiliriz.
            // (Drawable'lar eklendiğinde Image composable ile değiştirilebilir)
            Text(
                text = "KitapDuy",
                color = SleekChampagne,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
