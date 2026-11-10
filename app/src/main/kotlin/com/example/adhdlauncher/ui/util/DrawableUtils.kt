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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.adhdlauncher.data.IconStyle
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * ハニカム（正六角形・上下面または左右頂点）シェイプ
 * わずかに角を丸めた美しいハニカムセル形状を生成する
 */
val HexagonShape: Shape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = min(cx, cy) * 0.98f
        // Pointy-top 正六角形（-90度から60度刻み）
        for (i in 0 until 6) {
            val angleRad = Math.toRadians((i * 60.0) - 90.0)
            val x = cx + (r * cos(angleRad)).toFloat()
            val y = cy + (r * sin(angleRad)).toFloat()
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        return Outline.Generic(path)
    }
}

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
 * 1. Android 13 (API 33)+ で AdaptiveIconDrawable.monochrome が存在する場合は公式モノクロレイヤーを使用
 * 2. AdaptiveIconDrawable で monochrome が無い場合は background を除外し foreground のみを抽出して二重枠を防止
 * 3. それ以外のレガシーアイコンは輝度＋コントラスト抽出で単色化
 */
fun Drawable?.toThemedImageBitmapOrNull(
    style: IconStyle,
    maxSizePx: Int = 144
): ImageBitmap? {
    if (this == null) return null
    if (style == IconStyle.ORIGINAL) return toImageBitmapOrNull(maxSizePx)

    val bgColor = (0xFF000000.toInt()) or style.bgHex
    val fgColor = (0xFF000000.toInt()) or style.fgHex

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

        // 2. Android 13+ のネイティブ Monochrome レイヤーがあれば最優先利用
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

        // 3. フォールバック改善：AdaptiveIconDrawable なら background を捨てて foreground のみ抽出（二重枠の解消）
        val targetDrawable: Drawable = if (this is AdaptiveIconDrawable && this.foreground != null) {
            this.foreground
        } else {
            this
        }

        val srcBitmap = Bitmap.createBitmap(maxSizePx, maxSizePx, Bitmap.Config.ARGB_8888)
        val srcCanvas = Canvas(srcBitmap)
        val inset = if (this is AdaptiveIconDrawable) {
            (maxSizePx * -0.12f).toInt()
        } else {
            (maxSizePx * 0.16f).toInt()
        }
        targetDrawable.setBounds(inset, inset, maxSizePx - inset, maxSizePx - inset)
        targetDrawable.draw(srcCanvas)

        val r = Color.red(fgColor) / 255f
        val g = Color.green(fgColor) / 255f
        val b = Color.blue(fgColor) / 255f

        // 輝度抽出 + コントラスト強調 + テーマカラー合成行列
        val lumR = 0.299f * 1.18f
        val lumG = 0.587f * 1.18f
        val lumB = 0.114f * 1.18f
        val matrix = ColorMatrix(
            floatArrayOf(
                lumR * r, lumG * r, lumB * r, 0f, 10f * r,
                lumR * g, lumG * g, lumB * g, 0f, 10f * g,
                lumR * b, lumG * b, lumB * b, 0f, 10f * b,
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
