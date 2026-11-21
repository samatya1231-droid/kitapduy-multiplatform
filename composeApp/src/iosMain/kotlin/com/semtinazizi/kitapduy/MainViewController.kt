package com.semtinazizi.kitapduy

import androidx.compose.ui.window.ComposeUIViewController
import com.semtinazizi.kitapduy.di.appModules
import com.semtinazizi.kitapduy.platform.IOSBuildConfig
import com.semtinazizi.kitapduy.platform.IosAuthenticationBridge
import com.semtinazizi.kitapduy.platform.installKotlinExceptionHook
import org.koin.core.context.startKoin
/**
 * iOS giriş noktası - Swift/Xcode tarafından çağrılır
 * Bu fonksiyon KMP framework'ü içinden Swift'e export edilir
 */
fun MainViewController() = ComposeUIViewController(
    configure = {
        // Kotlin/Native global exception hook - arka plan thread crash'lerini önler
        try {
            installKotlinExceptionHook()
        } catch (e: Throwable) {
            println("Exception hook install failed: ${e.message}")
        }

        try {
            startKoin {
                modules(appModules)
            }
        } catch (e: Throwable) {
            println("Koin error or already started: ${e.message}")
        }
    }
) {
    App()
}

/**
 * iOS tarafından çağrılarak config değerlerini inject eder
 */
fun initializeIOSConfig(
    supabaseUrl: String,
    supabaseKey: String,
    cfAccountId: String,
    cfPublicUrl: String
) {
    if (supabaseUrl.isNotBlank()) IOSBuildConfig.SUPABASE_URL = supabaseUrl.trim()
    if (supabaseKey.isNotBlank()) IOSBuildConfig.SUPABASE_KEY = supabaseKey.trim()
    if (cfAccountId.isNotBlank()) IOSBuildConfig.CF_ACCOUNT_ID = cfAccountId.trim()
    if (cfPublicUrl.isNotBlank()) IOSBuildConfig.CF_PUBLIC_URL = cfPublicUrl.trim()
    
    // Zamanlanmis gunluk alinti bildirimleri iOS'ta crash yaptigi icin gecici olarak devre disi birakildi
    // DailyQuoteScheduler.scheduleDaily(null)
}

/**
 * kitapduy://login-callback URL'ini Supabase Auth'a iletir.
 * Google/Apple OAuth donusunde Swift tarafindan cagirilir.
 * PKCE kodunu mevcut kimlik dogrulama koprusu uzerinden isler.
 */
fun handleOAuthDeepLink(url: String) {
    val clean = url.trim()
    val isOAuth = clean.startsWith("kitapduy://login-callback", ignoreCase = true) ||
        clean.contains("code=", ignoreCase = true) ||
        clean.contains("access_token=", ignoreCase = true) ||
        clean.contains("error=", ignoreCase = true)
    if (isOAuth) {
        IosAuthenticationBridge.handleCallback(clean)
    } else if (clean.startsWith("kitapduy://", ignoreCase = true)) {
        com.semtinazizi.kitapduy.platform.PushNotificationBridge.onNotificationClicked(
            socialType = null,
            postId = null,
            bookId = null,
            deepLink = clean
        )
    } else {
        IosAuthenticationBridge.handleCallback(clean)
    }
}
