package com.example.engine.segmentation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import java.util.concurrent.ConcurrentHashMap

object MediaBitmapHelper {
    private val bitmapCache = ConcurrentHashMap<String, Bitmap>()

    /**
     * Creates or retrieves a rich media bitmap with a distinct foreground subject and backdrop.
     */
    fun getMediaBitmap(label: String, width: Int = 480, height: Int = 360): Bitmap {
        val key = "${label}_${width}_${height}"
        return bitmapCache.getOrPut(key) {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 1. Studio/Neon Backdrop (Background to be removed)
            val bgPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    Color.rgb(18, 22, 36), Color.rgb(38, 48, 72),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Background grid lines (to make removal obvious)
            val gridPaint = Paint().apply {
                color = Color.argb(40, 0, 229, 255)
                strokeWidth = 1.5f
            }
            for (x in 0 until width step 30) {
                canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), gridPaint)
            }
            for (y in 0 until height step 30) {
                canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), gridPaint)
            }

            // 2. Foreground Subject (Stylized Portrait / Silhouette with distinct colors)
            val centerX = width / 2f
            val centerY = height / 2f + 20f

            // Body / Shoulders
            val bodyPaint = Paint().apply {
                isAntiAlias = true
                shader = LinearGradient(
                    centerX - 100f, centerY, centerX + 100f, height.toFloat(),
                    Color.rgb(139, 92, 246), Color.rgb(236, 72, 153),
                    Shader.TileMode.CLAMP
                )
            }
            val bodyPath = Path().apply {
                moveTo(centerX - 90f, height.toFloat())
                quadTo(centerX - 80f, centerY + 50f, centerX, centerY + 40f)
                quadTo(centerX + 80f, centerY + 50f, centerX + 90f, height.toFloat())
                close()
            }
            canvas.drawPath(bodyPath, bodyPaint)

            // Head / Face
            val headPaint = Paint().apply {
                isAntiAlias = true
                shader = RadialGradient(
                    centerX, centerY - 25f, 60f,
                    Color.rgb(254, 215, 170), Color.rgb(251, 146, 60),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(centerX, centerY - 25f, 48f, headPaint)

            // Hair / Sunglasses / Cyberpunk Accents
            val hairPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(15, 23, 42)
            }
            canvas.drawArc(
                centerX - 52f, centerY - 76f, centerX + 52f, centerY + 10f,
                180f, 180f, true, hairPaint
            )

            // Sunglasses (Electric Cyan)
            val glassesPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(0, 229, 255)
            }
            canvas.drawRoundRect(centerX - 35f, centerY - 32f, centerX - 5f, centerY - 18f, 4f, 4f, glassesPaint)
            canvas.drawRoundRect(centerX + 5f, centerY - 32f, centerX + 35f, centerY - 18f, 4f, 4f, glassesPaint)
            val bridgePaint = Paint().apply {
                color = Color.rgb(0, 229, 255)
                strokeWidth = 3f
            }
            canvas.drawLine(centerX - 5f, centerY - 25f, centerX + 5f, centerY - 25f, bridgePaint)

            // Foreground Label Badge
            val labelPaint = Paint().apply {
                isAntiAlias = true
                color = Color.WHITE
                textSize = 18f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(label, centerX, height - 16f, labelPaint)

            bitmap
        }
    }
}
