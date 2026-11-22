package com.yangyws.dualscreenbrowser.browser

import android.annotation.SuppressLint
import android.app.Presentation
import android.content.Context
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.text.InputType
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
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.yangyws.dualscreenbrowser.R
import com.yangyws.dualscreenbrowser.ui.SpanViewportView
import kotlin.math.abs

class SecondaryBrowserPresentation(
    outerContext: Context,
    display: Display,
    private val initialUrl: String,
    private val jsEnabled: Boolean,
    private val spanEngine: SpanEngine,
    private val searchEngineProvider: () -> SearchEngine,
    private val adBlockEnabledProvider: () -> Boolean,
    private val userScriptsProvider: () -> Triple<Boolean, Boolean, List<UserScript>>,
    private val barVisibleProvider: () -> Boolean,
    private val spanModeProvider: () -> Boolean,
    private val touchpadVisibleProvider: () -> Boolean,
    private val bottomColorProvider: () -> Pair<Int, Int>,
    private val onAdBlocked: () -> Unit,
    private val onSendUrlToPrimary: (String) -> Unit,
    private val onSwapScreens: (currentSecondaryUrl: String) -> Unit,
    private val onUrlChanged: (String, String) -> Unit,
    private val onRequestClose: () -> Unit
) : Presentation(outerContext, display) {

    private var webView: WebView? = null
    private var spanViewport: SpanViewportView? = null
    private var touchpadContainer: LinearLayout? = null
    private var restoreBarBtn: ImageButton? = null
    private var topBarView: LinearLayout? = null
    private var dividerView: View? = null
    private var urlInput: EditText? = null
    private var progressBar: ProgressBar? = null
    private var currentUrl: String = initialUrl
    private var desktopMode: Boolean = false
    private val colorPaint = Paint()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#101216"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val topBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#1A1D24"))
            setPadding(dp(8), dp(6), dp(8), dp(6))
            visibility = if (barVisibleProvider()) android.view.View.VISIBLE else android.view.View.GONE
        }
        topBarView = topBar

        val btnBack = navButton(android.R.drawable.ic_media_previous, context.getString(R.string.action_back)) {
            webView?.let { if (it.canGoBack()) it.goBack() }
        }
        val btnForward = navButton(android.R.drawable.ic_media_next, context.getString(R.string.action_forward)) {
            webView?.let { if (it.canGoForward()) it.goForward() }
        }
        val btnRefresh = navButton(android.R.drawable.ic_popup_sync, context.getString(R.string.action_refresh)) {
            webView?.reload()
        }

        val addressDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(10).toFloat()
            setColor(Color.parseColor("#252A34"))
            setStroke(dp(1), Color.parseColor("#33FFFFFF"))
        }

        val addressBar = EditText(context).apply {
            hint = context.getString(R.string.url_placeholder)
            setText(initialUrl)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#8A92A6"))
            background = addressDrawable
            setPadding(dp(12), dp(8), dp(12), dp(8))
            textSize = 13.5f
            isSingleLine = true
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_GO ||
                    (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    val target = UrlResolver.resolveInput(v.text.toString(), searchEngineProvider())
                    loadUrl(target)
                    true
                } else {
                    false
                }
            }
        }
        urlInput = addressBar

        val btnSwap = navButton(android.R.drawable.ic_menu_rotate, context.getString(R.string.action_swap_screens)) {
            onSwapScreens(currentUrl)
        }
        val btnSendUp = navButton(android.R.drawable.ic_menu_upload, context.getString(R.string.action_send_to_primary)) {
            onSendUrlToPrimary(currentUrl)
        }

        val btnSize = dp(38)
        topBar.addView(btnBack, LinearLayout.LayoutParams(btnSize, btnSize))
        topBar.addView(btnForward, LinearLayout.LayoutParams(btnSize, btnSize))
        topBar.addView(btnRefresh, LinearLayout.LayoutParams(btnSize, btnSize))
        topBar.addView(
            addressBar,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(8), 0, dp(8), 0)
            }
        )
        topBar.addView(btnSwap, LinearLayout.LayoutParams(btnSize, btnSize))
        topBar.addView(btnSendUp, LinearLayout.LayoutParams(btnSize, btnSize))

        val divider = android.view.View(context).apply {
            setBackgroundColor(Color.parseColor("#14FFFFFF"))
            visibility = if (barVisibleProvider()) android.view.View.VISIBLE else android.view.View.GONE
        }
        dividerView = divider

        val progress = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
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

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (!url.isNullOrBlank()) {
                        currentUrl = url
                        urlInput?.setText(url)
                        val (forceDark, enableCopy, customScripts) = userScriptsProvider()
                        UserScriptManager.injectScripts(view, url, forceDark, enableCopy, customScripts)
                        onUrlChanged(url, view?.title.orEmpty())
                    }
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
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

        val restorePillBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#CC1A1D24"))
            setStroke(dp(1), Color.parseColor("#44FFFFFF"))
        }
        val btnRestoreBar = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_menu_more)
            contentDescription = context.getString(R.string.action_secondary_bar)
            background = restorePillBg
            setColorFilter(Color.WHITE)
            setPadding(dp(6), dp(6), dp(6), dp(6))
            visibility = if (!spanModeProvider() && !barVisibleProvider()) android.view.View.VISIBLE else android.view.View.GONE
            setOnClickListener {
                updatePresentationMode(barVisible = true, spanActive = false)
            }
        }
        restoreBarBtn = btnRestoreBar
        contentStack.addView(spanView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        contentStack.addView(
            btnRestoreBar,
            FrameLayout.LayoutParams(dp(34), dp(34)).apply {
                gravity = Gravity.TOP or Gravity.END
                setMargins(0, dp(8), dp(8), 0)
            }
        )

        root.addView(topBar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(divider, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)))
        root.addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(2)))
        root.addView(contentStack, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(createTouchpadArea(), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(112)))
        setContentView(root)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createTouchpadArea(): LinearLayout {
        var lastX = 0f
        var lastY = 0f
        var totalMove = 0f
        var isScrollZone = false
        val scrollZoneWidthPx = dp(40)
        val tapSlopPx = dp(10)

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#14171E"))
            visibility = if (touchpadVisibleProvider()) View.VISIBLE else View.GONE
        }
        touchpadContainer = container

        val topDivider = View(context).apply {
            setBackgroundColor(Color.parseColor("#24FFFFFF"))
        }
        val bodyRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val hintLabel = TextView(context).apply {
            text = context.getString(R.string.touchpad_hint)
            setTextColor(Color.parseColor("#6E7687"))
            textSize = 11.5f
            gravity = Gravity.CENTER
            setPadding(dp(12), 0, dp(12), 0)
        }
        val scrollTrack = View(context).apply {
            setBackgroundColor(Color.parseColor("#1AFFFFFF"))
        }

        bodyRow.addView(hintLabel, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        bodyRow.addView(scrollTrack, LinearLayout.LayoutParams(dp(4), ViewGroup.LayoutParams.MATCH_PARENT).apply {
            setMargins(0, dp(14), dp(10), dp(14))
        })

        container.addView(topDivider, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)))
        container.addView(bodyRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        container.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.x
                    lastY = event.y
                    totalMove = 0f
                    isScrollZone = event.x >= v.width - scrollZoneWidthPx
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    totalMove += abs(dx) + abs(dy)
                    if (isScrollZone || event.pointerCount >= 2) {
                        val scrollDy = (-dy * 2.2f).toInt()
                        if (spanModeProvider()) {
                            spanEngine.scrollByY(scrollDy)
                        } else {
                            webView?.scrollBy(0, scrollDy)
                        }
                    } else if (spanModeProvider()) {
                        spanEngine.moveCursorBy(dx * 1.35f, dy * 1.35f)
                    }
                    lastX = event.x
                    lastY = event.y
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (totalMove < tapSlopPx && !isScrollZone && spanModeProvider()) {
                        spanEngine.tapAtCursor()
                    }
                    true
                }
                else -> false
            }
        }
        return container
    }

    fun updateTouchpadVisibility(visible: Boolean) {
        touchpadContainer?.post {
            touchpadContainer?.visibility = if (visible) View.VISIBLE else View.GONE
            touchpadContainer?.parent?.requestLayout()
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
            setPadding(dp(7), dp(7), dp(7), dp(7))
            setOnClickListener { onClick() }
        }

    fun loadUrl(url: String) {
        currentUrl = url
        urlInput?.setText(url)
        webView?.loadUrl(url)
    }

    fun currentUrl(): String = currentUrl

    fun updateJsEnabled(enabled: Boolean) {
        webView?.settings?.javaScriptEnabled = enabled
    }

    fun scrollByY(dy: Int) {
        webView?.scrollBy(0, dy)
    }

    fun updatePresentationMode(barVisible: Boolean, spanActive: Boolean) {
        val barVis = if (barVisible && !spanActive) View.VISIBLE else View.GONE
        val restoreVis = if (!barVisible && !spanActive) View.VISIBLE else View.GONE
        val spanVis = if (spanActive) View.VISIBLE else View.GONE
        val webVis = if (spanActive) View.GONE else View.VISIBLE
        topBarView?.post {
            topBarView?.visibility = barVis
            dividerView?.visibility = barVis
            restoreBarBtn?.visibility = restoreVis
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
            wv.setLayerType(View.LAYER_TYPE_HARDWARE, null)
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
