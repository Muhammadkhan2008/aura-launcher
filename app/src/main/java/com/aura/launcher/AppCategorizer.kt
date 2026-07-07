package com.aura.launcher

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

/**
 * AppCategorizer — apps ko apne-aap category mein group karta hai.
 *
 * On-device, koi API nahi. Do tareeke use karta hai:
 * 1. Android ka apna app-category (Android 8+ pe apps khud batati hain)
 * 2. Package name / label se keyword matching (fallback)
 */
object AppCategorizer {

    enum class Category(val title: String) {
        SOCIAL("Social"),
        COMMUNICATION("Communication"),
        ENTERTAINMENT("Entertainment"),
        GAMES("Games"),
        PRODUCTIVITY("Productivity"),
        SHOPPING("Shopping"),
        FINANCE("Finance"),
        TOOLS("Tools"),
        PHOTOGRAPHY("Photos & Media"),
        OTHER("Other")
    }

    private val categoryCache = java.util.concurrent.ConcurrentHashMap<String, Category>()

    // Keyword hints — package name mein ye mile to category guess
    private val keywordMap = mapOf(
        Category.SOCIAL to listOf("facebook", "instagram", "twitter", "snapchat", "tiktok", "linkedin", "reddit", "pinterest", "threads"),
        Category.COMMUNICATION to listOf("whatsapp", "telegram", "messenger", "signal", "skype", "zoom", "gmail", "email", "mail", "dialer", "contacts", "sms", "message"),
        Category.ENTERTAINMENT to listOf("youtube", "netflix", "spotify", "prime", "hotstar", "music", "video", "player", "mxplayer", "vlc", "soundcloud"),
        Category.GAMES to listOf("game", "pubg", "ludo", "candy", "clash", "minecraft", "roblox", "freefire"),
        Category.PRODUCTIVITY to listOf("docs", "sheet", "office", "word", "excel", "powerpoint", "notion", "keep", "calendar", "drive", "pdf", "note"),
        Category.SHOPPING to listOf("amazon", "flipkart", "myntra", "shop", "ebay", "aliexpress", "daraz", "meesho"),
        Category.FINANCE to listOf("paytm", "phonepe", "gpay", "googlepay", "bank", "upi", "wallet", "pay", "easypaisa", "jazzcash"),
        Category.PHOTOGRAPHY to listOf("camera", "photo", "gallery", "gallary", "picsart", "snapseed", "lightroom", "canva")
    )

    /** Ek app ki category nikaalo. */
    fun categoryOf(context: Context, app: AppInfo): Category {
        categoryCache[app.packageName]?.let { return it }

        // 1. Android ka apna category (Android 8+)
        runCatching {
            val ai = context.packageManager.getApplicationInfo(app.packageName, 0)
            val sysCat = systemCategory(ai)
            if (sysCat != null) {
                categoryCache[app.packageName] = sysCat
                return sysCat
            }
        }

        // 2. Keyword matching
        val hay = (app.packageName + " " + app.label).lowercase()
        for ((cat, words) in keywordMap) {
            if (words.any { hay.contains(it) }) {
                categoryCache[app.packageName] = cat
                return cat
            }
        }

        categoryCache[app.packageName] = Category.OTHER
        return Category.OTHER
    }

    private fun systemCategory(ai: ApplicationInfo): Category? {
        if (android.os.Build.VERSION.SDK_INT < 26) return null
        return when (ai.category) {
            ApplicationInfo.CATEGORY_SOCIAL -> Category.SOCIAL
            ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_AUDIO -> Category.ENTERTAINMENT
            ApplicationInfo.CATEGORY_GAME -> Category.GAMES
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> Category.PRODUCTIVITY
            ApplicationInfo.CATEGORY_NEWS -> Category.PRODUCTIVITY
            ApplicationInfo.CATEGORY_MAPS -> Category.TOOLS
            ApplicationInfo.CATEGORY_IMAGE -> Category.PHOTOGRAPHY
            else -> null
        }
    }

    /** Saari apps ko categories mein group karke do. */
    fun groupApps(context: Context, apps: List<AppInfo>): Map<Category, List<AppInfo>> {
        return apps.groupBy { categoryOf(context, it) }
            .toSortedMap(compareBy { it.ordinal })
            .filterValues { it.isNotEmpty() }
    }
}
