package com.yangyws.dualscreenbrowser.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View

/** Draws a crisp arrow mouse pointer with tip at (x, y). */
fun drawArrowCursor(
    canvas: Canvas,
    x: Float,
    y: Float,
    sizePx: Float,
    accentColor: Int = Color.parseColor("#FF2563EB")
) {
    val path = Path().apply {
        moveTo(x, y)
        lineTo(x, y + sizePx)
        lineTo(x + sizePx * 0.28f, y + sizePx * 0.74f)
        lineTo(x + sizePx * 0.48f, y + sizePx * 1.10f)
        lineTo(x + sizePx * 0.62f, y + sizePx * 1.03f)
        lineTo(x + sizePx * 0.42f, y + sizePx * 0.68f)
        lineTo(x + sizePx * 0.74f, y + sizePx * 0.68f)
        close()
    }
    val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DD0F172A")
        style = Paint.Style.STROKE
        strokeWidth = (sizePx * 0.14f).coerceIn(2.5f, 5f)
        strokeJoin = Paint.Join.ROUND
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.STROKE
        strokeWidth = (sizePx * 0.06f).coerceIn(1.2f, 2.5f)
        strokeJoin = Paint.Join.ROUND
    }
    canvas.drawPath(path, outlinePaint)
    canvas.drawPath(path, fillPaint)
    canvas.drawPath(path, accentPaint)
}

/** Transparent non-touch-intercepting overlay for drawing the virtual arrow cursor on a Presentation. */
class CursorOverlayView(context: Context) : View(context) {
    var cursorX: Float = 0f
        private set
    var cursorY: Float = 0f
        private set
    var cursorVisible: Boolean = false
        private set
    var accentColor: Int = Color.parseColor("#FF2563EB")

    fun updateCursor(x: Float, y: Float, visible: Boolean) {
        cursorX = x
        cursorY = y
        cursorVisible = visible
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!cursorVisible) return
        val sizePx = (resources.displayMetrics.density * 22f).coerceIn(36f, 64f)
        drawArrowCursor(canvas, cursorX, cursorY, sizePx, accentColor)
    }
}
