package com.aura.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WallpaperSelector — Custom AI Wallpapers chunne ka premium widget.
 * Previews are loaded asynchronously in the background.
 */
@Composable
fun WallpaperSelector(onApplied: () -> Unit = {}) {
    val context = LocalContext.current
    val wallpapers = remember { WallpaperHelper.ASSET_WALLPAPERS }
    val loadedPreviews = remember { mutableStateMapOf<String, Bitmap>() }

    // Load previews in background to avoid UI stutter
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            wallpapers.forEach { wp ->
                val bmp = loadPreview(context, wp.assetPath)
                if (bmp != null) {
                    loadedPreviews[wp.id] = bmp
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "AI Premium Wallpapers",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(wallpapers, key = { it.id }) { wp ->
                val preview = loadedPreviews[wp.id]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(96.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(96.dp)
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = {
                                    val ok = WallpaperHelper.setAssetWallpaper(context, wp)
                                    if (ok) {
                                        Toast.makeText(context, "Wallpaper applied successfully ✓", Toast.LENGTH_SHORT).show()
                                        onApplied()
                                    } else {
                                        Toast.makeText(context, "Failed to apply wallpaper", Toast.LENGTH_SHORT).show()
                                    }
                                })
                            }
                    ) {
                        if (preview != null) {
                            Image(
                                bitmap = preview.asImageBitmap(),
                                contentDescription = wp.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFF9D86FF),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = wp.name,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        }
    }
}

private fun loadPreview(context: Context, assetPath: String): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.assets.open(assetPath).use {
            BitmapFactory.decodeStream(it, null, options)
        }
        // Scale down to ~128x200 to save memory for previews
        options.inSampleSize = calculateInSampleSize(options, 128, 200)
        options.inJustDecodeBounds = false
        context.assets.open(assetPath).use {
            BitmapFactory.decodeStream(it, null, options)
        }
    } catch (e: Exception) {
        null
    }
}

private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int
): Int {
    val (height: Int, width: Int) = options.outHeight to options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}
