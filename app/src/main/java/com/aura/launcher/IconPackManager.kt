package com.aura.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import org.xmlpull.v1.XmlPullParser

/**
 * IconPackManager — installed icon packs (Play Store se) apply karta hai.
 *
 * Nova/Apex jaisa feature. Icon pack ek normal app hota hai jisme
 * "appfilter.xml" hota hai jo har app ke liye custom icon batata hai.
 * Koi API/internet nahi — sab phone pe installed pack se.
 */
data class IconPack(
    val packageName: String,
    val label: String
)

object IconPackManager {

    // Icon pack apps ye intents declare karte hain
    private val ICON_PACK_ACTIONS = listOf(
        "org.adw.launcher.THEMES",
        "com.gau.go.launcherex.theme",
        "com.novalauncher.THEME"
    )

    /** Phone pe installed saare icon packs dhoondho. */
    fun getInstalledIconPacks(context: Context): List<IconPack> {
        val pm = context.packageManager
        val found = linkedMapOf<String, IconPack>()
        for (action in ICON_PACK_ACTIONS) {
            val intent = Intent(action)
            val list = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            for (info in list) {
                val pkg = info.activityInfo.packageName
                if (!found.containsKey(pkg)) {
                    found[pkg] = IconPack(pkg, info.loadLabel(pm).toString())
                }
            }
        }
        return found.values.toList()
    }

    /**
     * Icon pack ek baar load karo (appfilter.xml parse karke map banao).
     * Isse har app ke liye XML dobara parse nahi hota — fast.
     * @return LoadedIconPack ya null (load fail ho to)
     */
    fun load(context: Context, iconPackPackage: String): LoadedIconPack? {
        return runCatching {
            val pm = context.packageManager
            val res = pm.getResourcesForApplication(iconPackPackage)
            val parserId = res.getIdentifier("appfilter", "xml", iconPackPackage)
                .takeIf { it != 0 } ?: return null

            val map = HashMap<String, String>()
            val parser = res.getXml(parserId)
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    val comp = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")
                    if (comp != null && drawable != null) map[comp] = drawable
                }
                parser.next()
            }
            LoadedIconPack(res, iconPackPackage, map)
        }.getOrNull()
    }
}

/**
 * LoadedIconPack — ek baar parse kiya hua icon pack. Har app ka icon
 * fast deta hai (map se lookup).
 */
class LoadedIconPack(
    private val res: android.content.res.Resources,
    private val packPackage: String,
    private val componentToDrawable: Map<String, String>
) {
    fun getIcon(appPackage: String, appActivity: String): Drawable? {
        val component = "ComponentInfo{$appPackage/$appActivity}"
        val drawableName = componentToDrawable[component] ?: return null
        val id = res.getIdentifier(drawableName, "drawable", packPackage)
        if (id == 0) return null
        return runCatching {
            @Suppress("DEPRECATION")
            res.getDrawable(id)
        }.getOrNull()
    }
}
