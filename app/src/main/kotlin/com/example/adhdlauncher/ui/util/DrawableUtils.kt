package com.example.adhdlauncher.ui.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.adhdlauncher.data.IconStyle

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
    } catch (_: Exception) {
        null
    }
}

/**
 * 外部アイコンパック無しで全アプリを Material You（単色・低刺激トーン）へシステム変換する
 * - Android 13 (API 33)+ で AdaptiveIconDrawable.monochrome が存在する場合は公式モノクロレイヤーを使用
 * - 存在しないアプリも輝度抽出 + カラートーン合成フィルタにより 100% 統一トーンへ変換
 */
fun Drawable?.toThemedImageBitmapOrNull(
    style: IconStyle,
    maxSizePx: Int = 144
): ImageBitmap? {
    if (this == null) return null
    if (style == IconStyle.ORIGINAL) return toImageBitmapOrNull(maxSizePx)

    val (bgColor, fgColor) = when (style) {
        IconStyle.MATERIAL_SAGE -> Color.rgb(0x23, 0x2B, 0x22) to Color.rgb(0xB8, 0xCC, 0xAE)
        IconStyle.MATERIAL_CYAN -> Color.rgb(0x18, 0x24, 0x30) to Color.rgb(0x90, 0xCA, 0xF9)
        IconStyle.MONOCHROME -> Color.rgb(0x1E, 0x1F, 0x22) to Color.rgb(0xE2, 0xE4, 0xE8)
        IconStyle.ORIGINAL -> return toImageBitmapOrNull(maxSizePx)
    }

    return try {
        val outBitmap = Bitmap.createBitmap(maxSizePx, maxSizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)

        // 1. 丸角スクワークル背景を描画
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            this.style = Paint.Style.FILL
        }
        val radius = maxSizePx * 0.26f
        canvas.drawRoundRect(
            RectF(0f, 0f, maxSizePx.toFloat(), maxSizePx.toFloat()),
            radius,
            radius,
            bgPaint
        )

        // 2. Android 13+ のネイティブ Monochrome レイヤーがあれば優先利用
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && this is AdaptiveIconDrawable) {
            val mono = this.monochrome
            if (mono != null) {
                val inset = (maxSizePx * -0.18f).toInt()
                val mutated = mono.mutate()
                mutated.colorFilter = PorterDuffColorFilter(fgColor, PorterDuff.Mode.SRC_IN)
                mutated.setBounds(inset, inset, maxSizePx - inset, maxSizePx - inset)
                mutated.draw(canvas)
                return outBitmap.asImageBitmap()
            }
        }

        // 3. フォールバック：元のアイコンを一旦描画し、彩度0＋コントラスト強調＋テーマカラー乗算でMaterial You風に変換
        val srcBitmap = Bitmap.createBitmap(maxSizePx, maxSizePx, Bitmap.Config.ARGB_8888)
        val srcCanvas = Canvas(srcBitmap)
        val pad = (maxSizePx * 0.14f).toInt()
        setBounds(pad, pad, maxSizePx - pad, maxSizePx - pad)
        draw(srcCanvas)

        val r = Color.red(fgColor) / 255f
        val g = Color.green(fgColor) / 255f
        val b = Color.blue(fgColor) / 255f

        // 輝度抽出 + コントラスト調整 + テーマ色ティント行列
        val lumR = 0.299f * 1.15f
        val lumG = 0.587f * 1.15f
        val lumB = 0.114f * 1.15f
        val matrix = ColorMatrix(
            floatArrayOf(
                lumR * r, lumG * r, lumB * r, 0f, 12f * r,
                lumR * g, lumG * g, lumB * g, 0f, 12f * g,
                lumR * b, lumG * b, lumB * b, 0f, 12f * b,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(srcBitmap, 0f, 0f, paint)
        outBitmap.asImageBitmap()
    } catch (_: Exception) {
        toImageBitmapOrNull(maxSizePx)
    }
}
