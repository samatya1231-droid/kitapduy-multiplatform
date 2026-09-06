package com.semtinazizi.kitapduy

import androidx.compose.ui.window.ComposeUIViewController
import com.semtinazizi.kitapduy.di.appModules
import com.semtinazizi.kitapduy.platform.IOSBuildConfig
import com.semtinazizi.kitapduy.platform.DailyQuoteScheduler
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
    IOSBuildConfig.SUPABASE_URL = supabaseUrl
    IOSBuildConfig.SUPABASE_KEY = supabaseKey
    IOSBuildConfig.CF_ACCOUNT_ID = cfAccountId
    IOSBuildConfig.CF_PUBLIC_URL = cfPublicUrl
    
    // Zamanlanmis gunluk alinti bildirimleri iOS'ta crash yaptigi icin gecici olarak devre disi birakildi
    // DailyQuoteScheduler.scheduleDaily(null)
}

/**
 * kitapduy://login-callback URL'ini Supabase Auth'a iletir.
 * Google/Apple OAuth donusunde Swift tarafindan cagirilir.
 */
fun handleOAuthDeepLink(url: String) {
    try {
        val supabaseClient = com.semtinazizi.kitapduy.data.SupabaseManager.client
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            try {
                supabaseClient.auth.parseFragmentAndImportSession(url)
            } catch (e: Throwable) {
                println("KitapDuy: OAuth deep link isleme hatasi: ${e.message}")
            }
        }
    } catch (e: Throwable) {
        println("KitapDuy: OAuth deep link hatasi (Supabase hazir degil): ${e.message}")
    }
}
