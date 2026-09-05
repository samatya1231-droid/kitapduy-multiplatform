package com.semtinazizi.kitapduy.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

/**
 * KMP Supabase Manager
 * Hem Android hem iOS'ta aynı kod çalışır
 * Config değerleri platform katmanından (AppConfig) gelir
 */
object SupabaseManager {
    private var _client: SupabaseClient? = null

    fun initialize(supabaseUrl: String, supabaseKey: String) {
        if (_client != null) return
        _client = createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseKey
        ) {
            defaultSerializer = io.github.jan.supabase.serializer.KotlinXSerializer(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            })
            install(Postgrest)
            install(Storage)
            // Realtime plugin iOS'ta WebSocket bağlantısı açıyor:
            // NSURLSession callback'leri GCD utility-qos kuyruğunda çalışıyor
            // ve exception fırlatınca SIGABRT crash'e neden oluyor.
            // iOS stabilitesi sağlanana kadar Realtime devre dışı.
            // install(Realtime)
            install(Auth) {
                scheme = "kitapduy"
                host = "login-callback"
                // iOS'ta alwaysAutoRefresh = true, GCD utility-qos thread'inde
                // background coroutine başlatıyor ve exception -> SIGABRT crash'e yol açıyor.
                // Token yenileme manuel olarak yapılacak.
                alwaysAutoRefresh = false
                sessionManager = AppSessionManager()
            }
        }
    }

    val client: SupabaseClient
        get() = _client ?: error("SupabaseManager henüz başlatılmadı. initialize() çağır.")

    val auth get() = client.pluginManager.getPlugin(Auth)
    val postgrest get() = client.pluginManager.getPlugin(Postgrest)
    val storage get() = client.pluginManager.getPlugin(Storage)
}
