package com.yangyws.dualscreenbrowser.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import com.yangyws.dualscreenbrowser.browser.SpanEngine

/**
 * Displays either the top or bottom slice of SpanEngine's virtual WebView frame
 * and forwards touch events back into the virtual WebView's coordinate space.
 */
@SuppressLint("ViewConstructor")
class SpanViewportView(
    context: Context,
    private val engine: SpanEngine,
    private val isBottomViewport: Boolean
) : View(context) {

    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val srcRect = Rect()
    private val dstRect = RectF()
    private var currentBitmap: Bitmap? = null

    private val frameListener: (Bitmap) -> Unit = { bmp ->
        currentBitmap = bmp
        postInvalidateOnAnimation()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        engine.addFrameListener(frameListener)
    }

    override fun onDetachedFromWindow() {
        engine.removeFrameListener(frameListener)
        currentBitmap = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = currentBitmap ?: return
        if (bmp.isRecycled || engine.virtualWidth <= 0) return

        val top = if (isBottomViewport) engine.topViewportHeight else 0
        val bottom = if (isBottomViewport) {
            (engine.topViewportHeight + engine.bottomViewportHeight).coerceAtMost(bmp.height)
        } else {
            engine.topViewportHeight.coerceAtMost(bmp.height)
        }
        val right = engine.virtualWidth.coerceAtMost(bmp.width)
        if (bottom <= top || right <= 0) return

        srcRect.set(0, top, right, bottom)
        dstRect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawBitmap(bmp, srcRect, dstRect, paint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return engine.dispatchMappedTouch(event, width, height, isBottomViewport)
    }
}
