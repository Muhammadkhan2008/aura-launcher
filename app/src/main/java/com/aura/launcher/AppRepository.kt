package com.aura.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings

/**
 * AppInfo — ek installed app ki jaankari rakhta hai.
 */
data class AppInfo(
    val label: String,        // app ka naam (jaise "WhatsApp")
    val packageName: String,  // unique id (jaise "com.whatsapp")
    val icon: Drawable        // app ka icon
)

/**
 * AppRepository — phone se saari installed apps nikaalta hai.
 *
 * Yahan koi internet/API nahi chahiye. Ye Android ka apna
 * PackageManager use karta hai jo offline kaam karta hai.
 */
object AppRepository {

    fun getInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager

        // "Jo apps launcher se khul sakti hain" unki list maango
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfoList: List<ResolveInfo> =
            pm.queryIntentActivities(intent, 0)

        return resolveInfoList
            .filter { it.activityInfo.packageName != context.packageName } // khud Aura ko hide
            .map { info ->
                AppInfo(
                    label = info.loadLabel(pm).toString(),
                    packageName = info.activityInfo.packageName,
                    icon = info.loadIcon(pm)
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }  // A-Z sort
    }

    /**
     * Kisi bhi app ko package name se launch karo.
     */
    fun launchApp(context: Context, packageName: String) {
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(packageName)
        launchIntent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        }
    }

    /**
     * App ki "App Info" settings screen kholo (long-press menu se).
     */
    fun openAppInfo(context: Context, packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * App ko uninstall karne ka dialog kholo (long-press menu se).
     */
    fun uninstallApp(context: Context, packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

