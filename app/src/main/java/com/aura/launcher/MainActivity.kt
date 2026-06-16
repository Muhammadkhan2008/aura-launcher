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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * MainActivity — Aura ka home screen (Phase 4).
 *
 * HOME category Manifest mein hai, isliye home button pe ye khulti hai.
 * Back button handle karte hain taaki launcher home pe rahe.
 */
class MainActivity : ComponentActivity() {

    private var drawerOpenState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerOpenState.value) drawerOpenState.value = false
                // else: kuch nahi — home pe hi raho (xOS wapas na aaye)
            }
        })

        setContent { AuraTheme { AuraHomeScreen(drawerOpenState) } }
    }
}

@Composable
fun AuraHomeScreen(drawerOpen: MutableState<Boolean>) {
    val context = LocalContext.current
    val prefs = remember { AuraPrefs(context) }

    var apps by remember { mutableStateOf(emptyList<AppInfo>()) }
    var recents by remember { mutableStateOf(emptyList<AppInfo>()) }
    var predicted by remember { mutableStateOf(emptyList<AppInfo>()) }
    var isDefault by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        apps = AppRepository.getInstalledApps(context)
        isDefault = LauncherActions.isDefaultLauncher(context)
    }

    // Drawer band hone / apps load hone pe recents + prediction refresh
    LaunchedEffect(apps, drawerOpen.value) {
        if (apps.isNotEmpty()) {
            recents = RecentApps.getRecentApps(context, apps)
            if (prefs.smartPredictionEnabled) {
                predicted = AppUsageTracker.getPredictedApps(context, apps, 4)
            }
        }
        isDefault = LauncherActions.isDefaultLauncher(context)
    }

    val favorites = remember(apps, drawerOpen.value) {
        val favPkgs = prefs.getFavorites()
        favPkgs.mapNotNull { pkg -> apps.find { it.packageName == pkg } }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ---- HOME SCREEN (background = phone ka wallpaper) ----
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.55f)
                        )
                    )
                )
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount < -25) drawerOpen.value = true
                        else if (dragAmount > 25) LauncherActions.openNotifications(context)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { LockHelper.lockScreen(context) })
                }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    if (!isDefault) {
                        SetDefaultBanner(onClick = { LauncherActions.requestSetDefault(context) })
                    }
                    ClockHeader(modifier = Modifier.padding(top = 56.dp))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Smart prediction (on-device AI) — "is waqt ye apps"
                    if (predicted.isNotEmpty()) {
                        SmartSuggestRow(predicted = predicted) { AppRepository.launchApp(context, it) }
                    }
                    if (recents.isNotEmpty()) {
                        RecentRow(recents = recents)
                    }
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

        // ---- APP DRAWER ----
        AnimatedVisibility(
            visible = drawerOpen.value,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            AppDrawer(
                apps = apps,
                columns = prefs.gridColumns,
                onAppClick = { AppRepository.launchApp(context, it) },
                onClose = { drawerOpen.value = false }
            )
        }
    }
}

/**
 * SmartSuggestRow — on-device prediction: "is waqt aap ye apps kholte ho".
 * Koi API nahi — AppUsageTracker se aata hai.
 */
@Composable
fun SmartSuggestRow(predicted: List<AppInfo>, onClick: (AppInfo) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "✨ Suggested for you",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 24.dp, bottom = 6.dp)
        )
        LazyRow(modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)) {
            items(predicted, key = { it.packageName }) { app ->
                AppIcon(app = app, iconSize = 50, showLabel = false, onClick = { onClick(app) })
            }
        }
    }
}

/**
 * SetDefaultBanner — agar Aura default launcher nahi hai to
 * upar ek banner dikhta hai "tap to set default". Ek launcher ki
 * "kabza" lene ki sabse zaroori cheez.
 */
