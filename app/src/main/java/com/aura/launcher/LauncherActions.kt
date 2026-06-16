package com.aura.launcher

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/**
 * LauncherActions — Aura ki "launcher kabza" powers.
 *
 * Ye wo cheezein hain jo ek asli launcher control karta hai:
 * - Default launcher banna (RoleManager / settings)
 * - Notification panel kholna (swipe down gesture)
 * - System settings tak pahunch
 *
 * NOTE: 3 hardware buttons (back/home/recents) ko koi launcher
 * reprogram nahi kar sakta — wo Android SystemUI ke control mein hain.
 * Yahan wahi sab hai jo legally/technically possible hai.
 */
object LauncherActions {

    /**
     * Check: kya Aura abhi default launcher hai?
     */
    fun isDefaultLauncher(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val res = context.packageManager.resolveActivity(intent, 0)
        return res?.activityInfo?.packageName == context.packageName
    }

    /**
     * Aura ko default launcher banane ka system prompt kholo.
     * Android 10+ pe RoleManager se seedha "default home" dialog aata hai.
     * Purane Android pe home-settings screen khulti hai.
     */
    fun requestSetDefault(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)
                && !rm.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
                val intent = rm.createRequestRoleIntent(RoleManager.ROLE_HOME)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            }
        }
        // Fallback: home settings screen kholo
        val intent = Intent(Settings.ACTION_HOME_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure {
                // Aakhri fallback: general settings
                context.startActivity(
                    Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
    }

    /**
     * Notification panel kholo (swipe-down gesture ke liye).
     * Reflection use hota hai kyunki ye official API nahi hai par
     * har Android pe kaam karta hai.
     */
    @Suppress("WrongConstant")
    fun openNotifications(context: Context) {
        runCatching {
            val sbService = context.getSystemService("statusbar")
            val sbManager = Class.forName("android.app.StatusBarManager")
            val method = sbManager.getMethod("expandNotificationsPanel")
            method.invoke(sbService)
        }
    }

    /**
     * App search / settings kholo.
     */
    fun openSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
