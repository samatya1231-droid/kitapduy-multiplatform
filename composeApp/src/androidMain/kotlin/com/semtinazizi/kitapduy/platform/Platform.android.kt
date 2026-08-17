package com.semtinazizi.kitapduy.platform

import android.os.Build
import com.semtinazizi.kitapduy.BuildConfig

actual val platformName: String = "Android ${Build.VERSION.SDK_INT}"

actual fun createAudioPlayer(): AudioPlayer = AndroidAudioPlayer()

actual object AppConfig {
    actual val supabaseUrl: String = BuildConfig.SUPABASE_URL
    actual val supabaseKey: String = BuildConfig.SUPABASE_KEY
    actual val cfAccountId: String = BuildConfig.CF_ACCOUNT_ID
    actual val cfPublicUrl: String = BuildConfig.CF_PUBLIC_URL
    actual val cfWorkerUrl: String = BuildConfig.CF_WORKER_URL
    actual val cfUploadToken: String = BuildConfig.CF_UPLOAD_TOKEN
}
