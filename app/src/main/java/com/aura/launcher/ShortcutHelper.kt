package com.aura.launcher

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.util.Log

/**
 * ShortcutHelper — app ke dynamic/static shortcuts laata hai.
 * e.g. WhatsApp long-press → "New Message", "Status", "Calls"
 * Sirf tab kaam karta hai jab Aura DEFAULT launcher ho.
 */
object ShortcutHelper {

    data class AuraShortcut(
        val id: String,
        val label: String,
        val icon: Drawable?,
        val info: ShortcutInfo
    )

    fun getShortcuts(context: Context, packageName: String): List<AuraShortcut> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return emptyList()
        return runCatching {
            val la = context.getSystemService(LauncherApps::class.java)
            val query = LauncherApps.ShortcutQuery().apply {
                setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                )
                setPackage(packageName)
            }
            val list = la.getShortcuts(query, Process.myUserHandle()) ?: return emptyList()
            list.take(4).map { info ->
                val icon = runCatching {
                    la.getShortcutIconDrawable(info, context.resources.displayMetrics.densityDpi)
                }.getOrNull()
                AuraShortcut(
                    id    = info.id,
                    label = (info.shortLabel ?: info.longLabel ?: "").toString(),
                    icon  = icon,
                    info  = info
                )
            }
        }.getOrElse {
            Log.w("AuraShortcut", "Could not get shortcuts for $packageName: ${it.message}")
            emptyList()
        }
    }

    fun launch(context: Context, shortcut: AuraShortcut) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        runCatching {
            val la = context.getSystemService(LauncherApps::class.java)
            la.startShortcut(shortcut.info, null, null)
        }.onFailure {
            Log.e("AuraShortcut", "Launch failed: ${it.message}")
        }
    }
}
