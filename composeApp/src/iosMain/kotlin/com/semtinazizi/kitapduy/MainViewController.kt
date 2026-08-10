import androidx.compose.ui.window.ComposeUIViewController
import com.semtinazizi.kitapduy.KitapDuyApp
import com.semtinazizi.kitapduy.di.appModules
import com.semtinazizi.kitapduy.platform.IOSBuildConfig
import org.koin.core.context.startKoin

/**
 * iOS giriş noktası - Swift/Xcode tarafından çağrılır
 * Bu fonksiyon KMP framework'ü içinden Swift'e export edilir
 */
fun MainViewController() = ComposeUIViewController(
    configure = {
        // Koin DI başlat (iOS için)
        startKoin {
            modules(appModules)
        }
    }
) {
    KitapDuyApp()
}

/**
 * iOS tarafından çağrılarak config değerlerini inject eder
 * Swift AppDelegate veya @main struct içinden çağrılmalı
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
}
