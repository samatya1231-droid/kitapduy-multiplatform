package com.semtinazizi.kitapduy.platform

actual val platformName: String = "iOS"

actual val isSimulatorCiSmokeTest: Boolean
    get() {
        val env = platform.Foundation.NSProcessInfo.processInfo.environment
        val isCi = env["KITAPDUY_CI_SMOKE_TEST"] == "1"
        val isSimulator = !(env["SIMULATOR_UDID"] as? String).isNullOrBlank()
        return isCi && isSimulator
    }

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
    actual val cfAccessKey: String = "" // iOS'ta ileride eklenebilir
    actual val cfSecretKey: String = ""

    actual fun generatePresignedUrl(url: String): String {
        return url // iOS için ileride Swift CommonCrypto ile implement edilecek
    }
}
