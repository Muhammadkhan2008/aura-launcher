package com.aura.launcher

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 * WallpaperHelper — Aura ke apne premium gradient wallpapers banata
 * aur set karta hai. Koi internet/file nahi — gradients code se bante hain.
 *
 * User boring system wallpaper ki jagah ye premium gradients laga sakta hai.
 */
object WallpaperHelper {

    /**
     * AuraWallpaper — ek built-in gradient wallpaper.
     * colors = gradient ke do/teen rang.
     */
    data class AuraWallpaper(
        val id: String,
        val name: String,
        val colors: List<Int>
    )

    /** Built-in premium gradient wallpapers (Aura ke apne). */
    val WALLPAPERS: List<AuraWallpaper> = listOf(
        AuraWallpaper("aurora", "Aurora", listOf(0xFF0F0C29.toInt(), 0xFF302B63.toInt(), 0xFF24243E.toInt())),
        AuraWallpaper("sunset", "Sunset", listOf(0xFFFF512F.toInt(), 0xFFDD2476.toInt())),
        AuraWallpaper("ocean", "Ocean", listOf(0xFF2193B0.toInt(), 0xFF6DD5ED.toInt())),
        AuraWallpaper("violet", "Violet Dream", listOf(0xFF6C4DF6.toInt(), 0xFF9D86FF.toInt(), 0xFF2B1F4E.toInt())),
        AuraWallpaper("forest", "Forest", listOf(0xFF134E5E.toInt(), 0xFF71B280.toInt())),
        AuraWallpaper("midnight", "Midnight", listOf(0xFF000000.toInt(), 0xFF1A1A2E.toInt(), 0xFF16213E.toInt())),
        AuraWallpaper("peach", "Peach", listOf(0xFFFFB347.toInt(), 0xFFFFCC80.toInt())),
        AuraWallpaper("cosmic", "Cosmic", listOf(0xFF8E2DE2.toInt(), 0xFF4A00E0.toInt()))
    )

    fun byId(id: String): AuraWallpaper? = WALLPAPERS.firstOrNull { it.id == id }

    /**
     * Gradient ko bitmap banao (screen size ka).
     */
    private fun renderBitmap(context: Context, wp: AuraWallpaper): Bitmap {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            .defaultDisplay.getRealMetrics(metrics)
        val w = metrics.widthPixels.coerceAtLeast(720)
        val h = metrics.heightPixels.coerceAtLeast(1280)

        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val positions = FloatArray(wp.colors.size) { i ->
            if (wp.colors.size == 1) 0f else i.toFloat() / (wp.colors.size - 1)
        }
        val shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            wp.colors.toIntArray(), positions, Shader.TileMode.CLAMP
        )
        canvas.drawPaint(Paint().apply { this.shader = shader })
        return bmp
    }

    /**
     * Wallpaper ko system pe set karo (home + lock screen).
     * @return true agar set ho gaya.
     */
    fun setWallpaper(context: Context, wp: AuraWallpaper): Boolean {
        return runCatching {
            val bmp = renderBitmap(context, wp)
            val wm = WallpaperManager.getInstance(context)
            wm.setBitmap(bmp)
            true
        }.getOrDefault(false)
    }
}
