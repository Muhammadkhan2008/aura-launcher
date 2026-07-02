package com.aura.launcher

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.key
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
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

        // Bypass FileUriExposedException on Android 7.0+
        val builder = android.os.StrictMode.VmPolicy.Builder()
        android.os.StrictMode.setVmPolicy(builder.build())

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
                var activeIconAliasState by remember { mutableStateOf(prefs.activeIconAlias ?: "") }

                CompositionLocalProvider(LocalActiveIconAlias provides activeIconAliasState) {
                    if (!onboarded) {
                        OnboardingScreen(
                            prefs = prefs,
                            onComplete = {
                                prefs.isOnboarded = true
                                onboarded = true
                            }
                        )
                    } else {
                        AuraHomeScreen(
                            drawerOpen = drawerOpenState,
                            onIconAliasChanged = {
                                activeIconAliasState = it
                            }
                        )
                    }
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
fun AuraHomeScreen(
    drawerOpen: MutableState<Boolean>,
    onIconAliasChanged: (String) -> Unit = {}
) {
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
    var lockScreenOpen by remember { mutableStateOf(false) }
    var layoutStyleState by remember { mutableStateOf(prefs.layoutStyle) }

    val favPkgs = remember(drawerOpen.value) { prefs.getFavorites() }
    val favorites = remember(apps, favPkgs) {
        val appMap = apps.associateBy { it.packageName }
        favPkgs.mapNotNull { pkg -> appMap[pkg] }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ---- HOME SCREEN ----
        if (layoutStyleState == "WINDOWS") {
            AirViewWindowMode(
                apps = apps,
                favorites = favorites,
                useSystemWallpaper = useSystemWallpaperState,
                onSettingsOpen = {
                    openSettingsOnDrawerOpen = true
                    drawerOpen.value = true
                },
                onLockScreenTrigger = { lockScreenOpen = true },
                onOpenDrawer = { drawerOpen.value = true }
            )
        } else {
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
                                    "LOCK_SCREEN"   -> lockScreenOpen = true
                                }
                            } else if (dragAmount > 25) {
                                when (prefs.swipeDownAction) {
                                    "NOTIFICATIONS" -> LauncherActions.openNotifications(context)
                                    "OPEN_DRAWER"   -> drawerOpen.value = true
                                    "LOCK_SCREEN"   -> lockScreenOpen = true
                                }
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                when (prefs.doubleTapAction) {
                                    "LOCK_SCREEN"   -> lockScreenOpen = true
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
                            RecentRow(recents = recents, onSearchClick = { multitaskerOpen = true })
                        }
                        Text("Swipe up for apps  ▲",
                            color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 16.dp))
                        DockBar(favorites = favorites)
                    }
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
                    layoutStyleState = prefs.layoutStyle
                    onIconAliasChanged(prefs.activeIconAlias ?: "")
                },
                onLockScreenTrigger = { lockScreenOpen = true }
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

        if (lockScreenOpen) {
            AuraLockScreenOverlay(
                onUnlock = { lockScreenOpen = false }
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
fun RecentRow(recents: List<AppInfo>, onSearchClick: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Recent Apps",
                color = Color(0xFF9D86FF),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = "Search Recents",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
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
    onSettingsChanged: () -> Unit = {},
    onLockScreenTrigger: () -> Unit = {}
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
                                        executeAgenticAction(
                                            context = context,
                                            action = action,
                                            param = param,
                                            onClose = onClose,
                                            prefs = prefs,
                                            apps = apps,
                                            onQueryChange = { query = it },
                                            onLockScreenTrigger = onLockScreenTrigger
                                        )
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
    apps: List<AppInfo>,
    onQueryChange: (String) -> Unit = {},
    onLockScreenTrigger: () -> Unit = {}
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
                    onLockScreenTrigger()
                }
                "SEARCH_FILES" -> {
                    if (param.isNotBlank()) {
                        onQueryChange(param)
                    }
                }
                "CREATE_NOTE" -> {
                    if (param.isNotBlank()) {
                        val dir = java.io.File(context.getExternalFilesDir(null), "AuraNotes")
                        if (!dir.exists()) dir.mkdirs()
                        val file = java.io.File(dir, "note_${System.currentTimeMillis()}.txt")
                        file.writeText(param)
                        android.widget.Toast.makeText(
                            context,
                            "Note created: ${file.name} ✓",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
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
                    .offset(x = (-56).dp) // Offset to the left to be visible over the grid
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
                            .height(280.dp),
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
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        items(filteredActive, key = { it.packageName }) { app ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier
                                    .width(180.dp)
                                    .fillMaxHeight()
                                    .clickable {
                                        AppRepository.launchApp(context, app)
                                        onDismiss()
                                    }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Icon + Label
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Image(
                                            painter = rememberDrawablePainter(app.icon),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            app.label,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Preview placeholder
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color.White.copy(alpha = 0.02f),
                                                        Color.White.copy(alpha = 0.06f)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("⚡ Running", color = Color(0xFF00FFCC), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            Text(app.packageName.takeLast(16), color = Color.White.copy(alpha = 0.3f), fontSize = 8.sp, maxLines = 1)
                                        }
                                    }

                                    // Action buttons (Lock + Hibernate)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                toast(context, "${app.label} locked in memory 🔒")
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Text("🔒", fontSize = 14.sp)
                                        }

                                        Button(
                                            onClick = {
                                                prefs.freezeApp(app.packageName)
                                                activeListState = activeListState.filter { it.packageName != app.packageName }
                                                toast(context, "${app.label} hibernated! ❄️")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252).copy(alpha = 0.15f)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Hibernate", color = Color(0xFFFF8A8A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Bottom row: centered Trash can + close text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            activeListState.forEach { app ->
                                prefs.freezeApp(app.packageName)
                            }
                            activeListState = emptyList()
                            toast(context, "RAM Cleared! All tasks hibernated. ⚡")
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFFFF5252).copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.3f), CircleShape)
                    ) {
                        Text("🗑️", fontSize = 24.sp)
                    }

                    Spacer(Modifier.width(20.dp))

                    TextButton(onClick = onDismiss) {
                        Text("Close", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AuraLockScreenOverlay(
    onUnlock: () -> Unit
) {
    val context = LocalContext.current

    var timeString by remember { mutableStateOf("") }
    var dateString by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = java.util.Calendar.getInstance()
            timeString = android.text.format.DateFormat.getTimeFormat(context).format(now.time)
            dateString = android.text.format.DateFormat.getMediumDateFormat(context).format(now.time)
            kotlinx.coroutines.delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFA0B0814)) // Translucent glassmorphic overlay
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -20) {
                        onUnlock()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 60.dp, horizontal = 24.dp)
        ) {
            // Top section: Greeting + lock icon
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "🔒 Aura Secured",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(48.dp))

                // Giant premium clock
                Text(
                    text = timeString,
                    color = Color.White,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-2).sp
                )

                // Date
                Text(
                    text = dateString,
                    color = Color(0xFF9D86FF),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Middle section: Live info widgets (Battery + Weather)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Battery status with a sleek visual bar
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("⚡", fontSize = 20.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Aura Battery Monitor", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Active Status", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                            // Glowing indicator bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(3.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f) // Mock fill level
                                        .fillMaxHeight()
                                        .background(
                                            Brush.horizontalGradient(listOf(Color(0xFF2193B0), Color(0xFF6D4DF6))),
                                            RoundedCornerShape(3.dp)
                                        )
                                )
                            }
                        }
                    }
                }

                // Weather Widget summary
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("☀️", fontSize = 24.sp)
                        Column {
                            Text("Aura Live Weather", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("28°C • Sunny Skies • Clear Visibility", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                    }
                }
            }

            // Bottom section: Swipe guide
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "▲ Swipe up to unlock",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Aura OS v1.0",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

