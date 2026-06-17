package com.aura.launcher

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * WeatherHelper — location-based weather laata hai.
 *
 * Open-Meteo API use karta hai — BILKUL FREE, koi API key NAHI chahiye.
 * (https://open-meteo.com). Tumhare zero-budget ke liye perfect.
 *
 * Location ACCESS_COARSE se aati hai (approx, battery friendly).
 */
data class Weather(
    val tempC: Int,
    val code: Int,
    val description: String,
    val emoji: String
)

object WeatherHelper {

    /** Location permission mili hai? */
    fun hasLocationPermission(context: Context): Boolean =
        context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    /** Phone ki last-known location (lat, lon). Null = nahi mili. */
    @SuppressLint("MissingPermission")
    private fun getLocation(context: Context): Pair<Double, Double>? {
        if (!hasLocationPermission(context)) return null
        return runCatching {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = lm.getProviders(true)
            var best: android.location.Location? = null
            for (p in providers) {
                val loc = lm.getLastKnownLocation(p) ?: continue
                if (best == null || loc.accuracy < best!!.accuracy) best = loc
            }
            best?.let { it.latitude to it.longitude }
        }.getOrNull()
    }

    /**
     * Current weather laao (background thread pe).
     * @return Weather ya null (permission/location/network fail).
     */
    suspend fun getWeather(context: Context): Weather? = withContext(Dispatchers.IO) {
        val loc = getLocation(context) ?: return@withContext null
        val (lat, lon) = loc

        runCatching {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$lat&longitude=$lon&current_weather=true"
            )
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 15000
            }
            val code = conn.responseCode
            if (code !in 200..299) return@withContext null

            val resp = conn.inputStream.bufferedReader().use { it.readText() }
            val cw = JSONObject(resp).getJSONObject("current_weather")
            val temp = cw.getDouble("temperature").toInt()
            val wcode = cw.getInt("weathercode")
            Weather(temp, wcode, codeToDesc(wcode), codeToEmoji(wcode))
        }.getOrNull()
    }

    // WMO weather codes -> text/emoji (Open-Meteo standard)
    private fun codeToDesc(c: Int): String = when (c) {
        0 -> "Clear"
        1, 2, 3 -> "Partly cloudy"
        45, 48 -> "Foggy"
        in 51..57 -> "Drizzle"
        in 61..67 -> "Rainy"
        in 71..77 -> "Snowy"
        in 80..82 -> "Rain showers"
        in 95..99 -> "Thunderstorm"
        else -> "—"
    }

    private fun codeToEmoji(c: Int): String = when (c) {
        0 -> "☀️"
        1, 2, 3 -> "⛅"
        45, 48 -> "🌫️"
        in 51..67 -> "🌧️"
        in 71..77 -> "❄️"
        in 80..82 -> "🌦️"
        in 95..99 -> "⛈️"
        else -> "🌡️"
    }
}
