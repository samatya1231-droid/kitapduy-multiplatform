package com.semtinazizi.kitapduy

import androidx.compose.ui.window.ComposeUIViewController
import com.semtinazizi.kitapduy.di.appModules
import com.semtinazizi.kitapduy.platform.IOSBuildConfig
import com.semtinazizi.kitapduy.platform.DailyQuoteScheduler
import org.koin.core.context.startKoin

/**
 * iOS giriş noktası - Swift/Xcode tarafından çağrılır
 * Bu fonksiyon KMP framework'ü içinden Swift'e export edilir
 */
fun MainViewController() = ComposeUIViewController(
    configure = {
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