// -------------------------------------------------------------
// WINDOWS DESKTOP LAYOUT (AIR VIEW WINDOW MODE) IMPLEMENTATION
// -------------------------------------------------------------

data class WindowInfo(
    val app: AppInfo?,
    val type: String = "APP", // "APP" | "BROWSER" | "EXPLORER"
    val id: String = java.util.UUID.randomUUID().toString(),
    val initialOffset: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset(100f, 150f),
    var offset: androidx.compose.ui.geometry.Offset = initialOffset,
    var isMaximized: Boolean = false,
    var isMinimized: Boolean = false,
    var size: androidx.compose.ui.unit.DpSize = androidx.compose.ui.unit.DpSize(340.dp, 450.dp),
    var zIndex: Float = 0f,
    val initialUrl: String = "https://google.com",
    var currentUrl: String = initialUrl,
    var currentFolder: String = "This PC",
    var currentPath: String = ""
)

sealed class DesktopItem {
    data class SystemItem(val id: String, val label: String, val emoji: String, val onClick: () -> Unit) : DesktopItem()
    data class AppItem(val app: AppInfo) : DesktopItem()
}

fun checkStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}

fun requestStoragePermission(context: Context) {
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } else {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}

fun getMimeType(file: java.io.File): String {
    val ext = file.extension.lowercase()
    return when (ext) {
        "jpg", "jpeg", "png", "webp", "gif" -> "image/*"
        "mp4", "mkv", "3gp", "webm" -> "video/*"
        "mp3", "wav", "ogg", "aac", "flac" -> "audio/*"
        "pdf" -> "application/pdf"
        "txt", "html", "xml", "json", "md" -> "text/plain"
        "apk" -> "application/vnd.android.package-archive"
        else -> "*/*"
    }
}

