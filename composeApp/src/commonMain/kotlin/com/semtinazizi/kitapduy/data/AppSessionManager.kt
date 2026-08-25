package com.semtinazizi.kitapduy.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AppSessionManager(private val settings: Settings = Settings()) : SessionManager {
    
    companion object {
        const val SESSION_KEY = "supabase_session"
    }

    override suspend fun saveSession(session: UserSession) {
        settings[SESSION_KEY] = Json.encodeToString(session)
    }

    override suspend fun loadSession(): UserSession? {
        val sessionStr = settings.getStringOrNull(SESSION_KEY) ?: return null
        return try {
            Json.decodeFromString<UserSession>(sessionStr)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun deleteSession() {
        settings.remove(SESSION_KEY)
    }
}
