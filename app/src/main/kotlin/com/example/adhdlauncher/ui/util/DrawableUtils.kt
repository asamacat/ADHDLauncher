package com.example.adhdlauncher.ui.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

fun Drawable?.toImageBitmapOrNull(maxSizePx: Int = 144): ImageBitmap? {
    if (this == null) return null
    return try {
        val rawWidth = if (intrinsicWidth > 0) intrinsicWidth else maxSizePx
        val rawHeight = if (intrinsicHeight > 0) intrinsicHeight else maxSizePx
        val width = rawWidth.coerceAtMost(maxSizePx)
        val height = rawHeight.coerceAtMost(maxSizePx)

        if (this is BitmapDrawable && this.bitmap != null && this.bitmap.width <= maxSizePx && this.bitmap.height <= maxSizePx) {
            return this.bitmap.asImageBitmap()
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}