fun getFileEmoji(file: java.io.File): String {
    val ext = file.extension.lowercase()
    return when (ext) {
        "jpg", "jpeg", "png", "webp", "gif" -> "🖼️"
        "mp4", "mkv", "3gp", "webm" -> "🎬"
        "mp3", "wav", "ogg", "aac", "flac" -> "🎵"
        "pdf" -> "📕"
        "txt", "html", "xml", "json", "md" -> "📝"
        "apk" -> "🤖"
        "zip", "rar", "7z", "tar", "gz" -> "📦"
        "doc", "docx", "xls", "xlsx", "ppt", "pptx" -> "📄"
        else -> "📄"
    }
}

fun openActualFile(context: Context, file: java.io.File) {
    runCatching {
        val uri = Uri.fromFile(file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, getMimeType(file))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }.onFailure {
        android.widget.Toast.makeText(context, "Cannot open file: no compatible app found", android.widget.Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun AirViewWindowMode(
    apps: List<AppInfo>,
    favorites: List<AppInfo>,
    useSystemWallpaper: Boolean,
    onSettingsOpen: () -> Unit,
    onLockScreenTrigger: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AuraPrefs(context) }

    // List of active windows
    var activeWindows by remember { mutableStateOf(listOf<WindowInfo>()) }
    // Start menu open state
    var startMenuOpen by remember { mutableStateOf(false) }
    // Start menu search query
    var startSearchQuery by remember { mutableStateOf("") }

    val density = LocalDensity.current

    // Combine system desktop shortcuts and user apps in a Windows Desktop style
    val desktopItems = remember(apps) {
        val list = mutableListOf<DesktopItem>()
        val storageRoot = Environment.getExternalStorageDirectory().absolutePath

        // User Folder
        list.add(DesktopItem.SystemItem("user", "User Profile", "📁") {
            val maxZ = activeWindows.maxOfOrNull { it.zIndex } ?: 0f
            activeWindows = activeWindows + WindowInfo(
                app = null,
                type = "EXPLORER",
                currentFolder = "Documents",
                currentPath = "$storageRoot/Documents",
                zIndex = maxZ + 1f
            )
        })
        // This PC
        list.add(DesktopItem.SystemItem("pc", "This PC", "💻") {
            val maxZ = activeWindows.maxOfOrNull { it.zIndex } ?: 0f
            activeWindows = activeWindows + WindowInfo(
                app = null,
                type = "EXPLORER",
                currentFolder = "This PC",
                currentPath = storageRoot,
                zIndex = maxZ + 1f
            )
        })
        // Recycle Bin
        list.add(DesktopItem.SystemItem("bin", "Recycle Bin", "🗑️") {
            android.widget.Toast.makeText(context, "Recycle Bin is empty ✓", android.widget.Toast.LENGTH_SHORT).show()
        })
        // Web Browser
        list.add(DesktopItem.SystemItem("browser", "Web Browser", "🌐") {
            val maxZ = activeWindows.maxOfOrNull { it.zIndex } ?: 0f
            activeWindows = activeWindows + WindowInfo(
                app = null,
                type = "BROWSER",
                zIndex = maxZ + 1f
            )
        })
        // Settings
        list.add(DesktopItem.SystemItem("settings", "Control Panel", "⚙️") {
            onSettingsOpen()
        })
        // All other apps
        apps.forEach { list.add(DesktopItem.AppItem(it)) }
        list
    }

    // Chunk into columns of 6 items each
    val itemsPerColumn = 6
    val chunkedItems = remember(desktopItems) {
        desktopItems.chunked(itemsPerColumn)
    }

    // Sort apps for start menu
    val filteredApps = remember(apps, startSearchQuery) {
        if (startSearchQuery.isBlank()) apps
        else apps.filter { it.label.contains(startSearchQuery, ignoreCase = true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (useSystemWallpaper) {
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.20f),
                            Color.Black.copy(alpha = 0.45f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0F0B25),
                            Color(0xFF1B0F3A),
                            Color(0xFF0A071A)
                        )
                    )
                }
            )
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    startMenuOpen = false
                })
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -25) {
                        onOpenDrawer()
                    }
                }
            }
    ) {
        // Horizontally scrolling columns of desktop icons
        LazyRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(chunkedItems.size) { colIndex ->
                val columnList = chunkedItems[colIndex]
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.width(68.dp)
                ) {
                    columnList.forEach { item ->
                        when (item) {
                            is DesktopItem.SystemItem -> {
                                DesktopIcon(item.emoji, item.label) {
                                    item.onClick()
                                }
                            }
                            is DesktopItem.AppItem -> {
                                DesktopAppIcon(item.app) {
                                    activeWindows = openOrFocusWindow(activeWindows, item.app)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active floating windows
        activeWindows.forEach { window ->
            if (!window.isMinimized) {
                key(window.id) {
                    val isFocused = activeWindows.maxByOrNull { it.zIndex }?.id == window.id
                    FloatingWindow(
                        window = window,
                        isFocused = isFocused,
                        onFocus = {
                            val current = activeWindows.toMutableList()
                            val idx = current.indexOfFirst { it.id == window.id }
                            if (idx != -1) {
                                val win = current.removeAt(idx)
                                val maxZ = current.maxOfOrNull { it.zIndex } ?: 0f
                                current.add(win.copy(zIndex = maxZ + 1f))
                                activeWindows = current
                            }
                        },
                        onMinimize = {
                            activeWindows = activeWindows.map {
                                if (it.id == window.id) it.copy(isMinimized = true) else it
                            }
                        },
                        onMaximizeToggle = {
                            activeWindows = activeWindows.map {
                                if (it.id == window.id) it.copy(isMaximized = !it.isMaximized) else it
                            }
                        },
                        onClose = {
                            activeWindows = activeWindows.filter { it.id != window.id }
                        },
                        onDrag = { dragAmount ->
                            activeWindows = activeWindows.map {
                                if (it.id == window.id) {
                                    val newOffset = it.offset + dragAmount
                                    it.copy(offset = newOffset)
                                } else it
                            }
                        },
                        onResize = { dragAmount ->
                            activeWindows = activeWindows.map {
                                if (it.id == window.id) {
                                    val dx = with(density) { dragAmount.x.toDp() }
                                    val dy = with(density) { dragAmount.y.toDp() }
                                    val newW = (it.size.width + dx).coerceAtLeast(240.dp)
                                    val newH = (it.size.height + dy).coerceAtLeast(300.dp)
                                    it.copy(size = DpSize(newW, newH))
                                } else it
                            }
                        },
                        onFolderNavigate = { folderName, pathName ->
                            activeWindows = activeWindows.map {
                                if (it.id == window.id) {
                                    it.copy(currentFolder = folderName, currentPath = pathName)
                                } else it
                            }
                        }
                    )
                }
            }
        }

        // Windows 11-like Centered Taskbar at the bottom (Glassmorphic look)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(60.dp)
                .background(Color(0xD90D0B18))
                .border(1.dp, Color.White.copy(alpha = 0.12f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Taskbar center panel (Windows 11 Layout)
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Start Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (startMenuOpen) Color(0xFF6C4DF6).copy(alpha = 0.25f) else Color.Transparent)
                                .clickable { startMenuOpen = !startMenuOpen }
                                .border(1.dp, if (startMenuOpen) Color(0xFF9D86FF) else Color.Transparent, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🪟", fontSize = 20.sp)
                        }

                        // Search Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    startMenuOpen = true
                                    startSearchQuery = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Search, "Search", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        // Web Browser
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val maxZ = activeWindows.maxOfOrNull { it.zIndex } ?: 0f
                                    activeWindows = activeWindows + WindowInfo(
                                        app = null,
                                        type = "BROWSER",
                                        zIndex = maxZ + 1f
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🌐", fontSize = 20.sp)
                        }

                        // Explorer
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val maxZ = activeWindows.maxOfOrNull { it.zIndex } ?: 0f
                                    val storageRoot = Environment.getExternalStorageDirectory().absolutePath
                                    activeWindows = activeWindows + WindowInfo(
                                        app = null,
                                        type = "EXPLORER",
                                        currentFolder = "This PC",
                                        currentPath = storageRoot,
                                        zIndex = maxZ + 1f
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📁", fontSize = 20.sp)
                        }

                        // Divider
                        Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color.White.copy(alpha = 0.2f)))

                        // Running tasks
                        activeWindows.forEach { win ->
                            val isFocused = activeWindows.maxByOrNull { it.zIndex }?.id == win.id && !win.isMinimized
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isFocused) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                                    .border(1.dp, if (isFocused) Color(0xFF9D86FF) else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable {
                                        val current = activeWindows.toMutableList()
                                        val idx = current.indexOfFirst { it.id == win.id }
                                        if (idx != -1) {
                                            val w = current[idx]
                                            if (w.isMinimized) {
                                                val maxZ = current.maxOfOrNull { it.zIndex } ?: 0f
                                                current[idx] = w.copy(isMinimized = false, zIndex = maxZ + 1f)
                                            } else if (isFocused) {
                                                current[idx] = w.copy(isMinimized = true)
                                            } else {
                                                val maxZ = current.maxOfOrNull { it.zIndex } ?: 0f
                                                current[idx] = w.copy(zIndex = maxZ + 1f)
                                            }
                                            activeWindows = current
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    if (win.type == "BROWSER") {
                                        Text("🌐", fontSize = 16.sp)
                                    } else if (win.type == "EXPLORER") {
                                        Text("📁", fontSize = 16.sp)
                                    } else if (win.app != null) {
                                        Image(
                                            painter = rememberDrawablePainter(win.app.icon),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(modifier = Modifier.width(12.dp).height(2.dp).background(if (isFocused) Color(0xFF9D86FF) else Color.White.copy(alpha = 0.4f)))
                                }
                            }
                        }
                    }

                    // System tray clock & tray items
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val systemTime = remember { mutableStateOf("") }
                        LaunchedEffect(Unit) {
                            while(true) {
                                val now = java.util.Calendar.getInstance()
                                systemTime.value = android.text.format.DateFormat.getTimeFormat(context).format(now.time)
                                kotlinx.coroutines.delay(1000)
                            }
                        }

                        Text("📶", fontSize = 11.sp)
                        Text("🔋 ⚡", fontSize = 11.sp, color = Color(0xFF66E08F))
                        Text(
                            text = systemTime.value,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Start Menu Overlay (Windows 11 Styled floating bottom menu) with smooth slide animation
        AnimatedVisibility(
            visible = startMenuOpen,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 68.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(360.dp)
                    .height(460.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xF2110E21))
                    .border(1.dp, Color(0xFF6C4DF6).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {}) // Prevent dismiss when tapping inside
                    }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Search box
                    OutlinedTextField(
                        value = startSearchQuery,
                        onValueChange = { startSearchQuery = it },
                        placeholder = { Text("Search programs...", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF9D86FF),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.White.copy(alpha = 0.03f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
                        ),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, "Search", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp)) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("All Programs", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))

                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        // Quick icons sidebar (Avatar, settings, power)
                        Column(
                            modifier = Modifier
                                .width(48.dp)
                                .fillMaxHeight()
                                .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("👤", fontSize = 18.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                onSettingsOpen()
                                startMenuOpen = false
                            }) {
                                Icon(Icons.Filled.Settings, "Settings", tint = Color.White.copy(alpha = 0.6f))
                            }
                            IconButton(onClick = {
                                onLockScreenTrigger()
                                startMenuOpen = false
                            }) {
                                Text("🔒", fontSize = 16.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Grid of all programs
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            if (filteredApps.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No programs found", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                                }
                            } else {
                                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(filteredApps.size) { index ->
                                        val app = filteredApps[index]
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    activeWindows = openOrFocusWindow(activeWindows, app)
                                                    startMenuOpen = false
                                                }
                                                .padding(4.dp)
                                        ) {
                                            Image(
                                                painter = rememberDrawablePainter(app.icon),
                                                contentDescription = null,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = app.label,
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingWindow(
    window: WindowInfo,
    isFocused: Boolean,
    onFocus: () -> Unit,
    onMinimize: () -> Unit,
    onMaximizeToggle: () -> Unit,
    onClose: () -> Unit,
    onDrag: (androidx.compose.ui.geometry.Offset) -> Unit,
    onResize: (androidx.compose.ui.geometry.Offset) -> Unit,
    onFolderNavigate: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current

    // Resolve offset and size depending on Maximized state
    val modifier = if (window.isMaximized) {
        Modifier
            .fillMaxSize()
            .padding(bottom = 60.dp) // Leave taskbar visible
    } else {
        Modifier
            .offset(
                x = with(LocalDensity.current) { window.offset.x.toDp() },
                y = with(LocalDensity.current) { window.offset.y.toDp() }
            )
            .size(window.size)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xF7161426)),
        shape = if (window.isMaximized) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.2.dp,
            color = if (isFocused) Color(0xFF9D86FF) else Color(0xFF6C4DF6).copy(alpha = 0.4f)
        ),
        modifier = modifier
            .shadow(
                elevation = if (isFocused) 16.dp else 4.dp,
                shape = if (window.isMaximized) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(onPress = { onFocus() })
            }
            .zIndex(window.zIndex)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Title bar (Windows-like layout with glass header)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .background(Color(0xE6201C35))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { onFocus() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (!window.isMaximized) {
                                        onDrag(dragAmount)
                                    }
                                }
                            )
                        }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (window.type == "BROWSER") {
                            Text("🌐", fontSize = 14.sp)
                            Text(
                                text = "Aura Web Browser",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (window.type == "EXPLORER") {
                            Text("📁", fontSize = 14.sp)
                            Text(
                                text = "File Explorer - ${window.currentFolder}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (window.app != null) {
                            Image(
                                painter = rememberDrawablePainter(window.app.icon),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = window.app.label,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Windows-like Min, Max, Close Buttons (Fluent style)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Minimize (Yellow dot style)
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFC107))
                                .clickable { onMinimize() }
                        )
                        // Maximize (Green dot style)
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF26A69A))
                                .clickable { onMaximizeToggle() }
                        )
                        // Close (Red dot style)
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF5252))
                                .clickable { onClose() }
                        )
                    }
                }

                // Window Content Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF0A0718))
                ) {
                    if (window.type == "BROWSER") {
                        Column(modifier = Modifier.fillMaxSize()) {
                            var urlInput by remember { mutableStateOf(window.currentUrl) }
                            var webViewInstance by remember { mutableStateOf<android.webkit.WebView?>(null) }
                            var isLoading by remember { mutableStateOf(false) }

                            // Address bar row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1B1736))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Navigation Buttons
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { webViewInstance?.let { if (it.canGoBack()) it.goBack() } },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text("←", color = Color.White, fontSize = 14.sp)
                                    }
                                    IconButton(
                                        onClick = { webViewInstance?.let { if (it.canGoForward()) it.goForward() } },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text("→", color = Color.White, fontSize = 14.sp)
                                    }
                                    IconButton(
                                        onClick = { webViewInstance?.reload() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text("↻", color = Color.White, fontSize = 12.sp)
                                    }
                                }

                                // URL text field
                                androidx.compose.foundation.text.BasicTextField(
                                    value = urlInput,
                                    onValueChange = { urlInput = it },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        imeAction = androidx.compose.ui.text.input.ImeAction.Go
                                    ),
                                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                        onGo = {
                                            var formattedUrl = urlInput.trim()
                                            if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                                                formattedUrl = "https://$formattedUrl"
                                            }
                                            urlInput = formattedUrl
                                            window.currentUrl = formattedUrl
                                            webViewInstance?.loadUrl(formattedUrl)
                                        }
                                    ),
                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                )

                                // Go Button
                                Button(
                                    onClick = {
                                        var formattedUrl = urlInput.trim()
                                        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                                            formattedUrl = "https://$formattedUrl"
                                        }
                                        urlInput = formattedUrl
                                        window.currentUrl = formattedUrl
                                        webViewInstance?.loadUrl(formattedUrl)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C4DF6)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text("Go", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Dynamic Linear Progress Loader
                            if (isLoading) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().height(2.dp),
                                    color = Color(0xFF9D86FF),
                                    trackColor = Color.Transparent
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.White.copy(alpha = 0.05f)))
                            }

                            // WebView area
                            AndroidView(
                                factory = { ctx ->
                                    android.webkit.WebView(ctx).apply {
                                        webViewClient = object : android.webkit.WebViewClient() {
                                            override fun onPageStarted(view: android.webkit.WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                                super.onPageStarted(view, url, favicon)
                                                isLoading = true
                                                if (url != null) urlInput = url
                                            }
                                            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                                super.onPageFinished(view, url)
                                                isLoading = false
                                            }
                                        }
                                        settings.apply {
                                            javaScriptEnabled = true
                                            domStorageEnabled = true
                                            builtInZoomControls = true
                                            displayZoomControls = false
                                            useWideViewPort = true
                                            loadWithOverviewMode = true
                                            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                                        }
                                        loadUrl(window.currentUrl)
                                        webViewInstance = this
                                    }
                                },
                                update = { webView ->
                                    // Can add dynamic updates here
                                },
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            )
                        }
                    } else if (window.type == "EXPLORER") {
                        val hasStoragePerm = checkStoragePermission(context)
                        if (!hasStoragePerm) {
                            // Permission Prompt View
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Text("📁", fontSize = 48.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Storage Access Required",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "To browse and open files on your device like a real PC, Aura needs All Files Access permission.",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 11.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { requestStoragePermission(context) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C4DF6)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Grant Permission", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            val storageRoot = Environment.getExternalStorageDirectory().absolutePath
                            // Initialize currentPath if empty
                            LaunchedEffect(window.currentPath) {
                                if (window.currentPath.isEmpty()) {
                                    onFolderNavigate("This PC", storageRoot)
                                }
                            }

                            Row(modifier = Modifier.fillMaxSize()) {
                                // Sidebar list
                                Column(
                                    modifier = Modifier
                                        .width(80.dp)
                                        .fillMaxHeight()
                                        .background(Color.White.copy(alpha = 0.02f))
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    SidebarItem("💻", "This PC", window.currentFolder == "This PC") {
                                        onFolderNavigate("This PC", storageRoot)
                                    }
                                    SidebarItem("⬇️", "Downloads", window.currentFolder == "Downloads") {
                                        onFolderNavigate("Downloads", "$storageRoot/Download")
                                    }
                                    SidebarItem("📁", "Docs", window.currentFolder == "Documents") {
                                        onFolderNavigate("Documents", "$storageRoot/Documents")
                                    }
                                    SidebarItem("🖼️", "Pictures", window.currentFolder == "Pictures") {
                                        onFolderNavigate("Pictures", "$storageRoot/Pictures")
                                    }
                                    SidebarItem("🎵", "Music", window.currentFolder == "Music") {
                                        onFolderNavigate("Music", "$storageRoot/Music")
                                    }
                                    SidebarItem("🎥", "Videos", window.currentFolder == "Videos") {
                                        onFolderNavigate("Videos", "$storageRoot/Movies")
                                    }
                                }

                                // Explorer Main Directory grid
                                Column(modifier = Modifier.weight(1f).padding(8.dp)) {
                                    // Toolbar: Back arrow + Address bar
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    ) {
                                        val canGoUp = window.currentFolder != "This PC" && window.currentPath != storageRoot
                                        IconButton(
                                            onClick = {
                                                val currentDir = java.io.File(window.currentPath)
                                                val parent = currentDir.parentFile
                                                if (parent != null && parent.absolutePath.startsWith(storageRoot)) {
                                                    onFolderNavigate(parent.name, parent.absolutePath)
                                                } else {
                                                    onFolderNavigate("This PC", storageRoot)
                                                }
                                            },
                                            enabled = canGoUp,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Text("↑", color = if (canGoUp) Color.White else Color.White.copy(alpha = 0.2f), fontSize = 14.sp)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            val displayPath = if (window.currentFolder == "This PC") "This PC" else window.currentPath.replace(storageRoot, "This PC")
                                            Text(displayPath, color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }

                                    if (window.currentFolder == "This PC") {
                                        // Local folders on This PC with premium style
                                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            item { ExplorerFolderItem("📁", "Documents") { onFolderNavigate("Documents", "$storageRoot/Documents") } }
                                            item { ExplorerFolderItem("📥", "Downloads") { onFolderNavigate("Downloads", "$storageRoot/Download") } }
                                            item { ExplorerFolderItem("🖼️", "Pictures") { onFolderNavigate("Pictures", "$storageRoot/Pictures") } }
                                            item { ExplorerFolderItem("🎵", "Music") { onFolderNavigate("Music", "$storageRoot/Music") } }
                                            item { ExplorerFolderItem("🎬", "Videos") { onFolderNavigate("Videos", "$storageRoot/Movies") } }
                                            item { ExplorerFolderItem("💾", "Local Disk (C:)") { onFolderNavigate("Local Disk (C:)", storageRoot) } }
                                        }
                                    } else {
                                        // Query actual folders and files on local storage
                                        val filesList = remember(window.currentPath) {
                                            runCatching {
                                                if (window.currentPath.isEmpty()) emptyList()
                                                else {
                                                    val dir = java.io.File(window.currentPath)
                                                    if (!dir.exists()) dir.mkdirs()
                                                    dir.listFiles()?.filter { !it.name.startsWith(".") }?.sortedWith(
                                                        compareBy<java.io.File> { !it.isDirectory }.thenBy { it.name.lowercase() }
                                                    ) ?: emptyList()
                                                }
                                            }.getOrDefault(emptyList())
                                        }

                                        if (filesList.isEmpty()) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text("Folder is empty", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                                            }
                                        } else {
                                            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                                                modifier = Modifier.fillMaxSize(),
                                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                items(filesList.size) { index ->
                                                    val file = filesList[index]
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color.White.copy(alpha = 0.02f))
                                                            .clickable {
                                                                if (file.isDirectory) {
                                                                    onFolderNavigate(file.name, file.absolutePath)
                                                                } else {
                                                                    openActualFile(context, file)
                                                                }
                                                            }
                                                            .padding(6.dp)
                                                    ) {
                                                        val emoji = if (file.isDirectory) "📁" else getFileEmoji(file)
                                                        Text(emoji, fontSize = 28.sp)
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = file.name,
                                                            color = Color.White,
                                                            fontSize = 9.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (window.app != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    AppRepository.launchApp(context, window.app)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Image(
                                    painter = rememberDrawablePainter(window.app.icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Tap inside window to run",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { AppRepository.launchApp(context, window.app) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C4DF6)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Run Fullscreen ⚡", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Resize Handle at bottom right (Only if not maximized)
            if (!window.isMaximized) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(16.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { onFocus() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    onResize(dragAmount)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("◢", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SidebarItem(
    emoji: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color.White.copy(alpha = 0.08f) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Text(emoji, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, color = if (isSelected) Color(0xFF9D86FF) else Color.White.copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ExplorerFolderItem(
    emoji: String,
    name: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .clickable { onClick() }
            .padding(10.dp)
            .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(10.dp))
    ) {
        Text(emoji, fontSize = 22.sp)
        Text(name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun DesktopIcon(
    emoji: String,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = TextStyle(
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.85f),
                    offset = Offset(2f, 2f),
                    blurRadius = 6f
                )
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun DesktopAppIcon(
    app: AppInfo,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberDrawablePainter(app.icon),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.label,
            style = TextStyle(
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.85f),
                    offset = Offset(2f, 2f),
                    blurRadius = 6f
                )
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

fun openOrFocusWindow(currentList: List<WindowInfo>, app: AppInfo): List<WindowInfo> {
    val list = currentList.toMutableList()
    val idx = list.indexOfFirst { it.app?.packageName == app.packageName }
    val maxZ = list.maxOfOrNull { it.zIndex } ?: 0f

    if (idx != -1) {
        // Bring to focus & restore
        val win = list.removeAt(idx)
        list.add(win.copy(isMinimized = false, zIndex = maxZ + 1f))
    } else {
        // Create new window slightly shifted to avoid direct overlap
        val offsetVal = 100f + (list.size * 30) % 300f
        list.add(
            WindowInfo(
                app = app,
                initialOffset = androidx.compose.ui.geometry.Offset(offsetVal, offsetVal),
                zIndex = maxZ + 1f
            )
        )
    }
    return list
}


