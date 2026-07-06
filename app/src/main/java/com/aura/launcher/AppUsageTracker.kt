package com.aura.launcher

import android.content.Context
import org.json.JSONObject
import java.util.Calendar

/**
 * AppUsageTracker — Aura ka apna ON-DEVICE smart prediction engine.
 *
 * Koi API/internet nahi. Sab kuch phone ke andar (SharedPreferences).
 * Privacy-friendly: data kahin nahi jaata.
 *
 * Logic: har baar app khulti hai to record karte hain:
 *   - kitni baar khuli (frequency)
 *   - kis "time slot" mein khuli (subah/dopahar/shaam/raat)
 *
 * Prediction: abhi ke time slot + total frequency mila ke
 * "is waqt aap ye apps kholte ho" wali list banate hain.
 */
object AppUsageTracker {

    private const val PREFS = "aura_usage"
    private const val KEY_DATA = "usage_data"

    // Din ko 4 slots mein baant te hain
    private fun currentSlot(): Int {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (h) {
            in 5..11 -> 0   // subah
            in 12..16 -> 1  // dopahar
            in 17..20 -> 2  // shaam
            else -> 3       // raat
        }
    }

    private fun load(context: Context): JSONObject {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_DATA, "{}") ?: "{}"
        return runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
    }

    private fun save(context: Context, data: JSONObject) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_DATA, data.toString()).apply()
    }

    /** App khulne pe call hota hai — count badhao. */
    fun recordOpen(context: Context, packageName: String) {
        val data = load(context)
        val slot = currentSlot()
        // structure: { "pkg": { "total": n, "slots": [a,b,c,d] } }
        val appObj = data.optJSONObject(packageName) ?: JSONObject().apply {
            put("total", 0)
            put("slots", org.json.JSONArray(intArrayOf(0, 0, 0, 0)))
        }
        appObj.put("total", appObj.optInt("total") + 1)
        val slots = appObj.optJSONArray("slots") ?: org.json.JSONArray(intArrayOf(0, 0, 0, 0))
        slots.put(slot, slots.optInt(slot) + 1)
        appObj.put("slots", slots)
        data.put(packageName, appObj)
        save(context, data)
    }

    /**
     * Predict: is waqt user kaunsi apps kholta hai (top N).
     * Score = (is slot ki frequency * 2) + total frequency.
     */
    fun getPredictedApps(context: Context, allApps: List<AppInfo>, limit: Int = 6): List<AppInfo> {
        val data = load(context)
        val slot = currentSlot()
        if (data.length() == 0) return emptyList()

        val scores = mutableListOf<Pair<String, Int>>()
        val keys = data.keys()
        while (keys.hasNext()) {
            val pkg = keys.next()
            val obj = data.optJSONObject(pkg) ?: continue
            val total = obj.optInt("total")
            val slots = obj.optJSONArray("slots")
            val slotCount = slots?.optInt(slot) ?: 0
            val score = slotCount * 2 + total
            if (score > 0) scores.add(pkg to score)
        }

        val allAppsMap = allApps.associateBy { it.packageName }

        return scores
            .sortedByDescending { it.second }
            .mapNotNull { (pkg, _) -> allAppsMap[pkg] }
            .take(limit)
    }
}
