package com.semtinazizi.kitapduy.util

import androidx.compose.ui.graphics.Color
import com.russhwolf.settings.Settings
import com.semtinazizi.kitapduy.ui.theme.*

object KmpThemeManager {
    private const val KEY_THEME = "active_theme"
    private const val KEY_DARK_MODE = "is_dark_mode"
    private val settings: Settings by lazy { Settings() }

    fun applyTheme() {
        val theme = settings.getString(KEY_THEME, "Sleek")
        setTheme(theme)
    }

    fun setTheme(theme: String) {
        when (theme) {
            "Gece Mavisi" -> applyNightBlue()
            "Kahve Rengi" -> applyCoffee()
            "Orman Yeşili" -> applyForest()
            "Aura" -> applyAura()
            "Aurora" -> applyAurora()
            else -> applySleekDefault()
        }
        settings.putString(KEY_THEME, theme)
    }

    fun getActiveTheme(): String {
        return settings.getString(KEY_THEME, "Sleek")
    }

    private val darkMode by lazy { androidx.compose.runtime.mutableStateOf(settings.getBoolean(KEY_DARK_MODE, false)) }

    fun setDarkMode(isDark: Boolean) {
        darkMode.value = isDark
        settings.putBoolean(KEY_DARK_MODE, isDark)
    }

    // Match Android's default light appearance; saved user choices take priority.
    fun isDarkMode(systemDefault: Boolean): Boolean = darkMode.value

    // Used for initial states in Settings
    fun hasCustomDarkModeSetting(): Boolean {
        return settings.hasKey(KEY_DARK_MODE)
    }

    fun clearDarkModeSetting() {
        settings.remove(KEY_DARK_MODE)
        darkMode.value = false
    }

    private fun applySleekDefault() {
        val bg = Color(0xFF0F0F11)
        val surface = Color(0xFF1C1C1E)
        val primary = Color(0xFFE2B639)
        updateColors(bg, surface, primary)
    }

    private fun applyNightBlue() {
        val bg = Color(0xFF070B19) // Çok koyu gece mavisi
        val surface = Color(0xFF111827) // Maviye çalan yüzey
        val primary = Color(0xFF3B82F6) // Canlı mavi
        updateColors(bg, surface, primary)
    }

    private fun applyCoffee() {
        val bg = Color(0xFF1C120C) // Koyu kahve arka plan
        val surface = Color(0xFF2C1C14) // Kahve yüzey
        val primary = Color(0xFFD4A373) // Latte köpüğü rengi
        updateColors(bg, surface, primary)
    }

    private fun applyForest() {
        val bg = Color(0xFF0D1C12)
        val surface = Color(0xFF142C1B)
        val primary = Color(0xFF4ADE80)
        updateColors(bg, surface, primary)
    }

    private fun applyAura() {
        val bg = Color(0xFF12071A) // Çok koyu mor
        val surface = Color(0xFF1E0F2E) // Mor yüzey
        val primary = Color(0xFFE040FB) // Canlı mor/pembe
        updateColors(bg, surface, primary)
    }

    private fun applyAurora() {
        val bg = Color(0xFF071210) // Koyu zümrüt
        val surface = Color(0xFF0F2420) // Koyu yeşil yüzey
        val primary = Color(0xFF00E676) // Neon yeşil
        updateColors(bg, surface, primary)
    }

    private fun updateColors(bg: Color, surface: Color, primary: Color) {
        BrandDarkBackground = bg
        SleekDarkBackground = bg
        DeepNight = bg
        PremiumDeepBlue = bg

        BrandCardSurface = surface
        SleekSurface = surface
        PremiumSurface = surface

        BrandGold = primary
        SleekChampagne = primary
        SoftGold = primary
        PremiumGold = primary
        PremiumAccent = primary
    }
}
