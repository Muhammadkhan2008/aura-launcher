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
    suspend fun ask(apiKey: String, prompt: String): Result = withContext(Dispatchers.IO) {
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

            // Request body banao
            val body = JSONObject().apply {
                put("model", MODEL)
                put("temperature", 0.6)
                put("max_tokens", 512)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put(
                            "content",
                            "You are Aura, a helpful assistant inside an Android launcher. " +
                                "Answer concisely and clearly. Reply in the user's language."
                        )
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
