package com.aura.launcher

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/**
 * BackupManager — Aura ka pura setup ek JSON file mein save/restore.
 *
 * Phone badle, app reinstall ho — favourites, columns, settings sab wapas.
 * User file picker se jagah chunega (Storage Access Framework — koi
 * "all files" permission nahi chahiye, safe).
 *
 * AI key backup mein NAHI daalte (security — key personal hai).
 */
object BackupManager {

    /** Current setup ko JSON string mein badlo. */
    fun exportToJson(context: Context): String {
        val prefs = AuraPrefs(context)
        val root = JSONObject().apply {
            put("version", 1)
            put("gridColumns", prefs.gridColumns)
            put("smartPrediction", prefs.smartPredictionEnabled)
            put("showCategories", prefs.showCategoryView)
            put("favorites", JSONArray(prefs.getFavorites()))
            // NOTE: groqApiKey jaan-bujh ke skip (security)
        }
        return root.toString(2)
    }

    /** JSON se setup wapas restore karo. @return true agar success. */
    fun importFromJson(context: Context, json: String): Boolean {
        return runCatching {
            val root = JSONObject(json)
            val prefs = AuraPrefs(context)

            prefs.gridColumns = root.optInt("gridColumns", 4)
            prefs.smartPredictionEnabled = root.optBoolean("smartPrediction", true)
            prefs.showCategoryView = root.optBoolean("showCategories", false)

            val favs = root.optJSONArray("favorites")
            if (favs != null) {
                // purane favourites hata ke naye daalo
                prefs.getFavorites().forEach { prefs.removeFavorite(it) }
                for (i in 0 until favs.length()) {
                    prefs.addFavorite(favs.getString(i))
                }
            }
            true
        }.getOrDefault(false)
    }

    /** Backup file ko chuni hui Uri pe likho. */
    fun exportTo(context: Context, uri: Uri): Boolean {
        return runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(exportToJson(context).toByteArray())
            }
            true
        }.getOrDefault(false)
    }

    /** Chuni hui Uri se backup padho aur restore karo. */
    fun importFrom(context: Context, uri: Uri): Boolean {
        return runCatching {
            val json = context.contentResolver.openInputStream(uri)?.use { inp ->
                inp.bufferedReader().readText()
            } ?: return false
            importFromJson(context, json)
        }.getOrDefault(false)
    }
}
