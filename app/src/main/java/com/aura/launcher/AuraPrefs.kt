package com.aura.launcher

import android.content.Context
import android.content.SharedPreferences

/**
 * AuraPrefs — settings aur favourites phone mein save rakhta hai.
 *
 * SharedPreferences use karta hai — offline, koi API nahi.
 * - Favourites (dock ke apps)
 * - Grid columns (kitne columns drawer mein)
 */
class AuraPrefs(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("aura_settings", Context.MODE_PRIVATE)

    // ---- App drawer ke columns (3 se 6) ----
    var gridColumns: Int
        get() = prefs.getInt(KEY_COLUMNS, 4)
        set(value) = prefs.edit().putInt(KEY_COLUMNS, value.coerceIn(3, 6)).apply()

    // ---- Dock ke favourite apps (package names) ----
    fun getFavorites(): List<String> {
        val raw = prefs.getString(KEY_FAVORITES, "") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split("|")
    }

    fun addFavorite(packageName: String) {
        val current = getFavorites().toMutableList()
        if (!current.contains(packageName) && current.size < 5) {
            current.add(packageName)
            saveFavorites(current)
        }
    }

    fun removeFavorite(packageName: String) {
        val current = getFavorites().toMutableList()
        current.remove(packageName)
        saveFavorites(current)
    }

    fun isFavorite(packageName: String): Boolean =
        getFavorites().contains(packageName)

    private fun saveFavorites(list: List<String>) {
        prefs.edit().putString(KEY_FAVORITES, list.joinToString("|")).apply()
    }

    // ---- Groq AI key (user apni daalega; APK mein hardcode NAHI) ----
    var groqApiKey: String
        get() = prefs.getString(KEY_GROQ, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GROQ, value.trim()).apply()

    fun hasAiKey(): Boolean = groqApiKey.isNotBlank()

    // ---- Smart prediction on/off ----
    var smartPredictionEnabled: Boolean
        get() = prefs.getBoolean(KEY_PREDICT, true)
        set(value) = prefs.edit().putBoolean(KEY_PREDICT, value).apply()

    companion object {
        private const val KEY_COLUMNS = "grid_columns"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_GROQ = "groq_api_key"
        private const val KEY_PREDICT = "smart_prediction"
    }
}
