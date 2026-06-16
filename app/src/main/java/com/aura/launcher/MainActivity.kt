package com.aura.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * MainActivity — Aura ka home screen.
 *
 * HOME category Manifest mein hai, isliye home button dabane pe ye khulti hai.
 *
 * BACK BUTTON FIX: Launcher mein back button se "bahar" nahi nikalna chahiye
 * (warna purana launcher/xOS aa jaata tha). Yahan back button ko handle
 * karte hain — agar drawer khula hai to band karo, warna kuch mat karo.
 */
class MainActivity : ComponentActivity() {

    // Drawer khula hai ya nahi — back button ke liye track karte hain
    private var drawerOpenState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Back button ko control karo
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Drawer khula hai to band karo; warna kuch mat karo
                // (home pe hi raho, xOS wapas na aaye)
                if (drawerOpenState.value) {
                    drawerOpenState.value = false
                }
                // else: jaan-bujh ke kuch nahi — launcher home pe rahega
            }
        })

        setContent {
            AuraHomeScreen(drawerOpenState)
        }
    }
}

@Composable
fun AuraHomeScreen(drawerOpen: MutableState<Boolean>) {
    val context = LocalContext.current
    val prefs = remember { AuraPrefs(context) }

    var apps by remember { mutableStateOf(emptyList<AppInfo>()) }
    LaunchedEffect(Unit) {
        apps = AppRepository.getInstalledApps(context)
    }

    // Dock ke favourite apps
    val favorites = remember(apps, drawerOpen.value) {
        val favPkgs = prefs.getFavorites()
        favPkgs.mapNotNull { pkg -> apps.find { it.packageName == pkg } }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ---- HOME SCREEN (background = phone ka wallpaper, theme se) ----
        // Halka sa dark scrim taaki text/clock saaf dikhe (boring gradient hata diya)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.45f)
                        )
                    )
                )
                // Swipe UP se drawer khulega
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount < -25) drawerOpen.value = true   // upar swipe
                    }
                }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Upar: bada clock
                ClockHeader(modifier = Modifier.padding(top = 64.dp))

                // Niche: dock (favourite apps) + hint
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Swipe up for apps  ▲",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    DockBar(favorites = favorites)
                }
            }
        }

        // ---- APP DRAWER (swipe up pe upar se neeche slide hota hai) ----
        AnimatedVisibility(
            visible = drawerOpen.value,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            AppDrawer(
                apps = apps,
                columns = prefs.gridColumns,
                onAppClick = { AppRepository.launchApp(context, it.packageName) },
                onClose = { drawerOpen.value = false }
            )
        }
    }
}

/**
 * DockBar — niche favourite apps ki row. Khaali ho to hint dikhata hai.
 */
@Composable
fun DockBar(favorites: List<AppInfo>) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color.Black.copy(alpha = 0.30f))
            .padding(vertical = 8.dp, horizontal = 8.dp)
    ) {
        if (favorites.isEmpty()) {
            Text(
                text = "Long-press any app → \"Add to dock\"",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(vertical = 12.dp)
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                favorites.forEach { app ->
                    AppIcon(
                        app = app,
                        iconSize = 52,
                        showLabel = false,
                        onClick = { AppRepository.launchApp(context, app.packageName) }
                    )
                }
            }
        }
    }
}

/**
 * AppDrawer — poori app list, search ke saath. Swipe up pe khulta hai.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer(
    apps: List<AppInfo>,
    columns: Int,
    onAppClick: (AppInfo) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AuraPrefs(context) }
    var query by remember { mutableStateOf("") }
    var settingsOpen by remember { mutableStateOf(false) }
    // columns ko state banaya taaki settings se badle to turant refresh ho
    var cols by remember { mutableStateOf(columns) }

    val filtered = remember(query, apps) {
        if (query.isBlank()) apps
        else apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    if (settingsOpen) {
        SettingsPanel(
            prefs = prefs,
            onClose = { settingsOpen = false },
            onChanged = { cols = prefs.gridColumns }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF20F0C1E))   // gehra semi-transparent panel
            // Swipe DOWN se drawer band
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 25) onClose()
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp)
        ) {
            // Header: title + settings gear
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "All Apps  (${apps.size})",
                    color = Color.White,
                    fontSize = 20.sp
                )
                IconButton(onClick = { settingsOpen = true }) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search apps...", color = Color.White.copy(alpha = 0.6f)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f)
                )
            )

            Spacer(Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(cols),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    AppIcon(app = app, onClick = { onAppClick(app) })
                }
            }
        }
    }
}


