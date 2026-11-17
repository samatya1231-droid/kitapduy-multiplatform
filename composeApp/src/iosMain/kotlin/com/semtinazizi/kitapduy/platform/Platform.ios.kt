package com.semtinazizi.kitapduy.platform

actual val platformName: String = "iOS"

actual fun createAudioPlayer(): AudioPlayer = IosAudioPlayer()

actual object AppConfig {
    // iOS'ta değerler Info.plist'ten veya hardcoded olarak gelir
    // Codemagic build sırasında environment variables ile doldurulur
    actual val supabaseUrl: String get() = IOSBuildConfig.SUPABASE_URL
    actual val supabaseKey: String get() = IOSBuildConfig.SUPABASE_KEY
    actual val cfAccountId: String get() = IOSBuildConfig.CF_ACCOUNT_ID
    actual val cfPublicUrl: String get() = IOSBuildConfig.CF_PUBLIC_URL
    actual val cfWorkerUrl: String get() = IOSBuildConfig.CF_WORKER_URL
    actual val cfUploadToken: String get() = IOSBuildConfig.CF_UPLOAD_TOKEN
    actual val r2UploadTicketUrl: String get() = IOSBuildConfig.R2_UPLOAD_TICKET_URL
    actual val cfAccessKey: String = "" // iOS'ta ileride eklenebilir
    actual val cfSecretKey: String = ""
    actual val isCiSmokeTest: Boolean get() = IOSBuildConfig.isCiSmokeTest

    actual fun generatePresignedUrl(url: String): String {
        return url // iOS için ileride Swift CommonCrypto ile implement edilecek
    }
}
