package com.aura.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * SettingsPanel — settings dialog.
 *
 * - App drawer columns (3-6)
 * - Launcher "kabza" powers: default banao, recent apps permission,
 *   double-tap-to-lock permission.
 * Sab offline SharedPreferences / system settings se.
 */
@Composable
fun SettingsPanel(
    prefs: AuraPrefs,
    onClose: () -> Unit,
    onChanged: () -> Unit,
    onBackup: () -> Unit = {},
    onRestore: () -> Unit = {}
) {
    val context = LocalContext.current
    var columns by remember { mutableStateOf(prefs.gridColumns) }
    var apiKey by remember { mutableStateOf(prefs.groqApiKey) }
    var predictOn by remember { mutableStateOf(prefs.smartPredictionEnabled) }
    var iconPack by remember { mutableStateOf(prefs.iconPack) }

    val isDefault = remember { LauncherActions.isDefaultLauncher(context) }
    val hasUsage = remember { RecentApps.hasUsagePermission(context) }
    val hasAdmin = remember { LockHelper.isAdminActive(context) }

    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = Color(0xFF1B1730)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Aura Settings",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(20.dp))

                Text("App drawer columns: $columns", color = Color.White, fontSize = 15.sp)
                Slider(
                    value = columns.toFloat(),
                    onValueChange = { columns = it.toInt() },
                    valueRange = 3f..6f,
                    steps = 2
                )

                Spacer(Modifier.height(8.dp))
                Divider(color = Color.White.copy(alpha = 0.15f))
                Spacer(Modifier.height(12.dp))

                Text(
                    "Launcher Powers",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(8.dp))

                // Default launcher
                PowerRow(
                    title = "Default launcher",
                    status = if (isDefault) "Active ✓" else "Set Aura as default",
                    done = isDefault,
                    onClick = { LauncherActions.requestSetDefault(context) }
                )
                // Recent apps (UsageStats)
                PowerRow(
                    title = "Recent apps",
                    status = if (hasUsage) "Allowed ✓" else "Allow usage access",
                    done = hasUsage,
                    onClick = { RecentApps.requestUsagePermission(context) }
                )
                // Double-tap to lock (Device Admin)
                PowerRow(
                    title = "Double-tap to lock",
                    status = if (hasAdmin) "Enabled ✓" else "Enable lock permission",
                    done = hasAdmin,
                    onClick = { LockHelper.requestAdmin(context) }
                )

                Spacer(Modifier.height(8.dp))
                Divider(color = Color.White.copy(alpha = 0.15f))
                Spacer(Modifier.height(12.dp))

                // ---- AI Assistant section ----
                Text(
                    "AI Assistant (Groq)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    "Apni FREE key console.groq.com se banao aur yahan paste karo. " +
                        "Key sirf is phone mein save hoti hai, kahin nahi jaati.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    placeholder = { Text("gsk_...", color = Color.White.copy(alpha = 0.5f)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF9D86FF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f)
                    )
                )

                Spacer(Modifier.height(12.dp))
                // Smart prediction toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Smart app prediction", color = Color.White, fontSize = 14.sp)
                        Text(
                            "Time/habit se apps suggest kare (on-device)",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                    Switch(checked = predictOn, onCheckedChange = { predictOn = it })
                }

                Spacer(Modifier.height(8.dp))
                Divider(color = Color.White.copy(alpha = 0.15f))
                Spacer(Modifier.height(12.dp))

                // ---- Icon Pack section ----
                Text(
                    "Icon Pack",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                val iconPacks = remember { IconPackManager.getInstalledIconPacks(context) }
                if (iconPacks.isEmpty()) {
                    Text(
                        "Koi icon pack install nahi. Play Store se koi pack (jaise " +
                            "\"Whicons\", \"Delta\") install karo, phir yahan chunega.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                } else {
                    Spacer(Modifier.height(6.dp))
                    // "Default" option + har installed pack
                    IconPackOption(
                        label = "Default (system icons)",
                        selected = iconPack.isBlank(),
                        onClick = { iconPack = "" }
                    )
                    iconPacks.forEach { pack ->
                        IconPackOption(
                            label = pack.label,
                            selected = iconPack == pack.packageName,
                            onClick = { iconPack = pack.packageName }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Divider(color = Color.White.copy(alpha = 0.15f))
                Spacer(Modifier.height(12.dp))

                // ---- Wallpaper section (Aura ke apne premium wallpapers) ----
                Text(
                    "Wallpaper",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    "Aura ke premium wallpaper. Tap karke set karo.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    WallpaperHelper.WALLPAPERS.forEach { wp ->
                        WallpaperSwatch(
                            wallpaper = wp,
                            onClick = {
                                val ok = WallpaperHelper.setWallpaper(context, wp)
                                android.widget.Toast.makeText(
                                    context,
                                    if (ok) "Wallpaper set ✓" else "Wallpaper set fail",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Divider(color = Color.White.copy(alpha = 0.15f))
                Spacer(Modifier.height(12.dp))

                // ---- Backup / Restore section ----
                Text(
                    "Backup & Restore",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    "Apna layout (dock, settings) ek file mein save karo. " +
                        "Phone badle ya app reinstall ho to wapas le aao.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            // Pehle save karo phir backup banao (taaki latest data jaye)
                            prefs.gridColumns = columns
                            prefs.groqApiKey = apiKey
                            prefs.smartPredictionEnabled = predictOn
                            onBackup()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Backup") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onRestore,
                        modifier = Modifier.weight(1f)
                    ) { Text("Restore") }
                }

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onClose) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        prefs.gridColumns = columns
                        prefs.groqApiKey = apiKey
                        prefs.smartPredictionEnabled = predictOn
                        prefs.iconPack = iconPack
                        onChanged()
                        onClose()
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun PowerRow(
    title: String,
    status: String,
    done: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp)
            Text(
                status,
                color = if (done) Color(0xFF66E08F) else Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
        if (!done) {
            Button(onClick = onClick) { Text("Enable") }
        }
    }
}

/** Icon pack chunne ka ek option row (radio-style). */
@Composable
private fun IconPackOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(4.dp))
        Text(label, color = Color.White, fontSize = 14.sp)
    }
}

/** Ek wallpaper preview swatch (gradient circle). Tap karke set hota hai. */
@Composable
private fun WallpaperSwatch(
    wallpaper: WallpaperHelper.AuraWallpaper,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(end = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        wallpaper.colors.map { Color(it) }
                    )
                )
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onClick() })
                }
        )
        Spacer(Modifier.height(4.dp))
        Text(
            wallpaper.name,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp
        )
    }
}

