package com.semtinazizi.kitapduy.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

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
                // Refresh expired persisted sessions on both platforms.
                // A background queue alone does not identify the cause of a crash.
                if (com.semtinazizi.kitapduy.platform.platformName.startsWith("iOS")) {
                    alwaysAutoRefresh = false
                    flowType = io.github.jan.supabase.auth.FlowType.PKCE
                } else {
                    alwaysAutoRefresh = true
                }
                sessionManager = AppSessionManager()
            }
        }

        // iOS'ta alwaysAutoRefresh = false yaptigimiz icin uygulama acilisinda
        // tokenin yenilenmesi gerekiyorsa bunu guvenli bir coroutine icinde manuel yapiyoruz.
        // Boylece background thread unhandled exception yuzunden app crash olmaz.
        if (com.semtinazizi.kitapduy.platform.platformName.startsWith("iOS")) {
            GlobalScope.launch(Dispatchers.Default) {
                try {
                    client.auth.refreshCurrentSession()
                } catch (e: Exception) {
                    println("Supabase manual refresh failed: ${e.message}")
                }
            }
        }
    }

    val client: SupabaseClient
        get() = _client ?: error("SupabaseManager henüz başlatılmadı. initialize() çağır.")

    val auth get() = client.auth
    val postgrest get() = client.postgrest
    val storage get() = client.storage
}
