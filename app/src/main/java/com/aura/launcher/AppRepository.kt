package com.aura.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

/**
 * AppInfo — ek installed app ki jaankari rakhta hai.
 *
 * activityName bhi rakhte hain taaki app ko reliably launch kar sakein
 * (sirf packageName se kabhi-kabhi launch fail hota hai = "not responding").
 */
data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,   // exact launcher activity (reliable launch)
    val icon: Drawable
)

/**
 * AppRepository — phone se saari installed apps nikaalta hai.
 * Offline, PackageManager se. Koi API nahi.
 */
object AppRepository {

    fun getInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfoList: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)

        // Agar user ne koi icon pack chuna hai to use load karo (warna null)
        val packPkg = AuraPrefs(context).iconPack
        val iconPack = if (packPkg.isNotBlank()) {
            IconPackManager.load(context, packPkg)
        } else null

        return resolveInfoList
            .filter { it.activityInfo.packageName != context.packageName }
            .map { info ->
                val pkg = info.activityInfo.packageName
                // Icon pack se icon try karo; na mile to app ka apna icon
                val themedIcon = iconPack?.getIcon(pkg, info.activityInfo.name)
                AppInfo(
                    label = info.loadLabel(pm).toString(),
                    packageName = pkg,
                    activityName = info.activityInfo.name,
                    icon = themedIcon ?: info.loadIcon(pm)
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    /**
     * App launch karo — RELIABLE tareeka.
     *
     * Pehle exact activity component se try karo (sabse reliable),
     * agar woh fail ho to getLaunchIntentForPackage se, aur fail hone pe
     * user ko politely batao (silently fail nahi — "not responding" bug fix).
     */
    fun launchApp(context: Context, app: AppInfo) {
        // Try 1: exact component (best)
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(app.packageName, app.activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
            context.startActivity(intent)
            AppUsageTracker.recordOpen(context, app.packageName)
            return
        } catch (_: Exception) { /* fallback below */ }

        // Try 2: package default launch intent
        try {
            val launch = context.packageManager.getLaunchIntentForPackage(app.packageName)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
                AppUsageTracker.recordOpen(context, app.packageName)
                return
            }
        } catch (_: Exception) { /* fallback below */ }

        // Dono fail: user ko batao (silent freeze nahi)
        Toast.makeText(context, "${app.label} khul nahi paayi", Toast.LENGTH_SHORT).show()
    }

    /** Package name se launch (jab sirf package ho, jaise favourites). */
    fun launchByPackage(context: Context, packageName: String, allApps: List<AppInfo>) {
        val app = allApps.find { it.packageName == packageName }
        if (app != null) launchApp(context, app)
        else {
            try {
                val launch = context.packageManager.getLaunchIntentForPackage(packageName)
                launch?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (launch != null) context.startActivity(launch)
            } catch (_: Exception) {
                Toast.makeText(context, "App khul nahi paayi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openAppInfo(context: Context, packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun uninstallApp(context: Context, packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}


