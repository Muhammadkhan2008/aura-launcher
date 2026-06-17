package com.aura.launcher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

