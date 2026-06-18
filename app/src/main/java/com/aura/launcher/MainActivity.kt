package com.aura.launcher

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
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

        // IMMERSIVE MODE: sirf Aura ki screen pe nav bar + status bar
        // chhupte hain (Nova/launcher jaisa "takeover" feel). Ye system-wide
        // NAHI hai — kisi aur app mein nav bar normal rahega.
        enableImmersiveMode()

        // Mic permission ek baar maang lo (voice search ke liye, optional)
        requestMicPermissionIfNeeded()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerOpenState.value) drawerOpenState.value = false
                // else: kuch nahi — home pe hi raho (xOS wapas na aaye)
            }
        })

        setContent { AuraTheme { AuraHomeScreen(drawerOpenState) } }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Jab user wapas Aura pe aaye to immersive dobara lagao
        if (hasFocus) enableImmersiveMode()
    }

    override fun onResume() {
        super.onResume()
        // Badge refresh hone de jab notification change hो
        window.decorView.invalidate()
    }

    /** Mic + storage + location permission maango. */
    private fun requestMicPermissionIfNeeded() {
        val needed = mutableListOf<String>()

        // Mic (voice search)
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            needed.add(android.Manifest.permission.RECORD_AUDIO)
        }

        // Location (weather widget)
        if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            needed.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        // Storage / media (file search) — Android version ke hisaab se
        val storagePerms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        storagePerms.forEach { p ->
            if (checkSelfPermission(p) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                needed.add(p)
            }
        }

        if (needed.isNotEmpty()) {
            runCatching { requestPermissions(needed.toTypedArray(), 1001) }
        }
    }

    /**
     * Immersive mode — content ko edge-to-edge banata hai aur system bars
     * (status + navigation) ko swipe-to-reveal mode mein chhupata hai.
     * Sirf is activity (Aura home) pe asar — system-wide nahi.
     */
    private fun enableImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Status/nav bar transparent rahein
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        }
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
        VoiceSearch.setApps(apps)   // voice command app khol sake
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

        // ---- HOME SCREEN (background = premium gradient overlay) ----
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0F0C1E),
                            Color(0xFF1B1730).copy(alpha = 0.9f),
                            Color(0xFF0F0C1E)
                        )
                    )
                )
                .pointerInput(Unit) {
                    detectVerticalDragGestures(onVerticalDrag = { change, dragAmount ->
                        if (dragAmount < -25) drawerOpen.value = true
                        else if (dragAmount > 25) LauncherActions.openNotifications(context)
                    })
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
                    WeatherWidget(modifier = Modifier.padding(top = 4.dp))
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

                // Bottom navbar (back, home, recents) — system buttons ki tarah
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.40f))
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    IconButton(
                        onClick = { /* System back — already handled */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    // Home button (currently home, so just visual)
                    IconButton(
                        onClick = { /* Already on home */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Home, "Home", tint = Color(0xFF9D86FF), modifier = Modifier.size(22.dp))
                    }
                    // Recents button
                    IconButton(
                        onClick = {
                            val running = RecentsHelper.getRunningTasks(context, 8)
                            android.widget.Toast.makeText(context, if (running.isEmpty()) "No recent apps" else "${running.size} recent apps", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.MoreVert, "Recents", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
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
            "✨ For you",
            color = Color(0xFF9D86FF),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
        )
        LazyRow(modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)) {
            items(predicted, key = { it.packageName }) { app ->
                AppIcon(app = app, iconSize = 56, showLabel = false, onClick = { onClick(app) })
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
            color = Color(0xFF9D86FF),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
        )
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            items(recents, key = { it.packageName }) { app ->
                AppIcon(
                    app = app,
                    iconSize = 56,
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
                        iconSize = 64,
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
    var showCategories by remember { mutableStateOf(prefs.showCategoryView) }

    // AI state
    var aiAnswer by remember { mutableStateOf<String?>(null) }
    var aiLoading by remember { mutableStateOf(false) }

    // File search (universal search): query par files bhi dhoondho
    var files by remember { mutableStateOf<List<FileResult>>(emptyList()) }
    LaunchedEffect(query) {
        files = if (query.length >= 2) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                FileSearch.search(context, query)
            }
        } else emptyList()
    }

    val filtered = remember(query, apps) {
        if (query.isBlank()) apps
        else apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    // Backup: file create karke usme settings likho
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val ok = BackupManager.exportTo(context, uri)
            toast(context, if (ok) "Backup save ho gaya ✓" else "Backup fail")
        }
    }
    // Restore: file choose karke usme se settings padho
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val ok = BackupManager.importFrom(context, uri)
            toast(context, if (ok) "Restore ho gaya ✓ (drawer dobara kholo)" else "Restore fail")
            if (ok) cols = prefs.gridColumns
        }
    }

    if (settingsOpen) {
        SettingsPanel(
            prefs = prefs,
            onClose = { settingsOpen = false },
            onChanged = { cols = prefs.gridColumns },
            onBackup = { backupLauncher.launch("aura_backup.json") },
            onRestore = { restoreLauncher.launch(arrayOf("application/json")) }
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
                    TextButton(onClick = {
                        showCategories = !showCategories
                        prefs.showCategoryView = showCategories
                    }) {
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
                    if (it.isNotBlank()) SearchHistory.addQuery(context, it)
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
                    // Universal search: files bhi dikhao (jab query ho)
                    if (files.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                "Files  (${files.size})",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(files, span = { GridItemSpan(maxLineSpan) }, key = { it.uri.toString() }) { f ->
                            FileRow(file = f, onClick = { FileSearch.openFile(context, f) })
                        }
                    }
                }
            }
        }
    }
}