@Composable
fun SetDefaultBanner(onClick: () -> Unit) {
    Surface(
        color = Color(0xFF6C4DF6),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Aura ko default banao", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    "Tap karo → poora home Aura ka ho jayega",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp
                )
            }
            Text("SET  →", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * RecentRow — Aura ka apna recent apps (system recents ka alternative).
 */
@Composable
fun RecentRow(recents: List<AppInfo>) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Recent",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 24.dp, bottom = 6.dp)
        )
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            items(recents, key = { it.packageName }) { app ->
                AppIcon(
                    app = app,
                    iconSize = 48,
                    showLabel = false,
                    onClick = { AppRepository.launchApp(context, app) }
                )
            }
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
                        onClick = { AppRepository.launchApp(context, app) }
                    )
                }
            }
        }
    }
}

/**
 * AppDrawer — poori app list + AI search + auto-categories.
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
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var settingsOpen by remember { mutableStateOf(false) }
    var cols by remember { mutableStateOf(columns) }
    var showCategories by remember { mutableStateOf(false) }

    // AI state
    var aiAnswer by remember { mutableStateOf<String?>(null) }
    var aiLoading by remember { mutableStateOf(false) }

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
            .background(Color(0xF20F0C1E))
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
            // Header: title + category toggle + settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (showCategories) "Categories" else "All Apps  (${apps.size})",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row {
                    TextButton(onClick = { showCategories = !showCategories }) {
                        Text(
                            if (showCategories) "Grid" else "Folders",
                            color = Color(0xFF9D86FF)
                        )
                    }
                    IconButton(onClick = { settingsOpen = true }) {
                        Icon(Icons.Filled.Settings, "Settings", tint = Color.White)
                    }
                }
            }

            // Search bar (apps + AI). Enter dabane pe AI se pucho.
            GlassSearchBar(
                query = query,
                onQueryChange = {
                    query = it
                    aiAnswer = null
                },
                onAskAi = {
                    if (query.isNotBlank()) {
                        val key = prefs.groqApiKey
                        aiLoading = true
                        aiAnswer = null
                        scope.launch {
                            val res = GroqClient.ask(key, query)
                            aiLoading = false
                            aiAnswer = when (res) {
                                is GroqClient.Result.Success -> res.text
                                is GroqClient.Result.Error -> "⚠️ ${res.message}"
                                is GroqClient.Result.NoKey ->
                                    "AI key nahi mili. Settings → AI Assistant mein apni free Groq key daalo."
                            }
                        }
                    }
                }
            )

            // AI answer panel
            if (aiLoading || aiAnswer != null) {
                AiAnswerCard(loading = aiLoading, answer = aiAnswer)
            }

            Spacer(Modifier.height(8.dp))

            // Body: categories ya grid
            if (showCategories) {
                CategoryList(context = context, apps = apps, onAppClick = onAppClick)
            } else {
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
}

/** Glass-style search bar with an AI button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onAskAi: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search apps or ask AI…", color = Color.White.copy(alpha = 0.6f)) },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color.White.copy(alpha = 0.7f)) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF9D86FF),
                unfocusedBorderColor = Color.White.copy(alpha = 0.25f)
            )
        )
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onAskAi,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C4DF6))
        ) {
            Text("AI")
        }
    }
}

/** AI answer card (loading spinner ya jawaab). */
@Composable
fun AiAnswerCard(loading: Boolean, answer: String?) {
    Surface(
        color = Color(0xFF231C3D),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .heightIn(max = 260.dp)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF9D86FF),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Aura AI soch raha hai…", color = Color.White.copy(alpha = 0.85f))
                }
            } else {
                LazyColumn {
                    item {
                        Text(
                            answer ?: "",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

/** Auto-folders: apps category-wise group karke dikhata hai. */
@Composable
fun CategoryList(
    context: android.content.Context,
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit
) {
    val grouped = remember(apps) { AppCategorizer.groupApps(context, apps) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        grouped.forEach { (cat, list) ->
            item(key = cat.name) {
                Text(
                    "${cat.title}  (${list.size})",
                    color = Color(0xFF9D86FF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
                )
            }
            item(key = cat.name + "_row") {
                LazyRow {
                    items(list, key = { it.packageName }) { app ->
                        AppIcon(app = app, iconSize = 52, onClick = { onAppClick(app) })
                    }
                }
            }
        }
    }
}


