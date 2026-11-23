package com.yangyws.dualscreenbrowser.browser

import android.annotation.SuppressLint
import android.app.Presentation
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.os.SystemClock
import android.view.Display
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Renders a single tall WebView onto a VirtualDisplay + ImageReader and slices
 * each frame into top and bottom viewports for true cross-screen DOM/popup spanning.
 */
class SpanEngine {
    private var frameThread: HandlerThread? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var presentation: VirtualWebPresentation? = null
    private var virtualWebView: WebView? = null

    private val backBuffers = arrayOfNulls<Bitmap>(3)
    private var bufferIndex = 0
    private val bufferLock = Any()

    @Volatile
    private var stopping = false

    @Volatile
    var latestBitmap: Bitmap? = null
        private set

    var virtualWidth: Int = 0
        private set
    var topViewportHeight: Int = 0
        private set
    var hingeGapPx: Int = 0
        private set
    private var virtualScale: Float = 1f
    var bottomViewportHeight: Int = 0
        private set
    var totalHeight: Int = 0
        private set

    private val frameListeners = CopyOnWriteArrayList<(Bitmap) -> Unit>()
    private val cursorListeners = CopyOnWriteArrayList<() -> Unit>()

    @Volatile
    var cursorX: Float = 0f
        private set

    @Volatile
    var cursorY: Float = 0f
        private set

    @Volatile
    var cursorVisible: Boolean = false
        private set

    fun addCursorListener(listener: () -> Unit) {
        cursorListeners.addIfAbsent(listener)
    }

    fun removeCursorListener(listener: () -> Unit) {
        cursorListeners.remove(listener)
    }

    fun moveCursorBy(dx: Float, dy: Float) {
        if (virtualWidth <= 0 || totalHeight <= 0) return
        if (!cursorVisible) {
            cursorX = virtualWidth * 0.5f
            cursorY = topViewportHeight * 0.5f
            cursorVisible = true
        }
        cursorX = (cursorX + dx).coerceIn(0f, (virtualWidth - 1).toFloat())
        cursorY = (cursorY + dy).coerceIn(0f, (totalHeight - 1).toFloat())
        cursorListeners.forEach { it() }
    }

