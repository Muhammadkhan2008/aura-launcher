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
    pageCount: Int = 1
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var menuOpen by remember { mutableStateOf(false) }
    val prefs = remember { AuraPrefs(context) }
    var badgeCount by remember { mutableStateOf(0) }
    var shortcuts by remember { mutableStateOf<List<ShortcutHelper.AuraShortcut>>(emptyList()) }
    var pageStatuses by remember { mutableStateOf(List(maxOf(0, pageCount - 1)) { false }) }

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
                        pageStatuses = List(maxOf(0, pageCount - 1)) { idx ->
                            HomePageManager.getPageApps(context, idx + 1).contains(app.packageName)
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
                    modifier = Modifier.size(iconSize.dp).clip(RoundedCornerShape(12.dp))
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
                            color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            if (showLabel) {
                Spacer(Modifier.height(6.dp))
                Text(
                    app.label, color = Color.White, fontSize = 12.sp, maxLines = 1,
                    textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis
                )
            }
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            Text(app.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            shortcuts.forEach { shortcut ->
                DropdownMenuItem(
                    text = { Text(shortcut.label, fontSize = 14.sp) },
                    onClick = { ShortcutHelper.launch(context, shortcut); menuOpen = false }
                )
            }

            if (shortcuts.isNotEmpty()) HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            val isFav = prefs.isFavorite(app.packageName)
            DropdownMenuItem(
                text = { Text(if (isFav) "Remove from dock" else "Add to dock") },
                leadingIcon = { Icon(Icons.Filled.Star, null, tint = Color(0xFF9D86FF)) },
                onClick = {
                    if (isFav) prefs.removeFavorite(app.packageName) else prefs.addFavorite(app.packageName)
                    menuOpen = false
                }
            )
            DropdownMenuItem(
                text = { Text("App info") },
                leadingIcon = { Icon(Icons.Filled.Info, null, tint = Color(0xFF9D86FF)) },
                onClick = { AppRepository.openAppInfo(context, app.packageName); menuOpen = false }
            )
            DropdownMenuItem(
                text = { Text("Uninstall") },
                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Color(0xFFFF4444)) },
                onClick = { AppRepository.uninstallApp(context, app.packageName); menuOpen = false }
            )

            if (pageCount > 1) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                pageStatuses.forEachIndexed { idx, isOnPage ->
                    val pageDisplay = idx + 2
                    val pageIndex = idx + 1
                    DropdownMenuItem(
                        text = { Text(if (isOnPage) "Remove from Page $pageDisplay" else "Add to Page $pageDisplay") },
                        leadingIcon = { Icon(if (isOnPage) Icons.Filled.Delete else Icons.Filled.Add, null,
                            tint = if (isOnPage) Color(0xFFFF4444) else Color(0xFF9D86FF)) },
                        onClick = {
                            if (isOnPage) HomePageManager.removeAppFromPage(context, pageIndex, app.packageName)
                            else HomePageManager.addAppToPage(context, pageIndex, app.packageName)
                            pageStatuses = pageStatuses.toMutableList().also { it[idx] = !isOnPage }
                            menuOpen = false
                        }
                    )
                }
            }
        }
    }
}
