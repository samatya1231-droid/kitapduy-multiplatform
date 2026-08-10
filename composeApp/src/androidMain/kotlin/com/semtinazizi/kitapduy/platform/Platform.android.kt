package com.semtinazizi.kitapduy.platform

actual val platformName: String = "Android"

actual fun createAudioPlayer(): AudioPlayer = AndroidAudioPlayer()

actual object AppConfig {
    actual val supabaseUrl: String = BuildConfigHelper.supabaseUrl
    actual val supabaseKey: String = BuildConfigHelper.supabaseKey
    actual val cfAccountId: String = BuildConfigHelper.cfAccountId
    actual val cfPublicUrl: String = BuildConfigHelper.cfPublicUrl
}
