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
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.SweepGradient
import android.graphics.drawable.Drawable
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
import android.widget.PopupWindow
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
    private val customPrimaryProvider: () -> Int,
    private val bottomColorProvider: () -> Pair<Int, Int>,
    private val onAdBlocked: () -> Unit,
    private val onSendUrlToPrimary: (String) -> Unit,
    private val onSwapScreens: (currentSecondaryUrl: String) -> Unit,
    private val isBookmarkedProvider: (String) -> Boolean,
    private val onToggleBookmark: (url: String, title: String) -> Unit,
    private val onReopenTop: () -> Unit,
    private val onToggleSpanMode: () -> Unit,
    private val onOpenThemePicker: () -> Unit,
    private val onForwardKeyEvent: (KeyEvent) -> Boolean,
    private val onForwardMotionEvent: (MotionEvent) -> Boolean,
    private val onUrlChanged: (String, String) -> Unit,
    private val onRequestClose: () -> Unit
) : Presentation(outerContext, display) {

    private var webView: WebView? = null
    private var spanViewport: SpanViewportView? = null
    private var rootLayout: LinearLayout? = null
    private var touchpadContainer: LinearLayout? = null
    private var touchpadMoveHandle: ImageView? = null
    private var touchpadResizeHandle: ImageView? = null
    private var floatingMenuBtn: ImageButton? = null
    private var bottomMenuPopup: PopupWindow? = null
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
    private var hasLoadedInitialUrl: Boolean = false
    private var desktopMode: Boolean = false
    private var activeTheme: ThorTheme = themeProvider()
    private var activeCustomPrimary: Int = customPrimaryProvider()
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
            visibility = View.GONE
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
            visibility = View.GONE
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
            if (!spanModeProvider()) {
                hasLoadedInitialUrl = true
                loadUrl(initialUrl)
            } else {
                onPause()
            }
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

        val floatSize = dp(46)
        val floatMargin = dp(14)
        val initialAccent = activeTheme.effectivePrimary(activeCustomPrimary)
        val floatBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.argb(224, Color.red(activeTheme.surfaceColor), Color.green(activeTheme.surfaceColor), Color.blue(activeTheme.surfaceColor)))
            setStroke(dp(1), Color.argb(160, Color.red(initialAccent), Color.green(initialAccent), Color.blue(initialAccent)))
        }
        lateinit var btnFloatMenu: ImageButton
        btnFloatMenu = ImageButton(context).apply {
            setImageResource(R.drawable.ic_menu)
            contentDescription = context.getString(R.string.action_settings)
            background = floatBg
            setColorFilter(if (activeTheme.isLight) Color.parseColor("#111827") else Color.WHITE)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            elevation = dp(6).toFloat()
            visibility = if (!spanModeProvider()) View.VISIBLE else View.GONE
            var downRawX = 0f
            var downRawY = 0f
            var startX = 0f
            var startY = 0f
            var dragging = false
            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downRawX = event.rawX
                        downRawY = event.rawY
                        startX = v.x
                        startY = v.y
                        dragging = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - downRawX
                        val dy = event.rawY - downRawY
                        if (abs(dx) > dp(6) || abs(dy) > dp(6)) dragging = true
                        if (dragging) {
                            val maxX = (contentStack.width - v.width - floatMargin).toFloat().coerceAtLeast(floatMargin.toFloat())
                            val maxY = (contentStack.height - v.height - floatMargin).toFloat().coerceAtLeast(floatMargin.toFloat())
                            v.x = (startX + dx).coerceIn(floatMargin.toFloat(), maxX)
                            v.y = (startY + dy).coerceIn(floatMargin.toFloat(), maxY)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!dragging) {
                            showBottomFloatingMenu(v)
                        }
                        true
                    }
                    else -> false
                }
            }
        }
        floatingMenuBtn = btnFloatMenu
        contentStack.addView(
            btnFloatMenu,
            FrameLayout.LayoutParams(floatSize, floatSize).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                setMargins(0, 0, floatMargin, floatMargin)
            }
        )

        contentStack.addView(
            createTouchpadArea(contentStack),
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(116)).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                setMargins(dp(14), dp(8), dp(14), dp(10))
            }
        )

        root.addView(topBar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(divider, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)))
        root.addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(2)))
        root.addView(contentStack, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        window?.let { win ->
            win.setDecorFitsSystemWindows(false)
            win.decorView.post {
                win.insetsController?.let { controller ->
                    controller.hide(android.view.WindowInsets.Type.systemBars())
                    controller.systemBarsBehavior =
                        android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createTouchpadArea(parentStack: FrameLayout): LinearLayout {
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

        val padAccent = activeTheme.effectivePrimary(activeCustomPrimary)
        val padBg = createThemedCardDrawable(
            fillColor = Color.argb(228, Color.red(activeTheme.surfaceColor), Color.green(activeTheme.surfaceColor), Color.blue(activeTheme.surfaceColor)),
            cornerRadiusPx = dp(14).toFloat(),
            strokeWidthPx = dp(2).toFloat(),
            theme = activeTheme,
            accentColor = Color.argb(195, Color.red(padAccent), Color.green(padAccent), Color.blue(padAccent))
        )
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = padBg
            elevation = dp(8).toFloat()
            clipToOutline = true
            visibility = if (touchpadVisibleProvider()) View.VISIBLE else View.GONE
        }
        touchpadContainer = container

        val padArea = FrameLayout(context)
        val hintLabel = TextView(context).apply {
            text = context.getString(R.string.touchpad_hint)
            setTextColor(Color.parseColor("#8A92A6"))
            textSize = 11f
            gravity = Gravity.CENTER
            setPadding(dp(36), 0, dp(36), 0)
        }
        padArea.addView(hintLabel, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        fun makeMoveHandle(): ImageView {
            var downRawX = 0f
            var downRawY = 0f
            var startX = 0f
            var startY = 0f
            return ImageView(context).apply {
                setImageResource(R.drawable.ic_move_handle)
                setPadding(dp(6), dp(6), dp(6), dp(6))
                alpha = 0.88f
                setOnTouchListener { _, ev ->
                    when (ev.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            downRawX = ev.rawX
                            downRawY = ev.rawY
                            startX = container.x
                            startY = container.y
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val margin = dp(6).toFloat()
                            val maxX = (parentStack.width - container.width - margin).coerceAtLeast(margin)
                            val maxY = (parentStack.height - container.height - margin).coerceAtLeast(margin)
                            container.x = (startX + (ev.rawX - downRawX)).coerceIn(margin, maxX)
                            container.y = (startY + (ev.rawY - downRawY)).coerceIn(margin, maxY)
                            true
                        }
                        else -> true
                    }
                }
            }
        }

        fun makeResizeHandle(): ImageView {
            var downRawX = 0f
            var downRawY = 0f
            var startW = 0
            var startH = 0
            var startX = 0f
            var startY = 0f
            return ImageView(context).apply {
                setImageResource(R.drawable.ic_drag_corner)
                scaleX = -1f
                setPadding(dp(6), dp(6), dp(6), dp(6))
                alpha = 0.88f
                setOnTouchListener { _, ev ->
                    when (ev.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            downRawX = ev.rawX
                            downRawY = ev.rawY
                            startW = container.width.coerceAtLeast(dp(180))
                            startH = container.height.coerceAtLeast(dp(72))
                            startX = container.x
                            startY = container.y
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = ev.rawX - downRawX
                            val dy = ev.rawY - downRawY
                            val maxH = (parentStack.height - dp(16)).coerceAtLeast(dp(160))
                            val maxW = (parentStack.width - dp(16)).coerceAtLeast(dp(240))
                            val newH = (startH + dy.toInt()).coerceIn(dp(72), maxH)
                            val newW = (startW - dx.toInt()).coerceIn(dp(180), maxW)
                            val lp = container.layoutParams as FrameLayout.LayoutParams
                            lp.height = newH
                            lp.width = newW
                            container.layoutParams = lp
                            val margin = dp(6).toFloat()
                            val maxX = (parentStack.width - newW - margin).coerceAtLeast(margin)
                            val maxY = (parentStack.height - newH - margin).coerceAtLeast(margin)
                            container.x = (startX + (startW - newW)).coerceIn(margin, maxX)
                            container.y = startY.coerceIn(margin, maxY)
                            container.requestLayout()
                            true
                        }
                        else -> true
                    }
                }
            }
        }

        val handleSize = dp(30)
        val resizeHandle = makeResizeHandle().apply { setColorFilter(padAccent) }
        val moveHandle = makeMoveHandle().apply { setColorFilter(padAccent) }
        touchpadResizeHandle = resizeHandle
        touchpadMoveHandle = moveHandle
        padArea.addView(
            resizeHandle,
            FrameLayout.LayoutParams(handleSize, handleSize).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                setMargins(dp(4), 0, 0, dp(4))
            }
        )
        padArea.addView(
            moveHandle,
            FrameLayout.LayoutParams(handleSize, handleSize).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                setMargins(0, 0, dp(4), dp(4))
            }
        )

        container.addView(padArea, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

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

    fun updateTheme(theme: ThorTheme, customPrimaryColor: Int = activeCustomPrimary) {
        activeTheme = theme
        activeCustomPrimary = customPrimaryColor
        val accent = theme.effectivePrimary(customPrimaryColor)
        val fgColor = if (theme.isLight) Color.parseColor("#111827") else Color.WHITE
        val padBorderColor = Color.argb(195, Color.red(accent), Color.green(accent), Color.blue(accent))
        val padBg = createThemedCardDrawable(
            fillColor = Color.argb(228, Color.red(theme.surfaceColor), Color.green(theme.surfaceColor), Color.blue(theme.surfaceColor)),
            cornerRadiusPx = dp(14).toFloat(),
            strokeWidthPx = dp(2).toFloat(),
            theme = theme,
            accentColor = padBorderColor
        )
        rootLayout?.post {
            rootLayout?.setBackgroundColor(theme.backgroundColor)
            topBarView?.setBackgroundColor(theme.surfaceColor)
            applyFloatingButtonStyle(expanded = bottomMenuPopup?.isShowing == true)
            floatingMenuBtn?.setColorFilter(fgColor)
            touchpadContainer?.background = padBg
            touchpadMoveHandle?.setColorFilter(accent)
            touchpadResizeHandle?.setColorFilter(accent)
            (addressBox?.background as? GradientDrawable)?.setColor(theme.surfaceVariantColor)
            lockIcon?.setColorFilter(accent)
            progressBar?.progressTintList = ColorStateList.valueOf(accent)
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
            setColorFilter(if (activeTheme.isLight) Color.parseColor("#111827") else Color.WHITE)
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

    private fun createThemedCardDrawable(
        fillColor: Int,
        cornerRadiusPx: Float,
        strokeWidthPx: Float,
        theme: ThorTheme,
        accentColor: Int
    ): Drawable {
        if (theme != ThorTheme.RAINBOW) {
            return GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = cornerRadiusPx
                setColor(fillColor)
                setStroke(strokeWidthPx.toInt().coerceAtLeast(1), accentColor)
            }
        }
        return object : Drawable() {
            private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = fillColor
            }
            private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokeWidthPx
            }
            private val rect = RectF()
            private var lastW = -1
            private var lastH = -1

            override fun draw(canvas: Canvas) {
                val b = bounds
                if (b.width() <= 0 || b.height() <= 0) return
                val halfStroke = strokeWidthPx / 2f
                rect.set(b.left + halfStroke, b.top + halfStroke, b.right - halfStroke, b.bottom - halfStroke)
                canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, fillPaint)
                if (lastW != b.width() || lastH != b.height()) {
                    lastW = b.width()
                    lastH = b.height()
                    strokePaint.shader = SweepGradient(
                        b.exactCenterX(),
                        b.exactCenterY(),
                        intArrayOf(
                            Color.parseColor("#FFEAB308"), // Right: Yellow
                            Color.parseColor("#FF22C55E"), // Bottom: Green
                            Color.parseColor("#FF3B82F6"), // Left: Blue
                            Color.parseColor("#FFEF4444"), // Top: Red
                            Color.parseColor("#FFEAB308")
                        ),
                        floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)
                    )
                }
                canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, strokePaint)
            }

            override fun setAlpha(alpha: Int) {
                fillPaint.alpha = alpha
                strokePaint.alpha = alpha
            }

            override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
                fillPaint.colorFilter = colorFilter
                strokePaint.colorFilter = colorFilter
            }

            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }
    }

    private fun applyFloatingButtonStyle(expanded: Boolean) {
        val accent = activeTheme.effectivePrimary(activeCustomPrimary)
        val fillColor = if (expanded) {
            Color.argb(72, Color.red(accent), Color.green(accent), Color.blue(accent))
        } else {
            Color.argb(224, Color.red(activeTheme.surfaceColor), Color.green(activeTheme.surfaceColor), Color.blue(activeTheme.surfaceColor))
        }
        val strokeColor = if (expanded) {
            accent
        } else {
            Color.argb(165, Color.red(accent), Color.green(accent), Color.blue(accent))
        }
        floatingMenuBtn?.background = createThemedCardDrawable(
            fillColor = fillColor,
            cornerRadiusPx = dp(23).toFloat(),
            strokeWidthPx = if (expanded || activeTheme == ThorTheme.RAINBOW) dp(2).toFloat() else dp(1).toFloat(),
            theme = activeTheme,
            accentColor = strokeColor
        )
    }

    private fun showBottomFloatingMenu(anchor: View) {
        val accent = activeTheme.effectivePrimary(activeCustomPrimary)
        val fgColor = if (activeTheme.isLight) Color.parseColor("#111827") else Color.WHITE
        val mutedFg = if (activeTheme.isLight) Color.parseColor("#64748B") else Color.parseColor("#8A92A6")
        val borderColor = if (activeTheme.isLight) Color.parseColor("#24000000") else Color.parseColor("#24FFFFFF")
        val panelBg = createThemedCardDrawable(
            fillColor = activeTheme.surfaceColor,
            cornerRadiusPx = dp(12).toFloat(),
            strokeWidthPx = dp(2).toFloat(),
            theme = activeTheme,
            accentColor = Color.argb(190, Color.red(accent), Color.green(accent), Color.blue(accent))
        )
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = panelBg
            setPadding(dp(10), dp(10), dp(10), dp(8))
            elevation = dp(8).toFloat()
        }

        lateinit var popup: PopupWindow

        val navRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val btnSize = dp(34)
        navRow.addView(navButton(R.drawable.ic_arrow_back, context.getString(R.string.action_back)) {
            popup.dismiss()
            goBackIfPossible()
        }, LinearLayout.LayoutParams(btnSize, btnSize))
        navRow.addView(navButton(R.drawable.ic_arrow_forward, context.getString(R.string.action_forward)) {
            popup.dismiss()
            goForwardIfPossible()
        }, LinearLayout.LayoutParams(btnSize, btnSize))
        navRow.addView(navButton(R.drawable.ic_refresh, context.getString(R.string.action_refresh)) {
            popup.dismiss()
            reloadPage()
        }, LinearLayout.LayoutParams(btnSize, btnSize))
        navRow.addView(navButton(R.drawable.ic_swap_vert, context.getString(R.string.action_swap_screens)) {
            popup.dismiss()
            onSwapScreens(currentUrl())
        }, LinearLayout.LayoutParams(btnSize, btnSize))
        navRow.addView(navButton(R.drawable.ic_upload_top, context.getString(R.string.action_send_to_primary)) {
            popup.dismiss()
            onSendUrlToPrimary(currentUrl())
        }, LinearLayout.LayoutParams(btnSize, btnSize))
        val btnPalette = navButton(R.drawable.ic_palette, context.getString(R.string.settings_theme)) {
            popup.dismiss()
            onOpenThemePicker()
        }.apply {
            setColorFilter(accent)
        }
        navRow.addView(btnPalette, LinearLayout.LayoutParams(btnSize, btnSize))
        panel.addView(navRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val urlBoxBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(8).toFloat()
            setColor(activeTheme.surfaceVariantColor)
            setStroke(dp(1), borderColor)
        }
        val urlRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = urlBoxBg
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }
        val urlEdit = EditText(context).apply {
            hint = context.getString(R.string.url_placeholder)
            setText(currentUrl())
            setTextColor(fgColor)
            setHintTextColor(mutedFg)
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(0, 0, 0, 0)
            textSize = 13f
            isSingleLine = true
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_GO ||
                    (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    val target = UrlResolver.resolveInput(v.text.toString(), searchEngineProvider())
                    loadUrl(target)
                    popup.dismiss()
                    true
                } else {
                    false
                }
            }
        }
        val goBtn = TextView(context).apply {
            text = context.getString(R.string.action_go)
            setTextColor(accent)
            textSize = 12.5f
            setPadding(dp(8), dp(2), dp(4), dp(2))
            setOnClickListener {
                val target = UrlResolver.resolveInput(urlEdit.text.toString(), searchEngineProvider())
                loadUrl(target)
                popup.dismiss()
            }
        }
        urlRow.addView(urlEdit, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        urlRow.addView(goBtn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        panel.addView(urlRow, LinearLayout.LayoutParams(dp(248), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, dp(6), 0, dp(6))
        })

        fun addMenuItem(label: String, onClick: () -> Unit) {
            val tv = TextView(context).apply {
                text = label
                setTextColor(fgColor)
                textSize = 13.5f
                setPadding(dp(8), dp(9), dp(8), dp(9))
                val outValue = TypedValue()
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                setBackgroundResource(outValue.resourceId)
                setOnClickListener {
                    popup.dismiss()
                    onClick()
                }
            }
            panel.addView(tv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        val bookmarked = isBookmarkedProvider(currentUrl())
        addMenuItem(context.getString(if (bookmarked) R.string.action_remove_bookmark else R.string.action_add_bookmark)) {
            onToggleBookmark(currentUrl(), webView?.title.orEmpty())
        }
        val desktopLabel = if (desktopMode) "✓ ${context.getString(R.string.action_desktop_mode)}" else context.getString(R.string.action_desktop_mode)
        addMenuItem(desktopLabel) { toggleDesktopMode() }
        addMenuItem(context.getString(R.string.action_screenshot_bottom)) {
            val bmp = captureViewportBitmap()
            val saved = bmp?.let { b ->
                ScreenCaptureHelper.saveBitmapToGallery(context, b).also { b.recycle() }
            } ?: false
            val msgRes = if (saved) R.string.toast_screenshot_saved else R.string.toast_screenshot_failed
            Toast.makeText(context, context.getString(msgRes), Toast.LENGTH_SHORT).show()
        }
        addMenuItem(context.getString(R.string.action_long_screenshot)) {
            val bmp = captureFullPageBitmap()
            val saved = bmp?.let { b ->
                ScreenCaptureHelper.saveBitmapToGallery(context, b).also { b.recycle() }
            } ?: false
            val msgRes = if (saved) R.string.toast_screenshot_saved else R.string.toast_screenshot_failed
            Toast.makeText(context, context.getString(msgRes), Toast.LENGTH_SHORT).show()
        }
        addMenuItem(context.getString(R.string.action_span_mode)) { onToggleSpanMode() }
        addMenuItem(context.getString(R.string.action_reopen_top)) { onReopenTop() }

        popup = PopupWindow(panel, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
            isOutsideTouchable = true
            elevation = dp(10).toFloat()
            setOnDismissListener { applyFloatingButtonStyle(expanded = false) }
        }
        applyFloatingButtonStyle(expanded = true)
        bottomMenuPopup?.dismiss()
        bottomMenuPopup = popup
        panel.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val menuW = panel.measuredWidth.coerceAtLeast(dp(260))
        val menuH = panel.measuredHeight.coerceAtLeast(dp(240))
        val loc = IntArray(2)
        anchor.getLocationInWindow(loc)
        val rootW = (rootLayout?.width ?: context.resources.displayMetrics.widthPixels).coerceAtLeast(menuW + dp(16))
        val rootH = (rootLayout?.height ?: context.resources.displayMetrics.heightPixels).coerceAtLeast(menuH + dp(16))
        val margin = dp(8)
        val gap = dp(8)
        val targetX = if (loc[0] + anchor.width / 2 > rootW / 2) {
            (loc[0] + anchor.width - menuW).coerceIn(margin, (rootW - menuW - margin).coerceAtLeast(margin))
        } else {
            loc[0].coerceIn(margin, (rootW - menuW - margin).coerceAtLeast(margin))
        }
        val targetY = if (loc[1] - menuH - gap >= margin) {
            loc[1] - menuH - gap
        } else {
            (loc[1] + anchor.height + gap).coerceIn(margin, (rootH - menuH - margin).coerceAtLeast(margin))
        }
        popup.showAtLocation(anchor, Gravity.NO_GRAVITY, targetX, targetY)
    }

    fun updatePresentationMode(spanActive: Boolean) {
        val floatVis = if (!spanActive) View.VISIBLE else View.GONE
        val spanVis = if (spanActive) View.VISIBLE else View.GONE
        val webVis = if (spanActive) View.GONE else View.VISIBLE
        topBarView?.post {
            if (spanActive) {
                bottomMenuPopup?.dismiss()
                bottomMenuPopup = null
            }
            topBarView?.visibility = View.GONE
            dividerView?.visibility = View.GONE
            floatingMenuBtn?.visibility = floatVis
            spanViewport?.visibility = spanVis
            webView?.visibility = webVis
            if (spanActive) {
                progressBar?.visibility = View.GONE
                webView?.onPause()
            } else {
                webView?.onResume()
                if (!hasLoadedInitialUrl) {
                    hasLoadedInitialUrl = true
                    webView?.loadUrl(currentUrl)
                }
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

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (bottomMenuPopup?.isShowing != true && urlInput?.hasFocus() != true && onForwardKeyEvent(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (bottomMenuPopup?.isShowing != true && onForwardMotionEvent(event)) {
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        webView?.destroy()
        webView = null
    }
}
