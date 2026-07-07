package com.aura.launcher

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
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
    private suspend fun getLocation(context: Context): Pair<Double, Double>? = withContext(Dispatchers.Main) {
        if (!hasLocationPermission(context)) return@withContext null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return@withContext null
        
        // 1. Fast path: last known location
        val lastLoc = runCatching {
            val providers = lm.getProviders(true)
            var best: android.location.Location? = null
            for (p in providers) {
                val loc = lm.getLastKnownLocation(p) ?: continue
                if (best == null || loc.accuracy < best.accuracy) best = loc
            }
            best?.let { it.latitude to it.longitude }
        }.getOrNull()
        
        if (lastLoc != null) return@withContext lastLoc
        
        // 2. Slow path: Request single location update
        kotlinx.coroutines.suspendCancellableCoroutine<Pair<Double, Double>?> { continuation ->
            val provider = when {
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                else -> null
            }
            if (provider == null) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }
            
            val listener = object : android.location.LocationListener {
                override fun onLocationChanged(location: android.location.Location) {
                    lm.removeUpdates(this)
                    if (continuation.isActive) {
                        continuation.resume(location.latitude to location.longitude)
                    }
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            
            try {
                lm.requestLocationUpdates(provider, 0L, 0f, listener, android.os.Looper.getMainLooper())
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
            
            continuation.invokeOnCancellation {
                runCatching { lm.removeUpdates(listener) }
            }
        }
    }

    /**
     * Current weather laao (background thread pe).
     * @return Weather ya null (permission/location/network fail).
     */
    suspend fun getWeather(context: Context): Weather? = withContext(Dispatchers.IO) {
        val loc = kotlinx.coroutines.withTimeoutOrNull(8000L) {
            getLocation(context)
        } ?: return@withContext getCachedWeather(context)

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
            if (code !in 200..299) return@withContext getCachedWeather(context)

            val resp = conn.inputStream.bufferedReader().use { it.readText() }
            val cw = JSONObject(resp).getJSONObject("current_weather")
            val temp = cw.getDouble("temperature").toInt()
            val wcode = cw.getInt("weathercode")
            val w = Weather(temp, wcode, codeToDesc(wcode), codeToEmoji(wcode))
            
            // Save to cache
            saveWeatherToCache(context, w)
            w
        }.getOrElse { getCachedWeather(context) }
    }

    private fun getCachedWeather(context: Context): Weather? {
        val prefs = AuraPrefs(context)
        val temp = prefs.lastWeatherTemp
        if (temp == 999) return null
        return Weather(
            tempC = temp,
            code = prefs.lastWeatherCode,
            description = prefs.lastWeatherDesc,
            emoji = prefs.lastWeatherEmoji
        )
    }

    private fun saveWeatherToCache(context: Context, w: Weather) {
        val prefs = AuraPrefs(context)
        prefs.lastWeatherTemp = w.tempC
        prefs.lastWeatherCode = w.code
        prefs.lastWeatherDesc = w.description
        prefs.lastWeatherEmoji = w.emoji
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

    internal fun codeToEmoji(c: Int): String = when (c) {
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
