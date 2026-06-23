package com.aura.launcher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIcon(
    app: AppInfo,
    iconSize: Int = 56,
    showLabel: Boolean = true,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    val prefs = remember { AuraPrefs(context) }
    var badgeCount by remember { mutableStateOf(0) }
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    LaunchedEffect(app.packageName) {
        badgeCount = NotificationBadgeHelper.getNotificationCount(context, app.packageName)
    }

    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .combinedClickable(
                    onClick = {
                        view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                        onClick()
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                        menuOpen = true
                    }
                )
                .padding(vertical = 10.dp, horizontal = 4.dp)
        ) {
            Box {
                AsyncImage(
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

        if (menuOpen) {
            val isFav = prefs.isFavorite(app.packageName)
            val isHidden = prefs.isHidden(app.packageName)
            CustomQuickActionsMenu(
                app = app,
                isFav = isFav,
                isHidden = isHidden,
                onDismiss = { menuOpen = false },
                onToggleFav = {
                    if (isFav) prefs.removeFavorite(app.packageName) else prefs.addFavorite(app.packageName)
                },
                onToggleHide = {
                    if (isHidden) prefs.showApp(app.packageName) else prefs.hideApp(app.packageName)
                    menuOpen = false
                },
                onAppInfo = { AppRepository.openAppInfo(context, app.packageName) },
                onUninstall = { AppRepository.uninstallApp(context, app.packageName) }
            )
        }
    }
}

class SmartMenuPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val margin = 24 // Screen edges se safe padding (px)

        // Horizontal (X) Alignment: Try to center relative to the app icon
        var x = anchorBounds.left + (anchorBounds.width / 2) - (popupContentSize.width / 2)

        // Prevent horizontal clipping (Left & Right boundaries)
        if (x < margin) {
            x = margin
        } else if (x + popupContentSize.width > windowSize.width - margin) {
            x = windowSize.width - popupContentSize.width - margin
        }

        // Vertical (Y) Alignment: Prefer below the icon
        var y = anchorBounds.bottom + 16

        // Prevent vertical clipping (Bottom boundary), flip to above the icon if needed
        if (y + popupContentSize.height > windowSize.height - margin) {
            y = anchorBounds.top - popupContentSize.height - 16
        }

        return IntOffset(x, y)
    }
}

@Composable
fun CustomQuickActionsMenu(
    app: AppInfo,
    isFav: Boolean,
    isHidden: Boolean,
    onDismiss: () -> Unit,
    onToggleFav: () -> Unit,
    onToggleHide: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit
) {
    Popup(
        popupPositionProvider = SmartMenuPositionProvider(),
        onDismissRequest = onDismiss,
        properties = PopupProperties(clippingEnabled = false) // Removes default OS boundaries
    ) {
        Box(
            modifier = Modifier
                .width(220.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF231C3D).copy(alpha = 0.95f),
                            Color(0xFF0F0C1E).copy(alpha = 0.85f)
                        ),
                        radius = 400f
                    )
                )
                .padding(8.dp)
        ) {
            Column {
                QuickActionItem(
                    icon = Icons.Filled.Star,
                    text = if (isFav) "Remove from dock" else "Add to dock",
                    onClick = { onToggleFav(); onDismiss() }
                )

                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
                Spacer(modifier = Modifier.height(4.dp))

                QuickActionItem(
                    icon = if (isHidden) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    text = if (isHidden) "Show app" else "Hide app",
                    onClick = { onToggleHide() }
                )

                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
                Spacer(modifier = Modifier.height(4.dp))

                QuickActionItem(
                    icon = Icons.Filled.Info,
                    text = "App info",
                    onClick = { onAppInfo(); onDismiss() }
                )

                QuickActionItem(
                    icon = Icons.Filled.Delete,
                    text = "Uninstall",
                    onClick = { onUninstall(); onDismiss() },
                    isDestructive = true
                )
            }
        }
    }
}

@Composable
fun QuickActionItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val contentColor = if (isDestructive) Color(0xFFFF4444) else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = contentColor.copy(alpha = 0.8f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = contentColor,
            fontSize = 14.sp
        )
    }
}
