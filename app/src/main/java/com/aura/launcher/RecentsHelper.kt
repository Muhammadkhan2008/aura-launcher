package com.aura.launcher

import android.app.ActivityManager
import android.content.Context
import android.graphics.drawable.Drawable

/**
 * RecentsHelper — running apps (multitasking panel). 
 * ActivityManager se recent tasks kheench ke dikhao (system recents jaisa).
 */
object RecentsHelper {
    data class RecentTask(
        val title: String,
        val packageName: String,
        val icon: Drawable?
    )

    fun getRunningTasks(context: Context, max: Int = 8): List<RecentTask> {
        return runCatching {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            val tasks = am.getRunningTasks(max)
            tasks.mapNotNull { task ->
                val pkg = task.baseActivity?.packageName ?: return@mapNotNull null
                if (pkg == context.packageName) return@mapNotNull null
                val pm = context.packageManager
                val info = runCatching {
                    pm.getApplicationInfo(pkg, 0)
                }.getOrNull() ?: return@mapNotNull null
                RecentTask(
                    title = pm.getApplicationLabel(info).toString(),
                    packageName = pkg,
                    icon = pm.getApplicationIcon(pkg)
                )
            }
        }.getOrDefault(emptyList())
    }
}
