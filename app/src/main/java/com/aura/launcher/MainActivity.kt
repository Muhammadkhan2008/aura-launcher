package com.aura.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * MainActivity — Aura ka home screen.
 *
 * Kyunki Manifest mein HOME category di hai, ye activity
 * tab khulti hai jab user home button dabaata hai.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AuraHomeScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraHomeScreen() {
    val context = LocalContext.current

    // Saari apps load karo (offline, PackageManager se)
    var apps by remember { mutableStateOf(emptyList<AppInfo>()) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        apps = AppRepository.getInstalledApps(context)
    }

    // Search filter
    val filtered = remember(query, apps) {
        if (query.isBlank()) apps
        else apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    // Premium gradient background (beauty feature)
    val bg = Brush.verticalGradient(
        listOf(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(top = 48.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Bada clock + date (beauty, koi API nahi)
            ClockHeader(modifier = Modifier.padding(bottom = 24.dp))

            // Universal search bar (abhi local app search; baad mein AI judega)
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

            // App grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    AppIcon(app = app) {
                        AppRepository.launchApp(context, app.packageName)
                    }
                }
            }
        }
    }
}

@Composable
fun AppIcon(app: AppInfo, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(12.dp)
            .clickableNoRipple(onClick)
    ) {
        // Drawable ko Compose mein dikhane ke liye Coil use hota hai;
        // simple rakhne ke liye abhi placeholder box + label.
        coil.compose.AsyncImage(
            model = app.icon,
            contentDescription = app.label,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = app.label,
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Helper: bina ripple ke clickable (clean look)
@Composable
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    return this.then(
        androidx.compose.foundation.clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onClick
        )
    )
}
