package com.aura.launcher

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings

/**
 * RecentApps — Aura ka apna "recent apps" (UsageStats se).
 *
 * System recents button launcher control nahi kar sakta, isliye
 * Aura apna recent-apps list banata hai jo last use ki apps dikhata hai.
 *
 * Iske liye user ko ek baar "Usage Access" permission deni hoti hai
 * (settings se), bilkul jaise digital-wellbeing apps maangti hain.
 */
object RecentApps {

    /** Check: usage access permission mili hai? */
    fun hasUsagePermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Usage access settings screen kholo (permission maangne ke liye). */
    fun requestUsagePermission(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /**
     * Last 24 ghante mein use ki gayi apps, recent-first order mein.
     */
    fun getRecentApps(context: Context, allApps: List<AppInfo>): List<AppInfo> {
        if (!hasUsagePermission(context)) return emptyList()

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val dayAgo = now - 24 * 60 * 60 * 1000

        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, dayAgo, now
        ) ?: return emptyList()

        // Package -> last used time
        val lastUsed = stats
            .filter { it.lastTimeUsed > 0 }
            .groupBy { it.packageName }
            .mapValues { entry -> entry.value.maxOf { it.lastTimeUsed } }

        return lastUsed.entries
            .sortedByDescending { it.value }
            .mapNotNull { e -> allApps.find { it.packageName == e.key } }
            .filter { it.packageName != context.packageName }
            .take(8)
    }
}
