package com.aura.launcher

import android.content.Context
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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
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

        // EDGE-TO-EDGE: Status bar aur navigation bar transparent banayein
        setupEdgeToEdge()

        // Mic permission ek baar maang lo (voice search ke liye, optional)
        requestMicPermissionIfNeeded()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerOpenState.value) drawerOpenState.value = false
                // else: kuch nahi — home pe hi raho (xOS wapas na aaye)
            }
        })

        setContent {
            AuraTheme {
                val context = LocalContext.current
                val prefs = remember { AuraPrefs(context) }
                var onboarded by remember { mutableStateOf(prefs.isOnboarded) }

                if (!onboarded) {
                    OnboardingScreen(
                        prefs = prefs,
                        onComplete = {
                            onboarded = true
                        }
                    )
                } else {
                    AuraHomeScreen(drawerOpenState)
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Ensure transparent edge-to-edge settings are applied
        if (hasFocus) setupEdgeToEdge()
    }

    override fun onResume() {
        super.onResume()
        // Badge refresh hone de jab notification change hो
        window.decorView.invalidate()

        // Role propagation delay safety check
        val context = this
        val prefs = AuraPrefs(context)
        val currentlyDefault = LauncherActions.isDefaultLauncher(context)
        val wasDefault = prefs.wasDefaultLauncher
        if (currentlyDefault && !wasDefault) {
            prefs.wasDefaultLauncher = true
            LauncherActions.restartAppSafely(context)
        } else if (!currentlyDefault) {
            prefs.wasDefaultLauncher = false
        }
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
     * Edge-to-edge layout with transparent status and navigation bars.
     * Keeps them fully visible and usable but allows launcher content to draw underneath.
     */
    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
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

    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val fetchedApps = AppRepository.getInstalledApps(context)
            VoiceSearch.setApps(fetchedApps)
            val defaultVal = LauncherActions.isDefaultLauncher(context)
            apps = fetchedApps
            isDefault = defaultVal
        }
    }

    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context?, intent: android.content.Intent?) {
                val action = intent?.action
                val isReplacing = intent?.getBooleanExtra(android.content.Intent.EXTRA_REPLACING, false) ?: false
                if (!isReplacing && (action == android.content.Intent.ACTION_PACKAGE_ADDED || action == android.content.Intent.ACTION_PACKAGE_REMOVED)) {
                    refreshTrigger++
                }
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction(android.content.Intent.ACTION_PACKAGE_ADDED)
            addAction(android.content.Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        context.registerReceiver(receiver, filter)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    LaunchedEffect(apps, drawerOpen.value) {
        if (apps.isNotEmpty()) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val fetchedRecents = RecentApps.getRecentApps(context, apps)
                val fetchedPredicted = if (prefs.smartPredictionEnabled) {
                    AppUsageTracker.getPredictedApps(context, apps, 4)
                } else emptyList()
                val defaultVal = LauncherActions.isDefaultLauncher(context)
                recents = fetchedRecents
                predicted = fetchedPredicted
                isDefault = defaultVal
            }
        } else {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                isDefault = LauncherActions.isDefaultLauncher(context)
            }
        }
    }

    var quickMenuOpen by remember { mutableStateOf(false) }
    var gridColumnsState by remember { mutableStateOf(prefs.gridColumns) }
    var useSystemWallpaperState by remember { mutableStateOf(prefs.useSystemWallpaper) }
    var openSettingsOnDrawerOpen by remember { mutableStateOf(false) }
    var multitaskerOpen by remember { mutableStateOf(false) }
    var homeFreezerOpen by remember { mutableStateOf(false) }

    val favPkgs = remember(drawerOpen.value) { prefs.getFavorites() }
    val favorites = remember(apps, favPkgs) {
        val appMap = apps.associateBy { it.packageName }
        favPkgs.mapNotNull { pkg -> appMap[pkg] }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ---- HOME SCREEN ----
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (useSystemWallpaperState) {
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.20f),
                                Color.Black.copy(alpha = 0.35f)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF0F0C1E),
                                Color(0xFF1B1730).copy(alpha = 0.9f),
                                Color(0xFF0F0C1E)
                            )
                        )
                    }
                )
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount < -25) {
                            when (prefs.swipeUpAction) {
                                "OPEN_DRAWER"   -> drawerOpen.value = true
                                "NOTIFICATIONS" -> LauncherActions.openNotifications(context)
                                "LOCK_SCREEN"   -> LockHelper.lockScreen(context)
                            }
                        } else if (dragAmount > 25) {
                            when (prefs.swipeDownAction) {
                                "NOTIFICATIONS" -> LauncherActions.openNotifications(context)
                                "OPEN_DRAWER"   -> drawerOpen.value = true
                                "LOCK_SCREEN"   -> LockHelper.lockScreen(context)
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            when (prefs.doubleTapAction) {
                                "LOCK_SCREEN"   -> LockHelper.lockScreen(context)
                                "NOTIFICATIONS" -> LauncherActions.openNotifications(context)
                                "OPEN_DRAWER"   -> drawerOpen.value = true
                            }
                        },
                        onLongPress = {
                            quickMenuOpen = true
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: battery row + clock + weather (same on all pages)
                Column {
                    // Battery widget — top right
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, end = 20.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        BatteryWidget()
                    }
                    if (!isDefault) {
                        SetDefaultBanner(onClick = { LauncherActions.requestSetDefault(context) })
                    }
                    ClockHeader(modifier = Modifier.padding(top = 4.dp))
                    WeatherWidget(modifier = Modifier.padding(top = 4.dp))
                }

                // Middle: suggestions + recents
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (predicted.isNotEmpty()) {
                        SmartSuggestRow(predicted = predicted) { AppRepository.launchApp(context, it) }
                    }
                    if (recents.isNotEmpty()) {
                        RecentRow(recents = recents)
                    }
                    Text("Swipe up for apps  ▲",
                        color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 16.dp))
                    DockBar(favorites = favorites)
                }
            }
        }

        // ---- APP DRAWER ----
        AnimatedVisibility(
            visible = drawerOpen.value,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeOut(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
        ) {
            AppDrawer(
                apps = apps,
                columns = gridColumnsState,
                onAppClick = { AppRepository.launchApp(context, it) },
                onClose = { drawerOpen.value = false },
                initialSettingsOpen = openSettingsOnDrawerOpen,
                onSettingsOpenHandled = { openSettingsOnDrawerOpen = false },
                onSettingsChanged = {
                    gridColumnsState = prefs.gridColumns
                    useSystemWallpaperState = prefs.useSystemWallpaper
                }
            )
        }

        // ---- FLOATING SEARCH BUBBLE ----
        // Show bubble only when drawer is closed to avoid overlay overlap
        if (!drawerOpen.value) {
            FloatingSearchBubble(drawerOpenState = drawerOpen)
        }

        // ---- QUICK HOME SETTINGS DIALOG ----
        if (quickMenuOpen) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { quickMenuOpen = false }
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xF2141124), // Sleek glassmorphism look
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "Aura Quick Dashboard",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // 4 Big Dashboard Grid Shortcuts
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Column 1: Wallpaper & Freezer
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Wallpaper Selector Hub
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            // Do nothing extra, wallpaper hub is quick strip below
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("🎨 Wallpapers", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Select dynamic AI themes", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                    }
                                }

                                // Freezer Dialog Shortcut
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            quickMenuOpen = false
                                            homeFreezerOpen = true
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("❄️ App Freezer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Hibernate battery draining apps", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                    }
                                }
                            }

                            // Column 2: Multitasker & Settings
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Multitasker panel
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            quickMenuOpen = false
                                            multitaskerOpen = true
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("🔀 Multitasker", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Search & view active apps", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                    }
                                }

                                // Settings Panel
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            quickMenuOpen = false
                                            openSettingsOnDrawerOpen = true
                                            drawerOpen.value = true
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("⚙️ Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Customize launcher layout", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Divider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(Modifier.height(16.dp))

                        // AI Wallpapers quick horizontal strip
                        Text("Quick Wallpaper Apply:", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                        WallpaperSelector(onApplied = {
                            useSystemWallpaperState = true
                            prefs.useSystemWallpaper = true
                        })

                        Spacer(Modifier.height(16.dp))
                        Divider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(Modifier.height(16.dp))

                        // Grid column settings row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "App Grid Columns",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Drawer Columns: $gridColumnsState",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (cols in 3..6) {
                                    val isSelected = gridColumnsState == cols
                                    Button(
                                        onClick = {
                                            prefs.gridColumns = cols
                                            gridColumnsState = cols
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) Color(0xFF6C4DF6) else Color.White.copy(alpha = 0.08f)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(cols.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        Button(
                            onClick = { quickMenuOpen = false },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C4DF6)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Done", color = Color.White)
                        }
                    }
                }
            }
        }

        if (homeFreezerOpen) {
            FreezerDialog(
                prefs = prefs,
                apps = apps,
                onDismiss = { homeFreezerOpen = false },
                onRefresh = {
                    // Update freezer state
                }
            )
        }

        if (multitaskerOpen) {
            MultitaskerView(
                apps = apps,
                onDismiss = { multitaskerOpen = false }
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
    onClose: () -> Unit,
    initialSettingsOpen: Boolean = false,
    onSettingsOpenHandled: () -> Unit = {},
    onSettingsChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { AuraPrefs(context) }
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val alphabets = remember { ('A'..'Z').toList() }

    var query by remember { mutableStateOf("") }
    var settingsOpen by remember { mutableStateOf(false) }
    var freezerOpen by remember { mutableStateOf(false) }
    var frozenAppsState by remember { mutableStateOf(prefs.getFrozenApps()) }

    LaunchedEffect(initialSettingsOpen) {
        if (initialSettingsOpen) {
            settingsOpen = true
            onSettingsOpenHandled()
        }
    }
    var cols by remember { mutableStateOf(columns) }
    var showCategories by remember { mutableStateOf(prefs.showCategoryView) }

    // AI state
    var aiAnswer by remember { mutableStateOf<String?>(null) }
    var aiLoading by remember { mutableStateOf(false) }

    // File search
    var files by remember { mutableStateOf<List<FileResult>>(emptyList()) }
    LaunchedEffect(query) {
        files = if (query.length >= 2) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                FileSearch.search(context, query)
            }
        } else emptyList()
    }

    val filtered = remember(query, apps, frozenAppsState) {
        val hiddenApps = prefs.getHiddenApps()
        val list = if (query.isBlank()) {
            apps.filter { it.packageName !in hiddenApps && it.packageName !in frozenAppsState }.toMutableList()
        } else {
            apps.filter { it.label.contains(query, ignoreCase = true) && it.packageName !in hiddenApps && it.packageName !in frozenAppsState }.toMutableList()
        }
        if (query.isBlank() || "Freezer".contains(query, ignoreCase = true)) {
            val dummyDrawable = object : android.graphics.drawable.ColorDrawable(0) {}
            list.add(
                0,
                AppInfo(
                    label = "Freezer",
                    packageName = "com.aura.launcher.freezer",
                    activityName = "com.aura.launcher.FreezerActivity",
                    icon = dummyDrawable
                )
            )
        }
        list
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
            apps = apps,
            onClose = { settingsOpen = false },
            onChanged = {
                cols = prefs.gridColumns
                onSettingsChanged()
            },
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
                onAskAi = { targetQuery ->
                    val q = if (targetQuery.isNotBlank()) targetQuery else query
                    if (q.isNotBlank()) {
                        val key = prefs.groqApiKey
                        aiLoading = true
                        aiAnswer = null
                        scope.launch {
                            val res = GroqClient.ask(key, q, apps)
                            aiLoading = false
                            when (res) {
                                is GroqClient.Result.Success -> {
                                    val json = runCatching { org.json.JSONObject(res.text) }.getOrNull()
                                    if (json != null) {
                                        val action = json.optString("action", "SAY")
                                        val param = json.optString("param", "")
                                        val reply = json.optString("reply", "")
                                        aiAnswer = reply
                                        executeAgenticAction(context, action, param, onClose, prefs, apps)
                                    } else {
                                        aiAnswer = res.text
                                    }
                                }
                                is GroqClient.Result.Error -> {
                                    aiAnswer = "⚠️ ${res.message}"
                                }
                                is GroqClient.Result.NoKey -> {
                                    aiAnswer = "AI key nahi mili. Settings → AI Assistant mein apni free Groq key daalo."
                                }
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
                Row(modifier = Modifier.fillMaxSize()) {
                    if (query.isBlank()) {
                        AlphabetSidebar(
                            alphabets = alphabets,
                            onLetterSelected = { letter ->
                                scope.launch {
                                    val index = filtered.indexOfFirst {
                                        it.label.startsWith(letter.toString(), ignoreCase = true)
                                    }
                                    if (index >= 0) {
                                        gridState.scrollToItem(index)
                                    }
                                }
                            }
                        )
                    }

                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(cols),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 8.dp)
                    ) {
                        items(filtered, key = { it.packageName }) { app ->
                            AppIcon(
                                app = app,
                                onClick = {
                                    if (app.packageName == "com.aura.launcher.freezer") {
                                        freezerOpen = true
                                    } else {
                                        onAppClick(app)
                                    }
                                }
                            )
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

    if (freezerOpen) {
        FreezerDialog(
            prefs = prefs,
            apps = apps,
            onDismiss = { freezerOpen = false },
            onRefresh = {
                frozenAppsState = prefs.getFrozenApps()
            }
        )
    }
}

/** Glass-style search bar with mic (voice) + AI button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onAskAi: (String) -> Unit
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
                            val opened = VoiceSearch.tryOpenApp(context, spoken)
                            if (!opened && hasAiKey) {
                                onAskAi(spoken)
                            }
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
                if (hasAiKey) onAskAi(query)
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

/** Freezer Dialog for managing background frozen apps */
@Composable
fun FreezerDialog(
    prefs: AuraPrefs,
    apps: List<AppInfo>,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val frozenPkgs = remember { prefs.getFrozenApps() }
    var frozenList by remember { mutableStateOf(frozenPkgs.toList()) }
    var showAddDialog by remember { mutableStateOf(false) }

    val appMap = remember(apps) { apps.associateBy { it.packageName } }

    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1B1730),
                modifier = Modifier.fillMaxHeight(0.8f).padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Apps to Freeze", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    
                    val nonFrozenApps = apps.filter { it.packageName !in frozenList && it.packageName != "com.aura.launcher.freezer" }
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(nonFrozenApps) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        prefs.freezeApp(app.packageName)
                                        frozenList = frozenList + app.packageName
                                        onRefresh()
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = rememberDrawablePainter(app.icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(app.label, color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { showAddDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xF2101625),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2193B0).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("❄️ Aura Freezer", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Frozen apps do not drain battery", color = Color.Cyan.copy(alpha = 0.7f), fontSize = 10.sp)
                    }
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2193B0)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Freeze App", fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))

                if (frozenList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Freezer is empty. Add apps to freeze and hibernate them.",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.weight(1f, fill = false).heightIn(max = 300.dp)
                    ) {
                        items(frozenList) { pkg ->
                            val app = appMap[pkg]
                            if (app != null) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .clickable {
                                            toast(context, "${app.label} is frozen! Unfreeze it first.")
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Image(
                                            painter = rememberDrawablePainter(app.icon),
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF2193B0).copy(alpha = 0.4f))
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        app.label,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    TextButton(
                                        onClick = {
                                            prefs.unfreezeApp(pkg)
                                            frozenList = frozenList.filter { it != pkg }
                                            onRefresh()
                                        },
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Text("Unfreeze", color = Color.Cyan, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
                ) {
                    Text("Close", color = Color.White)
                }
            }
        }
    }
}

/** Chhota toast helper. */
private fun toast(context: android.content.Context, msg: String) {
    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
}

/**
 * Agentic actions executed on the Main thread.
 */
private fun executeAgenticAction(
    context: android.content.Context,
    action: String,
    param: String,
    onClose: () -> Unit,
    prefs: AuraPrefs,
    apps: List<AppInfo>
) {
    val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    mainHandler.post {
        runCatching {
            when (action) {
                "LAUNCH_APP" -> {
                    if (param.isNotBlank()) {
                        AppRepository.launchByPackage(context, param, apps)
                        onClose()
                    }
                }
                "OPEN_SETTINGS" -> {
                    context.startActivity(
                        android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                "OPEN_NOTIFICATIONS" -> {
                    LauncherActions.openNotifications(context)
                }
                "CLOSE_DRAWER" -> {
                    onClose()
                }
                "SET_GRID" -> {
                    val cols = param.toIntOrNull()
                    if (cols != null && cols in 3..6) {
                        prefs.gridColumns = cols
                    }
                }
                "LOCK_SCREEN" -> {
                    LockHelper.lockScreen(context)
                }
            }
        }
    }
}

@Composable
fun AlphabetSidebar(
    alphabets: List<Char>,
    onLetterSelected: (Char) -> Unit
) {
    var activeLetter by remember { mutableStateOf<Char?>(null) }
    var boxHeight by remember { mutableStateOf(1f) }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(28.dp)
            .onGloballyPositioned { coordinates ->
                boxHeight = coordinates.size.height.toFloat()
            }
            .pointerInput(alphabets, boxHeight) {
                detectTapGestures(
                    onPress = { offset ->
                        val index = ((offset.y / boxHeight) * alphabets.size).toInt().coerceIn(0, alphabets.size - 1)
                        val letter = alphabets[index]
                        activeLetter = letter
                        onLetterSelected(letter)
                    }
                )
            }
            .pointerInput(alphabets, boxHeight) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val index = ((offset.y / boxHeight) * alphabets.size).toInt().coerceIn(0, alphabets.size - 1)
                        val letter = alphabets[index]
                        activeLetter = letter
                        onLetterSelected(letter)
                    },
                    onDragEnd = { activeLetter = null },
                    onDragCancel = { activeLetter = null },
                    onDrag = { change, _ ->
                        change.consume()
                        val positionY = change.position.y
                        val index = ((positionY / boxHeight) * alphabets.size).toInt().coerceIn(0, alphabets.size - 1)
                        val letter = alphabets[index]
                        if (letter != activeLetter) {
                            activeLetter = letter
                            onLetterSelected(letter)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            alphabets.forEach { letter ->
                val isSelected = letter == activeLetter
                Text(
                    text = letter.toString(),
                    color = if (isSelected) Color(0xFF9D86FF) else Color.White.copy(alpha = 0.5f),
                    fontSize = if (isSelected) 13.sp else 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }

        // Floating Bubble Indicator showing the selected letter
        activeLetter?.let { letter ->
            Box(
                modifier = Modifier
                    .offset(x = 48.dp) // Offset to the right to be visible over the grid
                    .size(48.dp)
                    .background(Color(0xFF6C4DF6), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letter.toString(),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MultitaskerView(
    apps: List<AppInfo>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AuraPrefs(context) }
    var searchQuery by remember { mutableStateOf("") }

    val activeApps = remember(apps) {
        val list = if (RecentApps.hasUsagePermission(context)) {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
            val now = System.currentTimeMillis()
            val dayAgo = now - 24 * 60 * 60 * 1000
            val stats = usm.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, dayAgo, now) ?: emptyList()
            val lastUsed = stats.filter { it.lastTimeUsed > 0 }
                .groupBy { it.packageName }
                .mapValues { entry -> entry.value.maxOf { it.lastTimeUsed } }
            lastUsed.entries.sortedByDescending { it.value }
                .mapNotNull { e -> apps.find { it.packageName == e.key } }
                .filter { it.packageName != context.packageName }
                .take(20)
        } else {
            emptyList()
        }
        if (list.isEmpty()) {
            RecentsHelper.getRunningTasks(context, max = 20).mapNotNull { rt ->
                apps.find { it.packageName == rt.packageName }
            }
        } else {
            list
        }
    }

    var activeListState by remember { mutableStateOf(activeApps) }

    val filteredActive = remember(searchQuery, activeListState) {
        if (searchQuery.isBlank()) activeListState
        else activeListState.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xF2141124),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6C4DF6).copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "🔀 Aura Multitasker",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${activeListState.size} active background tasks",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Home, "Home", tint = Color.White)
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search running tasks...", color = Color.White.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF9D86FF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        containerColor = Color.White.copy(alpha = 0.05f)
                    ),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, "Search", tint = Color.White.copy(alpha = 0.5f)) }
                )

                Spacer(Modifier.height(16.dp))

                if (filteredActive.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (searchQuery.isNotBlank()) "No matching running apps"
                            else "No active tasks found",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(max = 260.dp)
                    ) {
                        items(filteredActive, key = { it.packageName }) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Image(
                                        painter = rememberDrawablePainter(app.icon),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            app.label,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            app.packageName,
                                            color = Color.White.copy(alpha = 0.4f),
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = {
                                        prefs.freezeApp(app.packageName)
                                        activeListState = activeListState.filter { it.packageName != app.packageName }
                                        toast(context, "${app.label} hibernated! ❄️")
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5252))
                                ) {
                                    Text("Hibernate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            activeListState.forEach { app ->
                                prefs.freezeApp(app.packageName)
                            }
                            activeListState = emptyList()
                            toast(context, "RAM Cleared! All tasks hibernated. ⚡")
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear RAM ⚡", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C4DF6)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}


