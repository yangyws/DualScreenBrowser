package com.yangyws.dualscreenbrowser.browser

import android.annotation.SuppressLint
import android.app.Presentation
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Display
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.yangyws.dualscreenbrowser.R
import com.yangyws.dualscreenbrowser.ui.SpanViewportView
import kotlin.math.abs
import kotlin.math.hypot

class SecondaryBrowserPresentation(
    outerContext: Context,
    display: Display,
    private val initialUrl: String,
    private val jsEnabled: Boolean,
    private val spanEngine: SpanEngine,
    private val searchEngineProvider: () -> SearchEngine,
    private val adBlockEnabledProvider: () -> Boolean,
    private val userScriptsProvider: () -> Triple<Boolean, Boolean, List<UserScript>>,
    private val spanModeProvider: () -> Boolean,
    private val touchpadVisibleProvider: () -> Boolean,
    private val themeProvider: () -> ThorTheme,
    private val bottomColorProvider: () -> Pair<Int, Int>,
    private val onAdBlocked: () -> Unit,
    private val onSendUrlToPrimary: (String) -> Unit,
    private val onSwapScreens: (currentSecondaryUrl: String) -> Unit,
    private val isBookmarkedProvider: (String) -> Boolean,
    private val onToggleBookmark: (url: String, title: String) -> Unit,
    private val onReopenTop: () -> Unit,
    private val onUrlChanged: (String, String) -> Unit,
    private val onRequestClose: () -> Unit
) : Presentation(outerContext, display) {

    private var webView: WebView? = null
    private var spanViewport: SpanViewportView? = null
    private var rootLayout: LinearLayout? = null
    private var touchpadContainer: LinearLayout? = null
    private var topBarView: LinearLayout? = null
    private var dividerView: View? = null
    private var urlInput: EditText? = null
    private var lockIcon: ImageView? = null
    private var clearAddressBtn: ImageView? = null
    private var addressBox: LinearLayout? = null
    private var backButton: ImageButton? = null
    private var forwardButton: ImageButton? = null
    private var progressBar: ProgressBar? = null
    private var currentUrl: String = initialUrl
    private var desktopMode: Boolean = false
    private var activeTheme: ThorTheme = themeProvider()
    private val colorPaint = Paint()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialTheme = themeProvider()
        activeTheme = initialTheme
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(initialTheme.backgroundColor)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        rootLayout = root

        val topBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(initialTheme.surfaceColor)
            setPadding(dp(8), dp(6), dp(8), dp(6))
            visibility = if (!spanModeProvider()) android.view.View.VISIBLE else android.view.View.GONE
        }
        topBarView = topBar

        val btnBack = navButton(R.drawable.ic_arrow_back, context.getString(R.string.action_back)) {
            webView?.let { if (it.canGoBack()) it.goBack() }
        }.apply {
            isEnabled = false
            alpha = 0.38f
        }
        backButton = btnBack
        val btnForward = navButton(R.drawable.ic_arrow_forward, context.getString(R.string.action_forward)) {
            webView?.let { if (it.canGoForward()) it.goForward() }
        }.apply {
            isEnabled = false
            alpha = 0.38f
        }
        forwardButton = btnForward
        val btnRefresh = navButton(R.drawable.ic_refresh, context.getString(R.string.action_refresh)) {
            webView?.reload()
        }

        val unfocusedBorderColor = Color.parseColor("#14FFFFFF")
        val focusedBorderColor = Color.argb(
            (255 * 0.65f).toInt(),
            Color.red(initialTheme.primaryColor),
            Color.green(initialTheme.primaryColor),
            Color.blue(initialTheme.primaryColor)
        )
        val addressDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(10).toFloat()
            setColor(initialTheme.surfaceVariantColor)
            setStroke(dp(1), unfocusedBorderColor)
        }

        val addrContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = addressDrawable
            setPadding(dp(10), dp(7), dp(10), dp(7))
        }
        addressBox = addrContainer

        val lockImg = ImageView(context).apply {
            setImageResource(R.drawable.ic_lock)
            setColorFilter(initialTheme.primaryColor)
            visibility = if (UrlResolver.isHttps(initialUrl)) View.VISIBLE else View.GONE
        }
        lockIcon = lockImg

        val clearImg = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            contentDescription = context.getString(R.string.action_close)
            setColorFilter(Color.parseColor("#8A92A6"))
            visibility = View.GONE
            setOnClickListener {
                urlInput?.setText("")
            }
        }
        clearAddressBtn = clearImg

        val addressBar = EditText(context).apply {
            hint = context.getString(R.string.url_placeholder)
            setText(UrlResolver.displayHost(initialUrl).ifBlank { initialUrl })
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#8A92A6"))
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(0, 0, 0, 0)
            textSize = 13.5f
            isSingleLine = true
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_GO
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) {
                    clearAddressBtn?.visibility = if (hasFocus() && !s.isNullOrEmpty()) View.VISIBLE else View.GONE
                }
            })
            setOnFocusChangeListener { v, hasFocus ->
                val et = v as EditText
                val border = if (hasFocus) {
                    Color.argb(
                        (255 * 0.65f).toInt(),
                        Color.red(activeTheme.primaryColor),
                        Color.green(activeTheme.primaryColor),
                        Color.blue(activeTheme.primaryColor)
                    )
                } else {
                    unfocusedBorderColor
                }
                (addressBox?.background as? GradientDrawable)?.setStroke(dp(1), border)
                if (hasFocus) {
                    lockIcon?.visibility = View.GONE
                    et.setText(currentUrl)
                    et.selectAll()
                    clearAddressBtn?.visibility = if (et.text.isNullOrEmpty()) View.GONE else View.VISIBLE
                } else {
                    clearAddressBtn?.visibility = View.GONE
                    lockIcon?.visibility = if (UrlResolver.isHttps(currentUrl)) View.VISIBLE else View.GONE
                    et.setText(UrlResolver.displayHost(currentUrl).ifBlank { currentUrl })
                }
            }
            setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_GO ||
                    (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    val target = UrlResolver.resolveInput(v.text.toString(), searchEngineProvider())
                    loadUrl(target)
                    v.clearFocus()
                    true
                } else {
                    false
                }
            }
        }
        urlInput = addressBar
        addrContainer.addView(lockImg, LinearLayout.LayoutParams(dp(13), dp(13)).apply {
            setMargins(0, 0, dp(5), 0)
        })
        addrContainer.addView(addressBar, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addrContainer.addView(clearImg, LinearLayout.LayoutParams(dp(16), dp(16)).apply {
            setMargins(dp(4), 0, 0, 0)
        })

        val btnSwap = navButton(R.drawable.ic_swap_vert, context.getString(R.string.action_swap_screens)) {
            onSwapScreens(currentUrl)
        }
        val btnSendUp = navButton(R.drawable.ic_upload_top, context.getString(R.string.action_send_to_primary)) {
            onSendUrlToPrimary(currentUrl)
        }
        lateinit var btnMore: ImageButton
        btnMore = navButton(R.drawable.ic_more_vert, context.getString(R.string.action_settings)) {
            val popup = PopupMenu(context, btnMore)
            val bookmarked = isBookmarkedProvider(currentUrl)
            popup.menu.add(0, 1, 0, context.getString(if (bookmarked) R.string.action_remove_bookmark else R.string.action_add_bookmark))
            val desktopLabel = if (desktopMode) {
                "✓ ${context.getString(R.string.action_desktop_mode)}"
            } else {
                context.getString(R.string.action_desktop_mode)
            }
            popup.menu.add(0, 2, 1, desktopLabel)
            popup.menu.add(0, 3, 2, context.getString(R.string.action_screenshot_bottom))
            popup.menu.add(0, 4, 3, context.getString(R.string.action_long_screenshot))
            popup.menu.add(0, 5, 4, context.getString(R.string.action_refresh))
            popup.menu.add(0, 6, 5, context.getString(R.string.action_reopen_top))
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> onToggleBookmark(currentUrl, webView?.title.orEmpty())
                    2 -> toggleDesktopMode()
                    3 -> {
                        val bmp = captureViewportBitmap()
                        val saved = bmp?.let { b ->
                            ScreenCaptureHelper.saveBitmapToGallery(context, b).also { b.recycle() }
                        } ?: false
                        val msgRes = if (saved) R.string.toast_screenshot_saved else R.string.toast_screenshot_failed
                        Toast.makeText(context, context.getString(msgRes), Toast.LENGTH_SHORT).show()
                    }
                    4 -> {
                        val bmp = captureFullPageBitmap()
                        val saved = bmp?.let { b ->
                            ScreenCaptureHelper.saveBitmapToGallery(context, b).also { b.recycle() }
                        } ?: false
                        val msgRes = if (saved) R.string.toast_screenshot_saved else R.string.toast_screenshot_failed
                        Toast.makeText(context, context.getString(msgRes), Toast.LENGTH_SHORT).show()
                    }
                    5 -> reloadPage()
                    6 -> onReopenTop()
                }
                true
            }
            popup.show()
        }

        val btnSize = dp(36)
        val btnGap = dp(2)
        topBar.addView(btnBack, LinearLayout.LayoutParams(btnSize, btnSize).apply { setMargins(0, 0, btnGap, 0) })
        topBar.addView(btnForward, LinearLayout.LayoutParams(btnSize, btnSize).apply { setMargins(0, 0, btnGap, 0) })
        topBar.addView(btnRefresh, LinearLayout.LayoutParams(btnSize, btnSize).apply { setMargins(0, 0, btnGap, 0) })
        topBar.addView(
            addrContainer,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(4), 0, dp(4), 0)
            }
        )
        topBar.addView(btnSwap, LinearLayout.LayoutParams(btnSize, btnSize).apply { setMargins(btnGap, 0, 0, 0) })
        topBar.addView(btnSendUp, LinearLayout.LayoutParams(btnSize, btnSize).apply { setMargins(btnGap, 0, 0, 0) })
        topBar.addView(btnMore, LinearLayout.LayoutParams(btnSize, btnSize).apply { setMargins(btnGap, 0, 0, 0) })

        val divider = android.view.View(context).apply {
            setBackgroundColor(Color.parseColor("#14FFFFFF"))
            visibility = if (!spanModeProvider()) android.view.View.VISIBLE else android.view.View.GONE
        }
        dividerView = divider

        val progress = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progressTintList = ColorStateList.valueOf(initialTheme.primaryColor)
            visibility = android.view.View.GONE
        }
        progressBar = progress

        val wv = WebView(context).apply {
            settings.javaScriptEnabled = jsEnabled
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
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

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url ?: return false
                    return uri.scheme != "http" && uri.scheme != "https" && uri.scheme != "about"
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    updateNavButtons(view)
                    if (!url.isNullOrBlank()) {
                        currentUrl = url
                        if (urlInput?.hasFocus() != true) {
                            lockIcon?.visibility = if (UrlResolver.isHttps(url)) View.VISIBLE else View.GONE
                            urlInput?.setText(UrlResolver.displayHost(url).ifBlank { url })
                        }
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    updateNavButtons(view)
                    if (!url.isNullOrBlank()) {
                        currentUrl = url
                        if (urlInput?.hasFocus() != true) {
                            lockIcon?.visibility = if (UrlResolver.isHttps(url)) View.VISIBLE else View.GONE
                            urlInput?.setText(UrlResolver.displayHost(url).ifBlank { url })
                        }
                        val (forceDark, enableCopy, customScripts) = userScriptsProvider()
                        UserScriptManager.injectScripts(view, url, forceDark, enableCopy, customScripts)
                        onUrlChanged(url, view?.title.orEmpty())
                    }
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    updateNavButtons(view)
                    progressBar?.let { bar ->
                        bar.progress = newProgress
                        bar.visibility = if (newProgress in 1..99) android.view.View.VISIBLE else android.view.View.GONE
                    }
                }
            }
            loadUrl(initialUrl)
        }
        webView = wv

        val contentStack = FrameLayout(context)
        val (b, w) = bottomColorProvider()
        applyColorFilterToWebView(wv, b, w)
        val spanView = SpanViewportView(context, spanEngine, isBottomViewport = true).apply {
            visibility = if (spanModeProvider()) View.VISIBLE else View.GONE
            setColorCalibration(b, w)
        }
        spanViewport = spanView
        wv.visibility = if (spanModeProvider()) View.GONE else View.VISIBLE

        contentStack.addView(wv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        contentStack.addView(spanView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        root.addView(topBar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(divider, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)))
        root.addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(2)))
        root.addView(contentStack, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(createTouchpadArea(), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(112)))
        setContentView(root)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createTouchpadArea(): LinearLayout {
        var lastAvgX = 0f
        var lastAvgY = 0f
        var lastSpanDist = 0f
        var totalMove = 0f
        var maxPointers = 0
        var downTimeMs = 0L
        val tapSlopPx = dp(12)

        fun avgCenter(event: MotionEvent, ignoreIndex: Int = -1): Pair<Float, Float> {
            var sx = 0f
            var sy = 0f
            var count = 0
            for (i in 0 until event.pointerCount) {
                if (i == ignoreIndex) continue
                sx += event.getX(i)
                sy += event.getY(i)
                count++
            }
            return if (count > 0) (sx / count) to (sy / count) else 0f to 0f
        }

        fun pairDistance(event: MotionEvent): Float {
            if (event.pointerCount < 2) return 0f
            return hypot(event.getX(1) - event.getX(0), event.getY(1) - event.getY(0))
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#14171E"))
            visibility = if (touchpadVisibleProvider()) View.VISIBLE else View.GONE
        }
        touchpadContainer = container

        val topDivider = View(context).apply {
            setBackgroundColor(Color.parseColor("#24FFFFFF"))
        }
        val padArea = FrameLayout(context)
        val hintLabel = TextView(context).apply {
            text = context.getString(R.string.touchpad_hint)
            setTextColor(Color.parseColor("#6E7687"))
            textSize = 11f
            gravity = Gravity.CENTER
            setPadding(dp(12), 0, dp(12), 0)
        }
        padArea.addView(hintLabel, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        container.addView(topDivider, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)))
        container.addView(padArea, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        padArea.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastAvgX = event.x
                    lastAvgY = event.y
                    lastSpanDist = 0f
                    totalMove = 0f
                    maxPointers = 1
                    downTimeMs = event.eventTime
                    true
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    maxPointers = maxOf(maxPointers, event.pointerCount)
                    val (cx, cy) = avgCenter(event)
                    lastAvgX = cx
                    lastAvgY = cy
                    lastSpanDist = pairDistance(event)
                    true
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    val (cx, cy) = avgCenter(event, ignoreIndex = event.actionIndex)
                    lastAvgX = cx
                    lastAvgY = cy
                    lastSpanDist = 0f
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val (cx, cy) = avgCenter(event)
                    val dx = cx - lastAvgX
                    val dy = cy - lastAvgY
                    totalMove += abs(dx) + abs(dy)

                    if (event.pointerCount == 2) {
                        val curDist = pairDistance(event)
                        val distDelta = abs(curDist - lastSpanDist)
                        if (lastSpanDist > dp(24) && distDelta > dp(4) && distDelta > abs(dy) * 1.15f) {
                            val factor = curDist / lastSpanDist
                            if (spanModeProvider()) spanEngine.zoomBy(factor) else webView?.zoomBy(factor)
                            lastSpanDist = curDist
                        } else {
                            val scrollDy = (-dy * 2.4f).toInt()
                            if (spanModeProvider()) spanEngine.scrollByY(scrollDy) else webView?.scrollBy(0, scrollDy)
                        }
                    } else if (event.pointerCount >= 3) {
                        val scrollDy = (-dy * 2.4f).toInt()
                        if (spanModeProvider()) spanEngine.scrollByY(scrollDy) else webView?.scrollBy(0, scrollDy)
                    } else if (spanModeProvider()) {
                        spanEngine.moveCursorBy(dx * 1.4f, dy * 1.4f)
                    }

                    lastAvgX = cx
                    lastAvgY = cy
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val durationMs = event.eventTime - downTimeMs
                    if (totalMove < tapSlopPx && durationMs < 350L) {
                        when {
                            maxPointers >= 3 -> {
                                if (spanModeProvider()) spanEngine.reload() else webView?.reload()
                            }
                            maxPointers == 2 -> {
                                if (spanModeProvider()) spanEngine.goBack() else goBackIfPossible()
                            }
                            maxPointers == 1 && spanModeProvider() -> {
                                spanEngine.tapAtCursor()
                            }
                        }
                    }
                    true
                }
                else -> false
            }
        }
        return container
    }

    private fun captureCurrentView() {
        val bmp: Bitmap? = if (spanModeProvider()) {
            spanEngine.captureStitchedBitmap()
        } else {
            val wv = webView
            if (wv != null && wv.width > 0 && wv.height > 0) {
                Bitmap.createBitmap(wv.width, wv.height, Bitmap.Config.ARGB_8888).also { b ->
                    wv.draw(Canvas(b))
                }
            } else null
        }
        if (bmp == null) {
            Toast.makeText(context, context.getString(R.string.toast_screenshot_failed), Toast.LENGTH_SHORT).show()
            return
        }
        val saved = ScreenCaptureHelper.saveBitmapToGallery(context, bmp)
        bmp.recycle()
        val msgRes = if (saved) R.string.toast_screenshot_saved else R.string.toast_screenshot_failed
        Toast.makeText(context, context.getString(msgRes), Toast.LENGTH_SHORT).show()
    }

    fun updateTouchpadVisibility(visible: Boolean) {
        touchpadContainer?.post {
            touchpadContainer?.visibility = if (visible) View.VISIBLE else View.GONE
            touchpadContainer?.parent?.requestLayout()
        }
    }

    fun updateTheme(theme: ThorTheme) {
        activeTheme = theme
        rootLayout?.post {
            rootLayout?.setBackgroundColor(theme.backgroundColor)
            topBarView?.setBackgroundColor(theme.surfaceColor)
            touchpadContainer?.setBackgroundColor(theme.surfaceColor)
            (addressBox?.background as? GradientDrawable)?.setColor(theme.surfaceVariantColor)
            lockIcon?.setColorFilter(theme.primaryColor)
            progressBar?.progressTintList = ColorStateList.valueOf(theme.primaryColor)
        }
    }

    private fun updateNavButtons(wv: WebView?) {
        val backOk = wv?.canGoBack() == true
        val fwdOk = wv?.canGoForward() == true
        backButton?.let {
            it.isEnabled = backOk
            it.alpha = if (backOk) 1f else 0.38f
        }
        forwardButton?.let {
            it.isEnabled = fwdOk
            it.alpha = if (fwdOk) 1f else 0.38f
        }
    }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    private fun navButton(iconRes: Int, desc: String, onClick: () -> Unit): ImageButton =
        ImageButton(context).apply {
            setImageResource(iconRes)
            contentDescription = desc
            val outValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
            setBackgroundResource(outValue.resourceId)
            setColorFilter(Color.WHITE)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setOnClickListener { onClick() }
        }

    fun loadUrl(url: String) {
        currentUrl = url
        if (urlInput?.hasFocus() != true) {
            lockIcon?.visibility = if (UrlResolver.isHttps(url)) View.VISIBLE else View.GONE
            urlInput?.setText(UrlResolver.displayHost(url).ifBlank { url })
        }
        webView?.loadUrl(url)
    }

    fun currentUrl(): String = webView?.url?.takeIf { it.isNotBlank() } ?: currentUrl

    fun updateJsEnabled(enabled: Boolean) {
        webView?.settings?.javaScriptEnabled = enabled
    }

    fun scrollByY(dy: Int) {
        webView?.scrollBy(0, dy)
    }

    fun updatePresentationMode(spanActive: Boolean) {
        val barVis = if (!spanActive) View.VISIBLE else View.GONE
        val spanVis = if (spanActive) View.VISIBLE else View.GONE
        val webVis = if (spanActive) View.GONE else View.VISIBLE
        topBarView?.post {
            topBarView?.visibility = barVis
            dividerView?.visibility = barVis
            spanViewport?.visibility = spanVis
            webView?.visibility = webVis
            if (spanActive) {
                progressBar?.visibility = View.GONE
            }
            topBarView?.parent?.requestLayout()
        }
    }

    fun updateBottomColor(brightness: Int, warmth: Int) {
        spanViewport?.post {
            spanViewport?.setColorCalibration(brightness, warmth)
        }
        webView?.post {
            webView?.let { wv -> applyColorFilterToWebView(wv, brightness, warmth) }
        }
    }

    private fun applyColorFilterToWebView(wv: WebView, brightnessOffset: Int, warmthOffset: Int) {
        if (brightnessOffset == 0 && warmthOffset == 0) {
            colorPaint.colorFilter = null
            wv.setLayerType(View.LAYER_TYPE_NONE, null)
        } else {
            val base = 1f + brightnessOffset * 0.01f
            val r = (base + warmthOffset * 0.004f).coerceIn(0.5f, 1.5f)
            val g = base.coerceIn(0.5f, 1.5f)
            val b = (base - warmthOffset * 0.005f).coerceIn(0.5f, 1.5f)
            colorPaint.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setScale(r, g, b, 1f) })
            wv.setLayerType(View.LAYER_TYPE_HARDWARE, colorPaint)
        }
        wv.invalidate()
    }

    fun goBackIfPossible() {
        webView?.let { if (it.canGoBack()) it.goBack() }
    }

    fun goForwardIfPossible() {
        webView?.let { if (it.canGoForward()) it.goForward() }
    }

    fun reloadPage() {
        webView?.reload()
    }

    fun captureViewportBitmap(): Bitmap? {
        val wv = webView ?: return null
        if (wv.width <= 0 || wv.height <= 0) return null
        return Bitmap.createBitmap(wv.width, wv.height, Bitmap.Config.ARGB_8888).also { bmp ->
            wv.draw(Canvas(bmp))
        }
    }

    fun captureFullPageBitmap(maxHeightPx: Int = 12000): Bitmap? {
        val wv = webView ?: return null
        val w = wv.width
        val vh = wv.height
        if (w <= 0 || vh <= 0) return null
        val density = context.resources.displayMetrics.density.coerceAtLeast(1f)
        val totalHeight = (wv.contentHeight * density).toInt().coerceIn(vh, maxHeightPx)
        val result = Bitmap.createBitmap(w, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val prevX = wv.scrollX
        val prevY = wv.scrollY
        var y = 0
        while (y < totalHeight) {
            val stepH = minOf(vh, totalHeight - y)
            wv.scrollTo(0, y)
            canvas.save()
            canvas.translate(0f, y.toFloat())
            canvas.clipRect(0, 0, w, stepH)
            wv.draw(canvas)
            canvas.restore()
            y += vh
        }
        wv.scrollTo(prevX, prevY)
        return result
    }

    fun toggleDesktopMode(): Boolean {
        desktopMode = !desktopMode
        webView?.let { wv ->
            val defaultUa = WebSettings.getDefaultUserAgent(context)
            wv.settings.userAgentString = if (desktopMode) {
                defaultUa.replace("Mobile", "").replace("Android", "X11; Linux x86_64")
            } else {
                defaultUa
            }
            wv.reload()
        }
        return desktopMode
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        webView?.destroy()
        webView = null
    }
}
