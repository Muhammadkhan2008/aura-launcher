# Keep Compose and Material3 runtime
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.** { *; }

# Keep JSON parsing (used for Groq API responses)
-keep class org.json.** { *; }

# Keep app's data classes (used in reflection-like patterns)
-keep class com.aura.launcher.AppInfo { *; }
-keep class com.aura.launcher.Weather { *; }
-keep class com.aura.launcher.FileResult { *; }

# Keep EncryptedSharedPreferences
-keep class androidx.security.crypto.** { *; }

# Keep Device Admin receiver
-keep class com.aura.launcher.AuraAdminReceiver { *; }
