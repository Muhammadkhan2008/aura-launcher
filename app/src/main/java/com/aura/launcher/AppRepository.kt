package com.aura.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast

data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable
)

object AppRepository {

    private const val TAG = "AuraLauncher"

    fun getInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfoList: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)

        val packPkg = AuraPrefs(context).iconPack
        val iconPack = if (packPkg.isNotBlank()) {
            IconPackManager.load(context, packPkg)
        } else null

        return resolveInfoList
            .filter { it.activityInfo.packageName != context.packageName }
            .map { info ->
                val pkg = info.activityInfo.packageName
                val themedIcon = iconPack?.getIcon(pkg, info.activityInfo.name)
                AppInfo(
                    label    = info.loadLabel(pm).toString(),
                    packageName  = pkg,
                    activityName = info.activityInfo.name,
                    icon     = themedIcon ?: info.loadIcon(pm)
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    /**
     * RELIABLE app launch — 3 fallback methods.
     *
     * Method 1: getLaunchIntentForPackage (system recommended way)
     * Method 2: Explicit ComponentName (backup)
     * Method 3: Market/Store open (last resort)
     */
    fun launchApp(context: Context, app: AppInfo) {
        val pm = context.packageManager

        // ── Method 1: System ka official launch intent ─────────────────────
        // Ye sabse safe hai — CATEGORY_LAUNCHER aur sab auto set hota hai
        val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(launchIntent)
                AppUsageTracker.recordOpen(context, app.packageName)
                Log.d(TAG, "Launched (method1): ${app.packageName}")
                return
            } catch (e: Exception) {
                Log.w(TAG, "Method1 failed: ${app.packageName} — ${e.message}")
            }
        }

        // ── Method 2: Explicit component (without CATEGORY_LAUNCHER) ────────
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                component = ComponentName(app.packageName, app.activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            AppUsageTracker.recordOpen(context, app.packageName)
            Log.d(TAG, "Launched (method2): ${app.packageName}")
            return
        } catch (e: Exception) {
            Log.w(TAG, "Method2 failed: ${app.packageName} — ${e.message}")
        }

        // ── Method 3: Play Store se open karo (app reinstall ho sakti) ─────
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=${app.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d(TAG, "Opened store: ${app.packageName}")
            return
        } catch (_: Exception) { }

        // Sab fail
        Toast.makeText(context, "${app.label} nahi khul rahi", Toast.LENGTH_SHORT).show()
        Log.e(TAG, "ALL methods failed: ${app.packageName}")
    }

    fun launchByPackage(context: Context, packageName: String, allApps: List<AppInfo>) {
        // Pehle getLaunchIntentForPackage try karo directly
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(launchIntent)
                AppUsageTracker.recordOpen(context, packageName)
                return
            } catch (_: Exception) { }
        }
        // Fallback: allApps list se dhundho
        val app = allApps.find { it.packageName == packageName }
        if (app != null) launchApp(context, app)
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
