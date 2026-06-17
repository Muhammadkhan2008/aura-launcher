package com.aura.launcher

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification

/**
 * NotificationBadgeHelper — pending notifications dekho (kaunse app ka notification wait kar raha hai).
 *
 * Android 7+ (API 24) par NotificationManager.getActiveNotifications() se notifications dekh sakte hain.
 * Pehle user ko permission "post notifications" dena padta hai (Android 13+).
 */
object NotificationBadgeHelper {

    /** Ek package ke kitne pending notifications hain? */
    fun getNotificationCount(context: Context, packageName: String): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return 0

        return runCatching {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val active = nm.activeNotifications
            active.count { it.packageName == packageName }
        }.getOrDefault(0)
    }

    /** Sabhi pending notifications (package -> count map). */
    fun getAllBadges(context: Context): Map<String, Int> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return emptyMap()

        return runCatching {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val active = nm.activeNotifications
            active.groupBy { it.packageName }
                .mapValues { (_, notifs) -> notifs.size }
        }.getOrDefault(emptyMap())
    }
}
