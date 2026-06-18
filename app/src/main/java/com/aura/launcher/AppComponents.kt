package com.aura.launcher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIcon(
    app: AppInfo,
    iconSize: Int = 56,
    showLabel: Boolean = true,
    onClick: () -> Unit,
    onHideToggled: (() -> Unit)? = null   // drawer ko refresh karne ke liye
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var menuOpen by remember { mutableStateOf(false) }
    val prefs = remember { AuraPrefs(context) }
    var badgeCount by remember { mutableStateOf(0) }
    var shortcuts by remember { mutableStateOf<List<ShortcutHelper.AuraShortcut>>(emptyList()) }
    var pageCount by remember { mutableStateOf(1) }
    var pageStatuses by remember { mutableStateOf(emptyList<Boolean>()) }
    var hiddenState by remember { mutableStateOf(prefs.isHidden(app.packageName)) }

    LaunchedEffect(app.packageName) {
        badgeCount = NotificationBadgeHelper.getNotificationCount(context, app.packageName)
    }

    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        shortcuts = ShortcutHelper.getShortcuts(context, app.packageName)
                        pageCount = HomePageManager.getPageCount(context)
                        if (pageCount > 1) {
                            pageStatuses = (1 until pageCount).map { p ->
                                HomePageManager.getPageApps(context, p).contains(app.packageName)
                            }
                        }
                        menuOpen = true
                    }
                )
                .padding(vertical = 10.dp, horizontal = 4.dp)
        ) {
            Box {
                coil.compose.AsyncImage(
                    model = app.icon,
                    contentDescription = app.label,
                    modifier = Modifier
                        .size(iconSize.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.TopEnd)
                            .background(Color(0xFFFF4444), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (badgeCount > 9) "9+" else badgeCount.toString(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            if (showLabel) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = app.label,
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Long-press popup menu (shortcuts + actions)
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false }
        ) {
            // App name header
            Text(
                app.label,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Dynamic shortcuts (WhatsApp → New Message, etc.)
            shortcuts.forEach { shortcut ->
                DropdownMenuItem(
                    text = { Text(shortcut.label, fontSize = 14.sp) },
                    leadingIcon = {
                        if (shortcut.icon != null) {
                            coil.compose.AsyncImage(
                                model = shortcut.icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    onClick = {
                        ShortcutHelper.launch(context, shortcut)
                        menuOpen = false
                    }
                )
            }

            if (shortcuts.isNotEmpty()) HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Standard actions
            val isFav = prefs.isFavorite(app.packageName)
            DropdownMenuItem(
                text = { Text(if (isFav) "Remove from dock" else "Add to dock") },
                leadingIcon = { Icon(Icons.Filled.Star, null, tint = Color(0xFF9D86FF)) },
                onClick = {
                    if (isFav) prefs.removeFavorite(app.packageName)
                    else prefs.addFavorite(app.packageName)
                    menuOpen = false
                }
            )
            DropdownMenuItem(
                text = { Text("App info") },
                leadingIcon = { Icon(Icons.Filled.Info, null, tint = Color(0xFF9D86FF)) },
                onClick = {
                    AppRepository.openAppInfo(context, app.packageName)
                    menuOpen = false
                }
            )
            DropdownMenuItem(
                text = { Text("Uninstall") },
                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Color(0xFFFF4444)) },
                onClick = {
                    AppRepository.uninstallApp(context, app.packageName)
                    menuOpen = false
                }
            )

            // Hide / Unhide app
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            DropdownMenuItem(
                text = { Text(if (hiddenState) "Unhide app" else "Hide app", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        if (hiddenState) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        null,
                        tint = Color(0xFF9D86FF)
                    )
                },
                onClick = {
                    if (hiddenState) prefs.showApp(app.packageName)
                    else prefs.hideApp(app.packageName)
                    hiddenState = !hiddenState
                    onHideToggled?.invoke()
                    menuOpen = false
                }
            )

            // Page pinning — only show if user has more than 1 home page
            if (pageCount > 1) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                pageStatuses.forEachIndexed { idx, isOnPage ->
                    val pageDisplay = idx + 2    // "Page 2", "Page 3" …
                    val pageIndex = idx + 1      // HomePageManager index (1-based)
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (isOnPage) "Remove from Page $pageDisplay"
                                else "Add to Page $pageDisplay",
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (isOnPage) Icons.Filled.Delete else Icons.Filled.Add,
                                null,
                                tint = if (isOnPage) Color(0xFFFF4444) else Color(0xFF9D86FF)
                            )
                        },
                        onClick = {
                            if (isOnPage) HomePageManager.removeAppFromPage(context, pageIndex, app.packageName)
                            else HomePageManager.addAppToPage(context, pageIndex, app.packageName)
                            pageStatuses = pageStatuses.toMutableList().also { it[idx] = !isOnPage }
                        }
                    )
                }
            }
        }
    }
}