    fun tapAtCursor() {
        val wv = virtualWebView ?: return
        if (!cursorVisible) return
        val now = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, cursorX, cursorY, 0)
        val up = MotionEvent.obtain(now, now + 40, MotionEvent.ACTION_UP, cursorX, cursorY, 0)
        wv.dispatchTouchEvent(down)
        wv.dispatchTouchEvent(up)
        down.recycle()
        up.recycle()
    }

    fun addFrameListener(listener: (Bitmap) -> Unit) {
        frameListeners.addIfAbsent(listener)
        latestBitmap?.let { bmp ->
            if (!bmp.isRecycled) listener(bmp)
        }
    }

    fun removeFrameListener(listener: (Bitmap) -> Unit) {
        frameListeners.remove(listener)
    }

    fun isRunning(): Boolean = virtualDisplay != null && virtualWebView != null

    fun currentUrl(): String? = virtualWebView?.url

    @SuppressLint("WrongConstant")
    fun start(
        context: Context,
        widthPx: Int,
        topHeightPx: Int,
        bottomHeightPx: Int,
        hingeGapPx: Int = 0,
        dpi: Int,
        initialUrl: String,
        jsEnabled: Boolean,
        adBlockEnabledProvider: () -> Boolean,
        userScriptsProvider: () -> Triple<Boolean, Boolean, List<UserScript>>,
        onAdBlocked: () -> Unit,
        onPageInfoChanged: (url: String, title: String, canBack: Boolean, canForward: Boolean) -> Unit,
        onProgressChanged: (Int) -> Unit
    ) {
        stop()
        stopping = false

        // Scale down oversized resolutions to keep VirtualDisplay + ImageReader fast and smooth
        val rawW = widthPx.coerceAtLeast(320)
        val scale = if (rawW > 760) 760f / rawW.toFloat() else 1f

        val safeW = (rawW * scale).toInt().coerceAtLeast(320)
        val safeTopH = (topHeightPx * scale).toInt().coerceAtLeast(200)
        val safeGap = (hingeGapPx * scale).toInt().coerceAtLeast(0)
        val safeBottomH = (bottomHeightPx * scale).toInt().coerceAtLeast(200)
        val safeTotalH = safeTopH + safeGap + safeBottomH
        val scaledDpi = (dpi * scale).toInt().coerceAtLeast(120)

        virtualWidth = safeW
        topViewportHeight = safeTopH
        virtualScale = scale
        this.hingeGapPx = safeGap
        bottomViewportHeight = safeBottomH
        totalHeight = safeTotalH

        val thread = HandlerThread("SpanFrameThread", Process.THREAD_PRIORITY_DISPLAY).apply { start() }
        frameThread = thread
        val handler = Handler(thread.looper)

        val reader = ImageReader.newInstance(safeW, safeTotalH, PixelFormat.RGBA_8888, 3)
        imageReader = reader

        reader.setOnImageAvailableListener({ r ->
            if (stopping) {
                runCatching { r.acquireLatestImage()?.close() }
                return@setOnImageAvailableListener
            }
            val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                if (stopping) return@setOnImageAvailableListener
                val plane = image.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * safeW
                val bufferWidth = safeW + rowPadding / pixelStride

                val bmp = synchronized(bufferLock) {
                    if (stopping) return@setOnImageAvailableListener
                    val idx = bufferIndex
                    bufferIndex = (bufferIndex + 1) % backBuffers.size
                    backBuffers[idx]?.takeIf {
                        !it.isRecycled && it.width == bufferWidth && it.height == safeTotalH
                    } ?: Bitmap.createBitmap(bufferWidth, safeTotalH, Bitmap.Config.ARGB_8888).also {
                        backBuffers[idx] = it
                    }
                }

                bmp.copyPixelsFromBuffer(buffer)
                if (!stopping) {
                    latestBitmap = bmp
                    frameListeners.forEach { it(bmp) }
                }
            } catch (_: Throwable) {
                // Ignore transient frame errors during teardown
            } finally {
                image.close()
            }
        }, handler)

        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val vd = dm.createVirtualDisplay(
            "DualScreenSpan",
            safeW,
            safeTotalH,
            scaledDpi,
            reader.surface,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
        )
        virtualDisplay = vd

        val pres = VirtualWebPresentation(
            outerContext = context.createDisplayContext(vd.display),
            display = vd.display,
            initialUrl = initialUrl,
            jsEnabled = jsEnabled,
            adBlockEnabledProvider = adBlockEnabledProvider,
            userScriptsProvider = userScriptsProvider,
            onAdBlocked = onAdBlocked,
            onPageInfoChanged = onPageInfoChanged,
            onProgressChanged = onProgressChanged,
            onWebViewReady = { wv -> virtualWebView = wv }
        )
        pres.show()
        presentation = pres
    }

    fun updateHingeGapPx(rawGapPx: Int) {
        val scaled = (rawGapPx * virtualScale).toInt().coerceAtLeast(0)
        hingeGapPx = scaled
        latestBitmap?.let { bmp ->
            if (!bmp.isRecycled) frameListeners.forEach { it(bmp) }
        }
    }

    fun loadUrl(url: String) {
        virtualWebView?.loadUrl(url)
    }

    fun goBack() {
        virtualWebView?.let { if (it.canGoBack()) it.goBack() }
    }

    fun goForward() {
        virtualWebView?.let { if (it.canGoForward()) it.goForward() }
    }

    fun reload() {
        virtualWebView?.reload()
    }

    fun scrollByY(dy: Int) {
        virtualWebView?.scrollBy(0, dy)
    }

    fun zoomBy(factor: Float) {
        virtualWebView?.zoomBy(factor.coerceIn(0.88f, 1.12f))
    }

    /** Captures a seamless stitched Bitmap of the top and bottom viewports (excluding hinge gap). */
    fun captureStitchedBitmap(): Bitmap? {
        val src = latestBitmap ?: return null
        if (src.isRecycled || virtualWidth <= 0) return null
        val w = virtualWidth.coerceAtMost(src.width)
        val topH = topViewportHeight.coerceAtMost(src.height)
        val botStart = (topViewportHeight + hingeGapPx).coerceAtMost(src.height)
        val botH = bottomViewportHeight.coerceAtMost(src.height - botStart).coerceAtLeast(0)
        val outH = (topH + botH).coerceAtLeast(1)
        val out = Bitmap.createBitmap(w, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        if (topH > 0) {
            canvas.drawBitmap(src, Rect(0, 0, w, topH), Rect(0, 0, w, topH), null)
        }
        if (botH > 0) {
            canvas.drawBitmap(src, Rect(0, botStart, w, botStart + botH), Rect(0, topH, w, topH + botH), null)
        }
        return out
    }

    /** Captures the full scrollable document by stepping through viewport tiles without slowing normal WebView rendering. */
    fun captureFullPageBitmap(maxHeightPx: Int = 12000): Bitmap? {
        val wv = virtualWebView ?: return null
        if (wv.width <= 0 || wv.height <= 0) return null
        val density = wv.resources.displayMetrics.density.coerceAtLeast(1f)
        val fullH = (wv.contentHeight * density).toInt().coerceIn(wv.height, maxHeightPx)
        val bmp = Bitmap.createBitmap(wv.width, fullH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val prevScrollX = wv.scrollX
        val prevScrollY = wv.scrollY
        val step = wv.height
        var y = 0
        while (y < fullH) {
            val targetY = y.coerceAtMost((fullH - step).coerceAtLeast(0))
            wv.scrollTo(0, targetY)
            canvas.save()
            canvas.translate(0f, targetY.toFloat())
            wv.draw(canvas)
            canvas.restore()
            if (targetY + step >= fullH) break
            y += step
        }
        wv.scrollTo(prevScrollX, prevScrollY)
        return bmp
    }

    fun dispatchMappedTouch(
        event: MotionEvent,
        viewWidth: Int,
        viewHeight: Int,
        isBottomViewport: Boolean
    ): Boolean {
        val wv = virtualWebView ?: return false
        if (viewWidth <= 0 || viewHeight <= 0 || virtualWidth <= 0) return false

        val regionTop = if (isBottomViewport) (topViewportHeight + hingeGapPx).toFloat() else 0f
        val regionHeight = if (isBottomViewport) bottomViewportHeight.toFloat() else topViewportHeight.toFloat()

        val scaleX = virtualWidth.toFloat() / viewWidth.toFloat()
        val scaleY = regionHeight / viewHeight.toFloat()

        val pointerCount = event.pointerCount
        val props = Array(pointerCount) { i ->
            MotionEvent.PointerProperties().also { event.getPointerProperties(i, it) }
        }
        val coords = Array(pointerCount) { i ->
            MotionEvent.PointerCoords().also {
                event.getPointerCoords(i, it)
                it.x *= scaleX
                it.y = regionTop + it.y * scaleY
            }
        }

        val mapped = MotionEvent.obtain(
            event.downTime,
            event.eventTime,
            event.action,
            pointerCount,
            props,
            coords,
            event.metaState,
            event.buttonState,
            event.xPrecision,
            event.yPrecision,
            event.deviceId,
            event.edgeFlags,
            event.source,
            event.flags
        )
        val handled = wv.dispatchTouchEvent(mapped)
        mapped.recycle()
        return handled
    }

    fun stop() {
        stopping = true
        imageReader?.setOnImageAvailableListener(null, null)
        virtualWebView?.let { wv ->
            runCatching {
                wv.stopLoading()
                wv.destroy()
            }
        }
        virtualWebView = null
        runCatching { presentation?.dismiss() }
        presentation = null
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        frameThread?.quitSafely()
        frameThread = null
        runCatching { imageReader?.close() }
        imageReader = null
        synchronized(bufferLock) {
            latestBitmap = null
            for (i in backBuffers.indices) {
                backBuffers[i] = null
            }
        }
    }

    private class VirtualWebPresentation(
        outerContext: Context,
        display: Display,
        private val initialUrl: String,
        private val jsEnabled: Boolean,
        private val adBlockEnabledProvider: () -> Boolean,
        private val userScriptsProvider: () -> Triple<Boolean, Boolean, List<UserScript>>,
        private val onAdBlocked: () -> Unit,
        private val onPageInfoChanged: (String, String, Boolean, Boolean) -> Unit,
        private val onProgressChanged: (Int) -> Unit,
        private val onWebViewReady: (WebView) -> Unit
    ) : Presentation(outerContext, display) {

        private var webView: WebView? = null

        @SuppressLint("SetJavaScriptEnabled")
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val root = FrameLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            val wv = WebView(context).apply {
                settings.javaScriptEnabled = jsEnabled
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val reqUrl = request?.url?.toString()
                        if (adBlockEnabledProvider() && AdBlocker.shouldBlockUrl(reqUrl)) {
                            AdBlocker.recordBlocked()
                            onAdBlocked()
                            return AdBlocker.emptyResponse()
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val uri = request?.url ?: return false
                        return uri.scheme != "http" && uri.scheme != "https" && uri.scheme != "about"
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (!url.isNullOrBlank()) {
                            val (forceDark, enableCopy, customScripts) = userScriptsProvider()
                            UserScriptManager.injectScripts(view, url, forceDark, enableCopy, customScripts)
                            onPageInfoChanged(
                                url,
                                view?.title.orEmpty(),
                                view?.canGoBack() == true,
                                view?.canGoForward() == true
                            )
                        }
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgressChanged(newProgress)
                    }
                }
                loadUrl(initialUrl)
            }
            webView = wv
            onWebViewReady(wv)
            root.addView(
                wv,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            setContentView(root)
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            webView?.destroy()
            webView = null
        }
    }
}
