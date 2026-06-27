package com.aura.launcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * GroqClient — Aura ka AI brain (universal search + commands).
 *
 * IMPORTANT (production safety):
 * - API key APK mein hardcode NAHI hai. User apni FREE key settings
 *   mein daalta hai (AuraPrefs). Isse:
 *     1. Key chori nahi hoti (har user ki apni)
 *     2. App "flop" nahi hota (har user ki apni free limit)
 *     3. Tumhara kharcha ZERO
 * - Key user ke phone se seedhe Groq ko jaati hai (privacy).
 *
 * Network ke liye plain HttpURLConnection use kiya (koi extra library
 * nahi, APK halki rehti hai).
 */
object GroqClient {

    private const val ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
    private const val MODEL = "llama-3.3-70b-versatile"

    sealed class Result {
        data class Success(val text: String) : Result()
        data class Error(val message: String) : Result()
        object NoKey : Result()
    }

    /**
     * AI se ek sawaal pucho. Background thread pe chalta hai (UI hang nahi).
     */
    suspend fun ask(
        apiKey: String,
        prompt: String,
        installedApps: List<AppInfo> = emptyList()
    ): Result = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.NoKey

        try {
            val url = URL(ENDPOINT)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 30000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
            }

            val appsListStr = installedApps.joinToString("\n") { "- ${it.label}: ${it.packageName}" }
            
            val systemInstructions = """
                You are Aura, a smart agentic AI assistant inside the Aura Android launcher.
                You can perform actions on the device by returning a structured JSON response.
                
                You MUST return a valid JSON object matching this structure (and nothing else, no markdown wrapper, no backticks, just the raw JSON):
                {
                  "action": "LAUNCH_APP" | "OPEN_SETTINGS" | "OPEN_NOTIFICATIONS" | "CLOSE_DRAWER" | "SET_GRID" | "LOCK_SCREEN" | "SEARCH_FILES" | "CREATE_NOTE" | "SAY",
                  "param": "string parameter value (e.g. package name, grid column count, file name/search term, or empty)",
                  "reply": "User-facing spoken response explaining your action in the user's language"
                }
                
                Actions guide:
                - To open an app, use "action": "LAUNCH_APP" and set "param" to the exact package name from the app list below.
                - To open launcher settings / device settings, use "action": "OPEN_SETTINGS".
                - To expand notification panel / status bar, use "action": "OPEN_NOTIFICATIONS".
                - To close the app drawer and go back to home screen, use "action": "CLOSE_DRAWER".
                - To change app grid column count (3 to 6), use "action": "SET_GRID" and set "param" to the column digit (e.g. "5").
                - To lock the screen / show lock overlay, use "action": "LOCK_SCREEN".
                - To search for files on the device, use "action": "SEARCH_FILES" and set "param" to the search term (e.g. "cats" or "receipt").
                - To create a note / text file, use "action": "CREATE_NOTE" and set "param" to the note content (e.g. "remember to buy milk").
                - For other normal questions or queries, use "action": "SAY" and set "reply" to your answer.
                
                Here is the list of installed apps on the device:
                ${if (appsListStr.isBlank()) "(No apps loaded)" else appsListStr}
            """.trimIndent()

            // Request body with response_format: json_object
            val body = JSONObject().apply {
                put("model", MODEL)
                put("temperature", 0.2) // Low temperature for higher structure compliance
                put("max_tokens", 512)
                put("response_format", JSONObject().apply { put("type", "json_object") })
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemInstructions)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }

            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val resp = stream.bufferedReader().use(BufferedReader::readText)

            if (code in 200..299) {
                val json = JSONObject(resp)
                val content = json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                Result.Success(content.trim())
            } else {
                val errMsg = runCatching {
                    JSONObject(resp).getJSONObject("error").getString("message")
                }.getOrDefault("AI error (code $code)")
                Result.Error(errMsg)
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
}