/** Glass-style search bar with mic (voice) + AI button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onAskAi: () -> Unit
) {
    val context = LocalContext.current
    var showHistory by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(emptyList<String>()) }
    val prefs = remember { AuraPrefs(context) }
    val hasAiKey = remember { prefs.hasAiKey() }

    LaunchedEffect(Unit) {
        history = SearchHistory.getHistory(context)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search apps or ask AI…", color = Color.White.copy(alpha = 0.6f)) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color.White.copy(alpha = 0.7f)) },
                trailingIcon = {
                    IconButton(onClick = {
                        VoiceSearch.startListening(context) { spoken ->
                            onQueryChange(spoken)
                            VoiceSearch.tryOpenApp(context, spoken)
                        }
                    }) {
                        Icon(Icons.Filled.Mic, "Voice search", tint = Color(0xFF9D86FF))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            if (query.isEmpty()) showHistory = !showHistory
                        })
                    },
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF9D86FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f)
                )
            )
            if (showHistory && history.isNotEmpty()) {
                Surface(
                    color = Color(0xFF1B1730),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp)
                ) {
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(history) { h ->
                            Text(
                                h,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(Unit) {
                                        detectTapGestures(onTap = {
                                            onQueryChange(h)
                                            showHistory = false
                                        })
                                    }
                                    .padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = {
                if (hasAiKey) onAskAi()
            },
            enabled = hasAiKey,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (hasAiKey) Color(0xFF6C4DF6) else Color.Gray.copy(alpha = 0.5f)
            )
        ) {
            Text("AI", color = if (hasAiKey) Color.White else Color.White.copy(alpha = 0.5f))
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

/** FileRow — universal search mein mili ek file ki row (icon + naam). */
@Composable
fun FileRow(file: FileResult, onClick: () -> Unit) {
    val emoji = when (file.type) {
        FileType.IMAGE -> "🖼️"
        FileType.VIDEO -> "🎬"
        FileType.AUDIO -> "🎵"
        FileType.DOCUMENT -> "📄"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.width(12.dp))
        Text(
            file.name,
            color = Color.White,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

/** Chhota toast helper. */
private fun toast(context: android.content.Context, msg: String) {
    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
}


