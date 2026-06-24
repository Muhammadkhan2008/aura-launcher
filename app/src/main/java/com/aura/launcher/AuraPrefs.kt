package com.aura.launcher

import android.content.Context
import android.content.SharedPreferences

/**
 * AuraPrefs — settings aur favourites phone mein save rakhta hai.
 *
 * SharedPreferences use karta hai — offline, koi API nahi.
 * - Favourites (dock ke apps)
 * - Grid columns + home pages
 * - Hidden apps
 * - Gesture customization
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

    fun isFavorite(packageName: String): Boolean = getFavorites().contains(packageName)

    private fun saveFavorites(list: List<String>) {
        prefs.edit().putString(KEY_FAVORITES, list.joinToString("|")).apply()
    }

    // ---- Groq AI key ----
    var groqApiKey: String
        get() = prefs.getString(KEY_GROQ, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GROQ, value.trim()).apply()

    fun hasAiKey(): Boolean = groqApiKey.isNotBlank()

    // ---- Smart prediction ----
    var smartPredictionEnabled: Boolean
        get() = prefs.getBoolean(KEY_PREDICT, true)
        set(value) = prefs.edit().putBoolean(KEY_PREDICT, value).apply()

    // ---- Icon pack ----
    var iconPack: String
        get() = prefs.getString(KEY_ICONPACK, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ICONPACK, value).apply()

    // ---- Category view ----
    var showCategoryView: Boolean
        get() = prefs.getBoolean(KEY_CATEGORY_VIEW, false)
        set(value) = prefs.edit().putBoolean(KEY_CATEGORY_VIEW, value).apply()

    // ---- Hidden apps ---- (chhupaye gaye apps drawer mein nahi dikhte)
    fun getHiddenApps(): Set<String> {
        val raw = prefs.getString(KEY_HIDDEN, "") ?: ""
        return if (raw.isBlank()) emptySet() else raw.split("|").toSet()
    }

    fun hideApp(pkg: String) {
        val set = getHiddenApps().toMutableSet().also { it.add(pkg) }
        prefs.edit().putString(KEY_HIDDEN, set.joinToString("|")).apply()
    }

    fun showApp(pkg: String) {
        val set = getHiddenApps().toMutableSet().also { it.remove(pkg) }
        prefs.edit().putString(KEY_HIDDEN, set.joinToString("|")).apply()
    }

    fun isHidden(pkg: String): Boolean = pkg in getHiddenApps()

    // ---- Gesture actions ----
    // Values: "NOTIFICATIONS" | "LOCK_SCREEN" | "OPEN_DRAWER" | "NOTHING"
    var swipeDownAction: String
        get() = prefs.getString(KEY_SWIPE_DOWN, "NOTIFICATIONS") ?: "NOTIFICATIONS"
        set(v) = prefs.edit().putString(KEY_SWIPE_DOWN, v).apply()

    var swipeUpAction: String
        get() = prefs.getString(KEY_SWIPE_UP, "OPEN_DRAWER") ?: "OPEN_DRAWER"
        set(v) = prefs.edit().putString(KEY_SWIPE_UP, v).apply()

    var doubleTapAction: String
        get() = prefs.getString(KEY_DOUBLE_TAP, "LOCK_SCREEN") ?: "LOCK_SCREEN"
        set(v) = prefs.edit().putString(KEY_DOUBLE_TAP, v).apply()

    var isOnboarded: Boolean
        get() = prefs.getBoolean("is_onboarded", false)
        set(value) = prefs.edit().putBoolean("is_onboarded", value).apply()

    var lastWeatherTemp: Int
        get() = prefs.getInt("last_weather_temp", 999)
        set(v) = prefs.edit().putInt("last_weather_temp", v).apply()

    var lastWeatherCode: Int
        get() = prefs.getInt("last_weather_code", -1)
        set(v) = prefs.edit().putInt("last_weather_code", v).apply()

    var lastWeatherDesc: String
        get() = prefs.getString("last_weather_desc", "") ?: ""
        set(v) = prefs.edit().putString("last_weather_desc", v).apply()

    var lastWeatherEmoji: String
        get() = prefs.getString("last_weather_emoji", "") ?: ""
        set(v) = prefs.edit().putString("last_weather_emoji", v).apply()

    companion object {
        private const val KEY_COLUMNS       = "grid_columns"
        private const val KEY_FAVORITES     = "favorites"
        private const val KEY_GROQ          = "groq_api_key"
        private const val KEY_PREDICT       = "smart_prediction"
        private const val KEY_ICONPACK      = "icon_pack"
        private const val KEY_CATEGORY_VIEW = "show_categories"
        private const val KEY_HIDDEN        = "hidden_apps"
        private const val KEY_SWIPE_DOWN    = "gesture_swipe_down"
        private const val KEY_SWIPE_UP      = "gesture_swipe_up"
        private const val KEY_DOUBLE_TAP    = "gesture_double_tap"
    }
}
