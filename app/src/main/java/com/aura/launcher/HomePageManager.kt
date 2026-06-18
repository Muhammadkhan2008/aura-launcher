package com.aura.launcher

import android.content.Context

/**
 * HomePageManager — multiple home pages ka data manage karta hai.
 * User swipe left/right kar ke pages switch kar sakta hai.
 * Har page pe different favorite apps aur wallpaper ho sakta hai.
 */
object HomePageManager {

    private const val PREF_NAME = "aura_pages"
    private const val KEY_PAGE_COUNT = "page_count"
    private const val KEY_PAGE_APPS = "page_apps_"
    private const val MAX_PAGES = 5

    fun getPageCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_PAGE_COUNT, 1).coerceIn(1, MAX_PAGES)
    }

    fun setPageCount(context: Context, count: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_PAGE_COUNT, count.coerceIn(1, MAX_PAGES)).apply()
    }

    fun getPageApps(context: Context, page: Int): List<String> {
        val raw = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString("$KEY_PAGE_APPS$page", "") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split("|")
    }

    fun setPageApps(context: Context, page: Int, packages: List<String>) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString("$KEY_PAGE_APPS$page", packages.joinToString("|")).apply()
    }

    fun addAppToPage(context: Context, page: Int, pkg: String) {
        val current = getPageApps(context, page).toMutableList()
        if (!current.contains(pkg)) {
            current.add(pkg)
            setPageApps(context, page, current)
        }
    }

    fun removeAppFromPage(context: Context, page: Int, pkg: String) {
        val current = getPageApps(context, page).toMutableList()
        current.remove(pkg)
        setPageApps(context, page, current)
    }
}
