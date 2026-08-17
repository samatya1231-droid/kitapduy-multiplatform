package com.semtinazizi.kitapduy.platform

actual val platformName: String = "iOS"

actual fun createAudioPlayer(): AudioPlayer = IosAudioPlayer()

actual object AppConfig {
    // iOS'ta değerler Info.plist'ten veya hardcoded olarak gelir
    // Codemagic build sırasında environment variables ile doldurulur
    actual val supabaseUrl: String = IOSBuildConfig.SUPABASE_URL
    actual val supabaseKey: String = IOSBuildConfig.SUPABASE_KEY
    actual val cfAccountId: String = IOSBuildConfig.CF_ACCOUNT_ID
    actual val cfPublicUrl: String = IOSBuildConfig.CF_PUBLIC_URL
    actual val cfWorkerUrl: String = IOSBuildConfig.CF_WORKER_URL
    actual val cfUploadToken: String = IOSBuildConfig.CF_UPLOAD_TOKEN
}
