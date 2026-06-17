package com.aura.launcher

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat

/**
 * VoiceSearch — mic se bola hua sun ke text mein badalta hai.
 *
 * Android ka apna SpeechRecognizer use karta hai (Google built-in).
 * Koi API key / paisa nahi — free. Internet on ho to behtar accuracy.
 *
 * Use:
 *  - "Open WhatsApp" jaisa bolo → app khulegi
 *  - Aur kuch bolo → wo text search box mein chala jayega
 */
class VoiceSearch(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null

    /** Mic permission mili hai ya nahi. */
    fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    /** Device pe voice recognition available hai ya nahi. */
    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    /**
     * Sunna shuru karo.
     * @param onResult  jo suna wo text (lowercase)
     * @param onError   koi dikkat (mic off, kuch suna nahi, etc.)
     * @param onReady   mic ready ho gaya (UI mein "Listening..." dikhane ke liye)
     */
    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onReady: () -> Unit = {}
    ) {
        if (!isAvailable()) {
            onError("Voice recognition is phone pe available nahi")
            return
        }
        if (!hasMicPermission()) {
            onError("Mic permission chahiye")
            return
        }

        // Purana recognizer band karo (memory leak na ho)
        stop()

        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = onReady()
                override fun onResults(results: Bundle?) {
                    val list = results?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )
                    val text = list?.firstOrNull()?.trim()?.lowercase()
                    if (text.isNullOrBlank()) onError("Kuch suna nahi")
                    else onResult(text)
                }
                override fun onError(error: Int) {
                    onError(errorText(error))
                }
                // baaki callbacks ki zarurat nahi
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        recognizer?.startListening(intent)
    }

    fun stop() {
        recognizer?.destroy()
        recognizer = null
    }

    private fun errorText(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH -> "Samajh nahi aaya, dobara bolo"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Kuch suna nahi"
        SpeechRecognizer.ERROR_NETWORK -> "Network issue"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mic permission chahiye"
        else -> "Voice error ($code)"
    }

    companion object {
        // App list ko yahan rakhte hain taaki voice command app khol sake
        @Volatile
        private var cachedApps: List<AppInfo> = emptyList()

        fun setApps(apps: List<AppInfo>) { cachedApps = apps }

        /**
         * UI se seedha call hone wala simple helper.
         * Mic se sunta hai, result text onResult ko deta hai.
         */
        fun startListening(context: Context, onResult: (String) -> Unit) {
            val vs = VoiceSearch(context)
            if (!vs.hasMicPermission()) {
                android.widget.Toast.makeText(
                    context, "Mic permission do: Settings → Apps → Aura → Permissions",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                return
            }
            android.widget.Toast.makeText(context, "🎤 Suniye... boliye", android.widget.Toast.LENGTH_SHORT).show()
            vs.startListening(
                onResult = { text ->
                    onResult(text)
                    vs.stop()
                },
                onError = { msg ->
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                    vs.stop()
                }
            )
        }

        /** Bola hua text se app khol do (agar match mile). */
        fun tryOpenApp(context: Context, spokenText: String): Boolean =
            handleVoiceCommand(context, spokenText, cachedApps)

        /**
         * Bola hua text se command nikaalo aur app launch karo.
         * @return true agar app khul gayi
         */
        fun handleVoiceCommand(
            context: Context,
            spokenText: String,
            apps: List<AppInfo>
        ): Boolean {
            val cleaned = spokenText
                .replace(Regex("\\b(open|launch|start|kholo|khol|chalu|karo|app)\\b"), "")
                .trim()

            if (cleaned.isBlank()) return false

            val match = apps.firstOrNull { it.label.equals(cleaned, ignoreCase = true) }
                ?: apps.firstOrNull { it.label.contains(cleaned, ignoreCase = true) }
                ?: apps.firstOrNull { cleaned.contains(it.label, ignoreCase = true) }

            return if (match != null) {
                AppRepository.launchApp(context, match)
                true
            } else false
        }
    }
}
