package com.aura.launcher

import android.content.Context
import android.content.SharedPreferences

/**
 * SearchHistory — pichli searches ko save karo.
 * Jab user search bar par click kare, history show karo (quick access).
 */
object SearchHistory {
    private const val PREF_NAME = "aura_search_history"
    private const val KEY = "queries"
    private const val MAX_SIZE = 15

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getHistory(context: Context): List<String> {
        val raw = getPrefs(context).getString(KEY, "") ?: ""
        return if (raw.isBlank()) emptyList()
        else raw.split("|").take(MAX_SIZE)
    }

    fun addQuery(context: Context, query: String) {
        if (query.isBlank()) return
        val history = getHistory(context).toMutableList()
        history.remove(query)
        history.add(0, query)
        val saved = history.take(MAX_SIZE).joinToString("|")
        getPrefs(context).edit().putString(KEY, saved).apply()
    }

    fun clear(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
