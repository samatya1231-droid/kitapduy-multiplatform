package com.semtinazizi.kitapduy.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
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
            })
            install(Postgrest)
            install(Storage)
            install(Realtime)
            install(Auth) {
                scheme = "kitapduy"
                host = "login-callback"
            }
        }
    }

    val client: SupabaseClient
        get() = _client ?: error("SupabaseManager henüz başlatılmadı. initialize() çağır.")

    val auth get() = client.pluginManager.getPlugin(Auth)
    val postgrest get() = client.pluginManager.getPlugin(Postgrest)
    val storage get() = client.pluginManager.getPlugin(Storage)
}
