package com.aura.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

@Composable
fun SettingsPanel(
    prefs: AuraPrefs,
    apps: List<AppInfo> = emptyList(),
    onClose: () -> Unit,
    onChanged: () -> Unit,
    onBackup: () -> Unit = {},
    onRestore: () -> Unit = {}
) {
    val context = LocalContext.current
    var columns   by remember { mutableStateOf(prefs.gridColumns) }
    var apiKey    by remember { mutableStateOf(prefs.groqApiKey) }
    var predictOn by remember { mutableStateOf(prefs.smartPredictionEnabled) }
    var useSysWp  by remember { mutableStateOf(prefs.useSystemWallpaper) }
    var iconPack  by remember { mutableStateOf(prefs.iconPack) }
    var showHiddenAppsManager by remember { mutableStateOf(false) }
    var showProDialog by remember { mutableStateOf(false) }
    var showIconChangeWarning by remember { mutableStateOf(false) }
    var pendingAliasToSet by remember { mutableStateOf("") }

    val isDefault = remember { LauncherActions.isDefaultLauncher(context) }
    val hasUsage = remember { RecentApps.hasUsagePermission(context) }
    val hasAdmin = remember { LockHelper.isDeviceAdminEnabled(context) }

    // Show hidden apps manager if requested
    if (showHiddenAppsManager) {
        HiddenAppsDialog(
            prefs = prefs,
            apps = apps,
            onDismiss = { showHiddenAppsManager = false }
        )
    }

    if (showProDialog) {
        Dialog(onDismissRequest = { showProDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF141124),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.5f)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("👑 Unlock Premium Icons", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "This launcher icon is a premium Pro feature. Upgrade your plan to unlock Whirl, Gold, and Cyberpunk icons, plus 10 dynamic AI wallpapers and Freezer features.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            prefs.isPro = true
                            showProDialog = false
                            android.widget.Toast.makeText(context, "Thank you for subscribing to Pro! ✓", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Upgrade to Pro - $4.99/mo", color = Color(0xFF1B1730), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showProDialog = false }) {
                        Text("Close", color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }

    if (showIconChangeWarning) {
        Dialog(onDismissRequest = { showIconChangeWarning = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF141124),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF9D86FF).copy(alpha = 0.5f)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("⚠️ Apply Icon Theme?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Android security resets the default launcher choice when switching launcher icons. Aura will ask you to select it as your default launcher after you proceed.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            showIconChangeWarning = false
                            LauncherActions.setAppIcon(context, pendingAliasToSet)
                            prefs.activeIconAlias = pendingAliasToSet
                            onChanged()
                            LauncherActions.requestSetDefault(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9D86FF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Proceed & Set Default", color = Color(0xFF141124), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showIconChangeWarning = false }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }

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

                Spacer(Modifier.height(8.dp))
                Divider(color = Color.White.copy(alpha = 0.15f))
                Spacer(Modifier.height(12.dp))

                // ---- Gesture customization ----
                Text(
                    "Gestures",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    "Har gesture pe koi bhi action assign karo.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
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
                    onClick = { LockHelper.requestDeviceAdmin(context) }
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

                Spacer(Modifier.height(12.dp))
                // Wallpaper transparency toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show System Wallpaper", color = Color.White, fontSize = 14.sp)
                        Text(
                            "Let system wallpaper show behind launcher",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                    Switch(checked = useSysWp, onCheckedChange = { useSysWp = it })
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

                // ---- Launcher Icon Section ----
                Text(
                    "Launcher App Icon",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    "Change the launcher icon on your home screen.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(8.dp))

                val currentAlias = remember { prefs.activeIconAlias }
                var selectedAliasState by remember { mutableStateOf(currentAlias) }

                val appIcons = listOf(
                    Triple("com.aura.launcher.MainActivity", "Classic Aura", false),
                    Triple("com.aura.launcher.MainActivityAliasNeon", "Neon Cyan", false),
                    Triple("com.aura.launcher.MainActivityAliasWhirl", "Whirl Violet (Pro)", true),
                    Triple("com.aura.launcher.MainActivityAliasGold", "Gold Premium (Pro)", true),
                    Triple("com.aura.launcher.MainActivityAliasCyber", "Cyber Pink (Pro)", true)
                )

                appIcons.forEach { (alias, name, isProIcon) ->
                    val isSelected = selectedAliasState == alias
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isProIcon && !prefs.isPro) {
                                    showProDialog = true
                                } else {
                                    if (alias != selectedAliasState) {
                                        pendingAliasToSet = alias
                                        showIconChangeWarning = true
                                    }
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                if (isProIcon && !prefs.isPro) {
                                    showProDialog = true
                                } else {
                                    if (alias != selectedAliasState) {
                                        pendingAliasToSet = alias
                                        showIconChangeWarning = true
                                    }
                                }
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF9D86FF))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = name,
                            color = if (isSelected) Color(0xFF9D86FF) else Color.White,
                            fontSize = 14.sp
                        )
                        if (isProIcon) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFFB300), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("PRO", color = Color(0xFF1B1730), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
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

                Spacer(Modifier.height(8.dp))
                Divider(color = Color.White.copy(alpha = 0.15f))
                Spacer(Modifier.height(12.dp))

                // ---- Hidden Apps Manager ----
                Text(
                    "Hidden Apps",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    "Long-press kisi bhi app pe aur 'Hide app' select karo.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showHiddenAppsManager = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Manage Hidden Apps") }

                Spacer(Modifier.height(8.dp))
                Divider(color = Color.White.copy(alpha = 0.15f))
                Spacer(Modifier.height(12.dp))
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
                        prefs.useSystemWallpaper = useSysWp
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

/** Dialog to manage hidden apps */
@Composable
private fun HiddenAppsDialog(
    prefs: AuraPrefs,
    apps: List<AppInfo>,
    onDismiss: () -> Unit
) {
    val hiddenApps = remember { prefs.getHiddenApps() }
    var hiddenList by remember { mutableStateOf(hiddenApps.toList()) }

    Dialog(onDismissRequest = onDismiss) {
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
                    text = "Hidden Apps (${hiddenList.size})",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))

                if (hiddenList.isEmpty()) {
                    Text(
                        "Koi app hide nahi hai. Long-press se hide kar sakte ho.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                } else {
                    val appMap = apps.associateBy { it.packageName }
                    hiddenList.forEach { pkg ->
                        val app = appMap[pkg]
                        if (app != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    app.label,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        prefs.showApp(pkg)
                                        hiddenList = hiddenList.filter { it != pkg }
                                    },
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text("Show")
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text("Done")
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
