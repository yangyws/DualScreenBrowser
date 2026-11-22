package com.yangyws.dualscreenbrowser.browser

import android.annotation.SuppressLint
import android.app.Presentation
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Display
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import com.yangyws.dualscreenbrowser.R

class SecondaryBrowserPresentation(
    outerContext: Context,
    display: Display,
    private val initialUrl: String,
    private val jsEnabled: Boolean,
    private val searchEngineProvider: () -> SearchEngine,
    private val adBlockEnabledProvider: () -> Boolean,
    private val userScriptsProvider: () -> Triple<Boolean, Boolean, List<UserScript>>,
    private val onAdBlocked: () -> Unit,
    private val onSendUrlToPrimary: (String) -> Unit,
    private val onSwapScreens: (currentSecondaryUrl: String) -> Unit,
    private val onUrlChanged: (String, String) -> Unit,
    private val onRequestClose: () -> Unit
) : Presentation(outerContext, display) {

    private var webView: WebView? = null
    private var urlInput: EditText? = null
    private var progressBar: ProgressBar? = null
    private var currentUrl: String = initialUrl
    private var desktopMode: Boolean = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121216"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val topBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#1E1E24"))
            setPadding(12, 8, 12, 8)
        }

        val btnBack = navButton(android.R.drawable.ic_media_previous, context.getString(R.string.action_back)) {
            webView?.let { if (it.canGoBack()) it.goBack() }
        }
        val btnForward = navButton(android.R.drawable.ic_media_next, context.getString(R.string.action_forward)) {
            webView?.let { if (it.canGoForward()) it.goForward() }
        }
        val btnRefresh = navButton(android.R.drawable.ic_popup_sync, context.getString(R.string.action_refresh)) {
            webView?.reload()
        }

        val addressBar = EditText(context).apply {
            hint = context.getString(R.string.url_placeholder)
            setText(initialUrl)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setBackgroundColor(Color.parseColor("#2A2A34"))
            setPadding(20, 12, 20, 12)
            textSize = 14f
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
        val btnClose = navButton(android.R.drawable.ic_menu_close_clear_cancel, context.getString(R.string.action_close)) {
            onRequestClose()
        }

        topBar.addView(btnBack, LinearLayout.LayoutParams(72, 72))
        topBar.addView(btnForward, LinearLayout.LayoutParams(72, 72))
        topBar.addView(btnRefresh, LinearLayout.LayoutParams(72, 72))
        topBar.addView(
            addressBar,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(12, 0, 12, 0)
            }
        )
        topBar.addView(btnSwap, LinearLayout.LayoutParams(72, 72))
        topBar.addView(btnSendUp, LinearLayout.LayoutParams(72, 72))
        topBar.addView(btnClose, LinearLayout.LayoutParams(72, 72))

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

        root.addView(topBar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 6))
        root.addView(wv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    private fun navButton(iconRes: Int, desc: String, onClick: () -> Unit): ImageButton =
        ImageButton(context).apply {
            setImageResource(iconRes)
            contentDescription = desc
            setBackgroundColor(Color.TRANSPARENT)
            setColorFilter(Color.WHITE)
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

    fun syncSpanScrollY(targetY: Int) {
        webView?.let { wv ->
            wv.post {
                wv.scrollTo(wv.scrollX, targetY.coerceAtLeast(0))
            }
        }
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
