package com.yangyws.dualscreenbrowser.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
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
    private val cursorFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val cursorStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF4CC38A")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val cursorOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AA000000")
        style = Paint.Style.STROKE
        strokeWidth = 7f
    }
    private val srcRect = Rect()
    private val dstRect = RectF()

    @Volatile
    private var currentBitmap: Bitmap? = null

    init {
        setBackgroundColor(Color.parseColor("#101216"))
    }

    fun setColorCalibration(brightnessOffset: Int, warmthOffset: Int) {
        if (brightnessOffset == 0 && warmthOffset == 0) {
            paint.colorFilter = null
        } else {
            val base = 1f + brightnessOffset * 0.01f
            val r = (base + warmthOffset * 0.004f).coerceIn(0.5f, 1.5f)
            val g = base.coerceIn(0.5f, 1.5f)
            val b = (base - warmthOffset * 0.005f).coerceIn(0.5f, 1.5f)
            paint.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setScale(r, g, b, 1f) })
        }
        invalidate()
    }

    private val frameListener: (Bitmap) -> Unit = { bmp ->
        currentBitmap = bmp
        postInvalidateOnAnimation()
    }

    private val cursorListener: () -> Unit = {
        postInvalidateOnAnimation()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        engine.addFrameListener(frameListener)
        engine.addCursorListener(cursorListener)
    }

    override fun onDetachedFromWindow() {
        engine.removeFrameListener(frameListener)
        engine.removeCursorListener(cursorListener)
        currentBitmap = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = currentBitmap ?: return
        if (bmp.isRecycled || engine.virtualWidth <= 0) return

        val top = if (isBottomViewport) engine.topViewportHeight + engine.hingeGapPx else 0
        val bottom = if (isBottomViewport) {
            (engine.topViewportHeight + engine.hingeGapPx + engine.bottomViewportHeight).coerceAtMost(bmp.height)
        } else {
            engine.topViewportHeight.coerceAtMost(bmp.height)
        }
        val right = engine.virtualWidth.coerceAtMost(bmp.width)
        if (bottom <= top || right <= 0) return

        canvas.drawColor(android.graphics.Color.BLACK)
        val srcLeft: Int
        val srcRight: Int
        var topViewShiftX = 0f
        if (isBottomViewport && engine.bottomWidthRatio < 0.99f) {
            val sliceW = (right * engine.bottomWidthRatio).toInt().coerceIn(1, right)
            srcLeft = engine.bottomRegionLeft(right, sliceW)
            srcRight = (srcLeft + sliceW).coerceAtMost(right)
        } else {
            srcLeft = 0
            srcRight = right
            if (engine.isBottomPanLocked && engine.bottomWidthRatio < 0.99f) {
                val sliceW = (right * engine.bottomWidthRatio).toInt().coerceIn(1, right)
                val maxShift = ((right - sliceW) / 2f).coerceAtLeast(0f)
                val shiftPx = engine.bottomPanOffsetRatio * maxShift
                topViewShiftX = -shiftPx * (width.toFloat() / right.toFloat())
            }
        }

        srcRect.set(srcLeft, top, srcRight, bottom)
        dstRect.set(topViewShiftX, 0f, width.toFloat() + topViewShiftX, height.toFloat())
        canvas.drawBitmap(bmp, srcRect, dstRect, paint)

        if (engine.cursorVisible && srcRight > srcLeft && bottom > top) {
            val cx = engine.cursorX
            val cy = engine.cursorY
            if (cx >= srcLeft && cx <= srcRight && cy >= top && cy <= bottom) {
                val drawX = ((cx - srcLeft) / (srcRight - srcLeft).toFloat()) * width.toFloat() + topViewShiftX
                val drawY = ((cy - top) / (bottom - top).toFloat()) * height.toFloat()
                val pointerSize = (resources.displayMetrics.density * 22f).coerceIn(36f, 64f)
                drawArrowCursor(canvas, drawX, drawY, pointerSize)
            }
        }
    }

    private enum class BottomGestureMode { NONE, PAN_HORIZONTAL, VERTICAL_SCROLL }
    private var gestureMode = BottomGestureMode.NONE
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var downTimeMs = 0L

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val allowHorizontalPan = engine.bottomWidthRatio < 0.99f && (isBottomViewport || engine.isBottomPanLocked)
        if (!allowHorizontalPan || event.pointerCount > 1) {
            gestureMode = BottomGestureMode.NONE
            return engine.dispatchMappedTouch(event, width, height, isBottomViewport)
        }

        val touchSlop = 16f * resources.displayMetrics.density.coerceAtLeast(1f) / 2f
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                gestureMode = BottomGestureMode.NONE
                downX = event.x
                downY = event.y
                lastX = event.x
                downTimeMs = event.eventTime
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dxTotal = event.x - downX
                val dyTotal = event.y - downY
                if (gestureMode == BottomGestureMode.NONE) {
                    if (kotlin.math.abs(dxTotal) > touchSlop && kotlin.math.abs(dxTotal) > kotlin.math.abs(dyTotal)) {
                        gestureMode = BottomGestureMode.PAN_HORIZONTAL
                    } else if (kotlin.math.abs(dyTotal) > touchSlop) {
                        gestureMode = BottomGestureMode.VERTICAL_SCROLL
                        val synDown = MotionEvent.obtain(downTimeMs, event.eventTime, MotionEvent.ACTION_DOWN, downX, downY, 0)
                        engine.dispatchMappedTouch(synDown, width, height, isBottomViewport)
                        synDown.recycle()
                    }
                }
                when (gestureMode) {
                    BottomGestureMode.PAN_HORIZONTAL -> {
                        val dx = event.x - lastX
                        lastX = event.x
                        engine.panBottomByViewDelta(dx, width, isBottomViewport)
                        invalidate()
                        return true
                    }
                    BottomGestureMode.VERTICAL_SCROLL -> {
                        // Lock horizontal coordinate to downX so screen only moves vertically at the same longitudinal position
                        val lockedMove = MotionEvent.obtain(downTimeMs, event.eventTime, MotionEvent.ACTION_MOVE, downX, event.y, 0)
                        engine.dispatchMappedTouch(lockedMove, width, height, isBottomViewport)
                        lockedMove.recycle()
                        return true
                    }
                    BottomGestureMode.NONE -> return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val prevMode = gestureMode
                gestureMode = BottomGestureMode.NONE
                return when (prevMode) {
                    BottomGestureMode.PAN_HORIZONTAL -> true
                    BottomGestureMode.VERTICAL_SCROLL -> {
                        val lockedUp = MotionEvent.obtain(downTimeMs, event.eventTime, event.actionMasked, downX, event.y, 0)
                        val res = engine.dispatchMappedTouch(lockedUp, width, height, isBottomViewport)
                        lockedUp.recycle()
                        res
                    }
                    BottomGestureMode.NONE -> {
                        if (event.actionMasked == MotionEvent.ACTION_UP) {
                            val tapDown = MotionEvent.obtain(downTimeMs, event.eventTime, MotionEvent.ACTION_DOWN, downX, downY, 0)
                            engine.dispatchMappedTouch(tapDown, width, height, isBottomViewport)
                            tapDown.recycle()
                            val tapUp = MotionEvent.obtain(downTimeMs, event.eventTime, MotionEvent.ACTION_UP, event.x, event.y, 0)
                            val res = engine.dispatchMappedTouch(tapUp, width, height, isBottomViewport)
                            tapUp.recycle()
                            res
                        } else {
                            true
                        }
                    }
                }
            }
        }
        return engine.dispatchMappedTouch(event, width, height, isBottomViewport)
    }
}
