package com.aura.launcher

import android.content.Context

/**
 * ThemeManager — Light/Dark mode switch karne ke liye.
 * SharedPreferences mein save hota hai, UI ko re-compose karega.
 */
object ThemeManager {

    private const val PREF_NAME = "aura_theme"
    private const val KEY_DARK_MODE = "dark_mode"

    fun isDarkMode(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DARK_MODE, true)  // Default = dark
    }

    fun setDarkMode(context: Context, isDark: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_DARK_MODE, isDark).apply()
    }

    fun toggleTheme(context: Context) {
        setDarkMode(context, !isDarkMode(context))
    }
}
