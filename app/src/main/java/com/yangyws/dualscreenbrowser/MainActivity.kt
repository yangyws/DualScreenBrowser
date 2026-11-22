package com.yangyws.dualscreenbrowser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.SystemClock
import android.view.Display
import android.view.KeyEvent
import android.view.MotionEvent
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material.icons.rounded.ViewStream
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.yangyws.dualscreenbrowser.browser.AdBlocker
import com.yangyws.dualscreenbrowser.browser.BookmarkItem
import com.yangyws.dualscreenbrowser.browser.BrowserPrefs
import com.yangyws.dualscreenbrowser.browser.BrowserTab
import com.yangyws.dualscreenbrowser.browser.GamepadActions
import com.yangyws.dualscreenbrowser.browser.GamepadController
import com.yangyws.dualscreenbrowser.browser.GamepadTargetScreen
import com.yangyws.dualscreenbrowser.browser.HistoryItem
import com.yangyws.dualscreenbrowser.browser.SearchEngine
import com.yangyws.dualscreenbrowser.browser.SecondaryBrowserPresentation
import com.yangyws.dualscreenbrowser.browser.SpanEngine
import com.yangyws.dualscreenbrowser.browser.SpanSource
import com.yangyws.dualscreenbrowser.browser.UrlResolver
import com.yangyws.dualscreenbrowser.browser.UserScript
import com.yangyws.dualscreenbrowser.browser.UserScriptManager
import com.yangyws.dualscreenbrowser.system.AppLanguage
import com.yangyws.dualscreenbrowser.system.LocaleHelper
import com.yangyws.dualscreenbrowser.ui.EmptyStateView
import com.yangyws.dualscreenbrowser.ui.SpanViewportView
import com.yangyws.dualscreenbrowser.ui.StatusChip
import com.yangyws.dualscreenbrowser.ui.StrongEaseOut
import com.yangyws.dualscreenbrowser.ui.TactileIconButton

private enum class BrowserPanel {
    NONE,
    TABS,
    BOOKMARKS,
    HISTORY,
    USERSCRIPTS,
    GAMEPAD_GUIDE,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    private var launchUrlState by mutableStateOf<String?>(null)
    private var gamepadActions: GamepadActions? = null
    private var gamepadEnabledFlag: Boolean = true
    private var isEditingTextFlag: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchUrlState = intent?.dataString
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF4CC38A),
                    background = Color(0xFF101216),
                    surface = Color(0xFF1A1D24),
                    surfaceVariant = Color(0xFF252A34)
                )
            ) {
                DualScreenBrowserApp(
                    externalUrl = launchUrlState,
                    onConsumeExternalUrl = { launchUrlState = null },
                    onRegisterGamepad = { actions, enabled, editing ->
                        gamepadActions = actions
                        gamepadEnabledFlag = enabled
                        isEditingTextFlag = editing
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.dataString?.let { launchUrlState = it }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val actions = gamepadActions
        if (actions != null &&
            GamepadController.handleKeyEvent(event, gamepadEnabledFlag, isEditingTextFlag, actions)
        ) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val actions = gamepadActions
        if (actions != null &&
            GamepadController.handleGenericMotionEvent(event, gamepadEnabledFlag, actions)
        ) {
            return true
        }
        return super.onGenericMotionEvent(event)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun DualScreenBrowserApp(
    externalUrl: String?,
    onConsumeExternalUrl: () -> Unit,
    onRegisterGamepad: (GamepadActions, Boolean, Boolean) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val prefs = remember { BrowserPrefs(context) }

    var tabs by remember {
        mutableStateOf(listOf(BrowserTab(url = externalUrl ?: BrowserTab.DEFAULT_HOME_URL)))
    }
    var activeTabId by remember { mutableStateOf(tabs.first().id) }
    val activeTab = tabs.firstOrNull { it.id == activeTabId } ?: tabs.first()

    var addressText by remember(activeTab.id) { mutableStateOf(activeTab.url) }
    var loadProgress by remember { mutableFloatStateOf(0f) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    var bookmarks by remember { mutableStateOf(prefs.loadBookmarks()) }
    var history by remember { mutableStateOf(prefs.loadHistory()) }
    var searchEngine by remember { mutableStateOf(prefs.searchEngine) }
    var autoDualScreen by remember { mutableStateOf(prefs.autoDualScreen) }
    var jsEnabled by remember { mutableStateOf(prefs.jsEnabled) }
    var adBlockEnabled by remember { mutableStateOf(prefs.adBlockEnabled) }
    var adBlockedCount by remember { mutableIntStateOf(AdBlocker.blockedCount()) }
    var gamepadEnabled by remember { mutableStateOf(prefs.gamepadEnabled) }
    var gamepadTarget by remember { mutableStateOf(GamepadTargetScreen.TOP) }
    var forceDarkWeb by remember { mutableStateOf(prefs.forceDarkWeb) }
    var enableCopyScript by remember { mutableStateOf(prefs.enableCopyScript) }
    var userScripts by remember { mutableStateOf(prefs.loadUserScripts()) }
    var spanMode by remember { mutableStateOf(true) }
    var spanSource by remember { mutableStateOf(prefs.spanSource) }
    var showSecondaryBar by remember { mutableStateOf(prefs.showSecondaryBar) }
    val spanEngine = remember { SpanEngine() }
    var topViewportSize by remember { mutableStateOf(IntSize.Zero) }
    var isEditingText by remember { mutableStateOf(false) }
    var findBarVisible by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var findMatchActive by remember { mutableIntStateOf(0) }
    var findMatchTotal by remember { mutableIntStateOf(0) }
    var overflowMenuExpanded by remember { mutableStateOf(false) }
    var activePanel by remember { mutableStateOf(BrowserPanel.NONE) }

    val webViews = remember { mutableStateMapOf<String, WebView>() }

    // Secondary display state
    val displayManager = remember {
        context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }
    var secondaryDisplay by remember {
        mutableStateOf(findSecondaryDisplay(displayManager))
    }
    var dualScreenActive by remember {
        mutableStateOf(secondaryDisplay != null)
    }
    var secondaryUrl by remember { mutableStateOf("https://duckduckgo.com") }
    var presentation by remember { mutableStateOf<SecondaryBrowserPresentation?>(null) }

    LaunchedEffect(externalUrl) {
        if (!externalUrl.isNullOrBlank()) {
            val newTab = BrowserTab(url = externalUrl)
            tabs = tabs + newTab
            activeTabId = newTab.id
            addressText = externalUrl
            onConsumeExternalUrl()
        }
    }

    // Watch for secondary display plug/unplug
    DisposableEffect(displayManager) {
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {
                secondaryDisplay = findSecondaryDisplay(displayManager)
                if (secondaryDisplay != null) {
                    dualScreenActive = true
                }
            }

            override fun onDisplayRemoved(displayId: Int) {
                val found = findSecondaryDisplay(displayManager)
                secondaryDisplay = found
                if (found == null) dualScreenActive = false
            }

            override fun onDisplayChanged(displayId: Int) = Unit
        }
        displayManager.registerDisplayListener(listener, null)
        onDispose {
            displayManager.unregisterDisplayListener(listener)
        }
    }

    // Manage SecondaryBrowserPresentation lifecycle
    DisposableEffect(dualScreenActive, secondaryDisplay, jsEnabled) {
        val targetDisplay = secondaryDisplay
        if (dualScreenActive && targetDisplay != null) {
            val pres = SecondaryBrowserPresentation(
                outerContext = context.createDisplayContext(targetDisplay),
                display = targetDisplay,
                initialUrl = secondaryUrl,
                jsEnabled = jsEnabled,
                spanEngine = spanEngine,
                searchEngineProvider = { searchEngine },
                adBlockEnabledProvider = { adBlockEnabled },
                userScriptsProvider = { Triple(forceDarkWeb, enableCopyScript, userScripts) },
                barVisibleProvider = { showSecondaryBar && !spanMode },
                spanModeProvider = { spanMode },
                onAdBlocked = { adBlockedCount = AdBlocker.blockedCount() },
                onSendUrlToPrimary = { urlFromBottom ->
                    webViews[activeTabId]?.loadUrl(urlFromBottom)
                    addressText = urlFromBottom
                    Toast.makeText(context, context.getString(R.string.toast_sent_to_top), Toast.LENGTH_SHORT).show()
                },
                onSwapScreens = { urlFromBottom ->
                    val topUrl = activeTab.url
                    webViews[activeTabId]?.loadUrl(urlFromBottom)
                    addressText = urlFromBottom
                    secondaryUrl = topUrl
                    presentation?.loadUrl(topUrl)
                    Toast.makeText(context, context.getString(R.string.toast_swapped_screens), Toast.LENGTH_SHORT).show()
                },
                onUrlChanged = { url, title ->
                    secondaryUrl = url
                    val entry = HistoryItem(title = title.ifBlank { url }, url = url)
                    history = (listOf(entry) + history.filterNot { it.url == url }).take(100)
                    prefs.saveHistory(history)
                },
                onRequestClose = {
                    dualScreenActive = false
                    spanMode = false
                }
            )
            pres.show()
            presentation = pres
        } else {
            presentation?.dismiss()
            presentation = null
        }
        onDispose {
            presentation?.dismiss()
            presentation = null
        }
    }

    LaunchedEffect(spanMode, showSecondaryBar, dualScreenActive, presentation) {
        presentation?.updatePresentationMode(showSecondaryBar, spanMode && dualScreenActive)
    }

    val viewportReady = topViewportSize.width > 0 && topViewportSize.height > 0

    // Start/stop VirtualDisplay SpanEngine when Span Mode toggles
    DisposableEffect(spanMode, dualScreenActive, secondaryDisplay, jsEnabled, viewportReady) {
        val secDisplay = secondaryDisplay
        if (spanMode && dualScreenActive && secDisplay != null && viewportReady) {
            val topW = topViewportSize.width
            val topH = topViewportSize.height
            val secMetrics = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            secDisplay.getRealMetrics(secMetrics)
            val secAspect = if (secMetrics.widthPixels > 0) {
                secMetrics.heightPixels.toFloat() / secMetrics.widthPixels.toFloat()
            } else {
                1f
            }
            val bottomH = (topW * secAspect).toInt().coerceAtLeast(240)
            val dpi = context.resources.displayMetrics.densityDpi

            val spanInitialUrl = if (spanSource == SpanSource.BOTTOM) {
                presentation?.currentUrl()?.takeIf { it.isNotBlank() } ?: secondaryUrl
            } else {
                activeTab.url
            }

            spanEngine.start(
                context = context,
                widthPx = topW,
                topHeightPx = topH,
                bottomHeightPx = bottomH,
                dpi = dpi,
                initialUrl = spanInitialUrl,
                jsEnabled = jsEnabled,
                adBlockEnabledProvider = { adBlockEnabled },
                userScriptsProvider = { Triple(forceDarkWeb, enableCopyScript, userScripts) },
                onAdBlocked = { adBlockedCount = AdBlocker.blockedCount() },
                onPageInfoChanged = { url, title, canBack, canForward ->
                    addressText = url
                    canGoBack = canBack
                    canGoForward = canForward
                    if (spanSource == SpanSource.BOTTOM) {
                        secondaryUrl = url
                    } else {
                        tabs = tabs.map {
                            if (it.id == activeTab.id) it.copy(url = url, title = title) else it
                        }
                    }
                    if (!activeTab.isIncognito) {
                        val entry = HistoryItem(title = title.ifBlank { url }, url = url)
                        history = (listOf(entry) + history.filterNot { it.url == url }).take(100)
                        prefs.saveHistory(history)
                    }
                },
                onProgressChanged = { newProgress ->
                    loadProgress = if (newProgress in 1..99) newProgress / 100f else 0f
                }
            )
        } else {
            val lastSpanUrl = spanEngine.currentUrl()
            spanEngine.stop()
            if (!lastSpanUrl.isNullOrBlank()) {
                if (spanSource == SpanSource.BOTTOM) {
                    secondaryUrl = lastSpanUrl
                    presentation?.loadUrl(lastSpanUrl)
                } else if (lastSpanUrl != webViews[activeTabId]?.url) {
                    webViews[activeTabId]?.loadUrl(lastSpanUrl)
                }
            }
        }
        onDispose {
            spanEngine.stop()
        }
    }

    // Cleanup WebViews when activity disposes
    DisposableEffect(Unit) {
        onDispose {
            spanEngine.stop()
            webViews.values.forEach { it.destroy() }
            webViews.clear()
        }
    }

    BackHandler(enabled = activePanel != BrowserPanel.NONE || canGoBack) {
        if (activePanel != BrowserPanel.NONE) {
            activePanel = BrowserPanel.NONE
        } else {
            webViews[activeTabId]?.let { wv ->
                if (wv.canGoBack()) wv.goBack()
            }
        }
    }

    val isBookmarked = bookmarks.any { it.url == activeTab.url }

    fun navigateActiveTab(rawInput: String) {
        val target = UrlResolver.resolveInput(rawInput, searchEngine)
        addressText = target
        if (spanMode && spanEngine.isRunning()) {
            spanEngine.loadUrl(target)
        } else {
            webViews[activeTabId]?.loadUrl(target)
        }
        focusManager.clearFocus()
    }

    fun openTab(url: String = BrowserTab.DEFAULT_HOME_URL, incognito: Boolean = false) {
        val tab = BrowserTab(url = url, isIncognito = incognito)
        tabs = tabs + tab
        activeTabId = tab.id
        addressText = url
        activePanel = BrowserPanel.NONE
    }

    fun closeTab(tabId: String) {
        val removed = webViews.remove(tabId)
        removed?.destroy()
        val remaining = tabs.filterNot { it.id == tabId }
        if (remaining.isEmpty()) {
            val fresh = BrowserTab()
            tabs = listOf(fresh)
            activeTabId = fresh.id
            addressText = fresh.url
        } else {
            tabs = remaining
            if (activeTabId == tabId) {
                val next = remaining.last()
                activeTabId = next.id
                addressText = next.url
            }
        }
    }

    val gamepadActions = remember(activeTabId, tabs, dualScreenActive, gamepadTarget, spanMode) {
        object : GamepadActions {
            override fun scrollActive(dy: Int) {
                if (spanMode && spanEngine.isRunning()) {
                    spanEngine.scrollByY(dy)
                } else if (gamepadTarget == GamepadTargetScreen.BOTTOM && dualScreenActive && presentation != null) {
                    presentation?.scrollByY(dy)
                } else {
                    webViews[activeTabId]?.scrollBy(0, dy)
                }
            }

            override fun goBackActive() {
                if (spanMode && spanEngine.isRunning()) {
                    spanEngine.goBack()
                } else if (gamepadTarget == GamepadTargetScreen.BOTTOM && dualScreenActive && presentation != null) {
                    presentation?.goBackIfPossible()
                } else {
                    webViews[activeTabId]?.let { if (it.canGoBack()) it.goBack() }
                }
            }

            override fun goForwardActive() {
                if (spanMode && spanEngine.isRunning()) {
                    spanEngine.goForward()
                } else if (gamepadTarget == GamepadTargetScreen.BOTTOM && dualScreenActive && presentation != null) {
                    presentation?.goForwardIfPossible()
                } else {
                    webViews[activeTabId]?.let { if (it.canGoForward()) it.goForward() }
                }
            }

            override fun previousTab() {
                if (tabs.size <= 1) return
                val idx = tabs.indexOfFirst { it.id == activeTabId }.coerceAtLeast(0)
                val prev = tabs[(idx - 1 + tabs.size) % tabs.size]
                activeTabId = prev.id
                addressText = prev.url
            }

            override fun nextTab() {
                if (tabs.size <= 1) return
                val idx = tabs.indexOfFirst { it.id == activeTabId }.coerceAtLeast(0)
                val next = tabs[(idx + 1) % tabs.size]
                activeTabId = next.id
                addressText = next.url
            }

            override fun refreshActive() {
                if (spanMode && spanEngine.isRunning()) {
                    spanEngine.reload()
                } else if (gamepadTarget == GamepadTargetScreen.BOTTOM && dualScreenActive && presentation != null) {
                    presentation?.reloadPage()
                } else {
                    webViews[activeTabId]?.reload()
                }
            }

            override fun swapScreens() {
                if (!dualScreenActive || presentation == null) return
                val bottomUrl = presentation?.currentUrl() ?: secondaryUrl
                val topUrl = activeTab.url
                navigateActiveTab(bottomUrl)
                secondaryUrl = topUrl
                presentation?.loadUrl(topUrl)
                Toast.makeText(context, context.getString(R.string.toast_swapped_screens), Toast.LENGTH_SHORT).show()
            }

            override fun toggleTargetScreen() {
                if (!dualScreenActive || presentation == null) {
                    gamepadTarget = GamepadTargetScreen.TOP
                    return
                }
                gamepadTarget = if (gamepadTarget == GamepadTargetScreen.TOP) {
                    GamepadTargetScreen.BOTTOM
                } else {
                    GamepadTargetScreen.TOP
                }
                val msgRes = if (gamepadTarget == GamepadTargetScreen.TOP) {
                    R.string.toast_gamepad_target_top
                } else {
                    R.string.toast_gamepad_target_bottom
                }
                Toast.makeText(context, context.getString(msgRes), Toast.LENGTH_SHORT).show()
            }

            override fun toggleSpanMode() {
                if (!dualScreenActive || secondaryDisplay == null) return
                spanMode = !spanMode
                presentation?.updatePresentationMode(showSecondaryBar, spanMode && dualScreenActive)
                val msgRes = if (spanMode) R.string.toast_span_on else R.string.toast_span_off
                Toast.makeText(context, context.getString(msgRes), Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(gamepadActions, gamepadEnabled, isEditingText, activePanel, findBarVisible) {
        onRegisterGamepad(gamepadActions, gamepadEnabled, isEditingText || activePanel != BrowserPanel.NONE || findBarVisible)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Top Address & Quick Actions Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            TactileIconButton(
                onClick = {
                    if (spanMode && spanEngine.isRunning()) {
                        spanEngine.goBack()
                    } else {
                        webViews[activeTabId]?.let { if (it.canGoBack()) it.goBack() }
                    }
                },
                enabled = canGoBack
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back), modifier = Modifier.size(20.dp))
            }
            TactileIconButton(
                onClick = {
                    if (spanMode && spanEngine.isRunning()) {
                        spanEngine.goForward()
                    } else {
                        webViews[activeTabId]?.let { if (it.canGoForward()) it.goForward() }
                    }
                },
                enabled = canGoForward
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = stringResource(R.string.action_forward), modifier = Modifier.size(20.dp))
            }
            TactileIconButton(
                onClick = {
                    if (spanMode && spanEngine.isRunning()) {
                        spanEngine.reload()
                    } else {
                        val wv = webViews[activeTabId]
                        if (loadProgress in 0.01f..0.99f) wv?.stopLoading() else wv?.reload()
                    }
                }
            ) {
                Icon(
                    imageVector = if (loadProgress in 0.01f..0.99f) Icons.Rounded.Close else Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.action_refresh),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Address input box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (activeTab.isIncognito) Color(0xFF2B2139) else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        width = 1.dp,
                        color = if (isEditingText) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                        } else {
                            Color.White.copy(alpha = 0.08f)
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (activeTab.isIncognito) {
                        Icon(
                            Icons.Rounded.VisibilityOff,
                            contentDescription = stringResource(R.string.action_incognito),
                            tint = Color(0xFFCE93D8),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    BasicTextField(
                        value = addressText,
                        onValueChange = { addressText = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 13.5.sp),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = { navigateActiveTab(addressText) }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { isEditingText = it.isFocused },
                        decorationBox = { inner ->
                            if (addressText.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.url_placeholder),
                                    color = Color(0xFF8A92A6),
                                    fontSize = 13.5.sp
                                )
                            }
                            inner()
                        }
                    )
                    if (isEditingText && addressText.isNotEmpty()) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.action_close),
                            tint = Color(0xFF8A92A6),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { addressText = "" }
                        )
                    }
                }
            }

            // Tabs count pill
            Surface(
                onClick = { activePanel = BrowserPanel.TABS },
                shape = RoundedCornerShape(9.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Tab, contentDescription = stringResource(R.string.action_tabs), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.action_tabs_count, tabs.size),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }
            }

            // Overflow menu with all secondary tools (Bookmarks, History, Find, Desktop, Dual-Screen, Settings)
            Box {
                TactileIconButton(
                    onClick = { overflowMenuExpanded = true },
                    active = overflowMenuExpanded
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.action_settings),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = overflowMenuExpanded,
                    onDismissRequest = { overflowMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (isBookmarked) R.string.action_remove_bookmark else R.string.action_add_bookmark
                                )
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                contentDescription = null,
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else Color.White
                            )
                        },
                        onClick = {
                            overflowMenuExpanded = false
                            if (isBookmarked) {
                                bookmarks = bookmarks.filterNot { it.url == activeTab.url }
                                prefs.saveBookmarks(bookmarks)
                                Toast.makeText(context, context.getString(R.string.toast_bookmark_removed), Toast.LENGTH_SHORT).show()
                            } else {
                                val entry = BookmarkItem(
                                    title = activeTab.title.ifBlank { activeTab.url },
                                    url = activeTab.url
                                )
                                bookmarks = listOf(entry) + bookmarks
                                prefs.saveBookmarks(bookmarks)
                                Toast.makeText(context, context.getString(R.string.toast_bookmark_added), Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_bookmarks)) },
                        leadingIcon = { Icon(Icons.Rounded.Bookmark, contentDescription = null) },
                        onClick = {
                            overflowMenuExpanded = false
                            activePanel = BrowserPanel.BOOKMARKS
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_history)) },
                        leadingIcon = { Icon(Icons.Rounded.History, contentDescription = null) },
                        onClick = {
                            overflowMenuExpanded = false
                            activePanel = BrowserPanel.HISTORY
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_find_in_page)) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        onClick = {
                            overflowMenuExpanded = false
                            findBarVisible = !findBarVisible
                            if (!findBarVisible) {
                                findQuery = ""
                                findMatchActive = 0
                                findMatchTotal = 0
                                webViews[activeTabId]?.clearMatches()
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_desktop_mode)) },
                        leadingIcon = { Icon(Icons.Rounded.Computer, contentDescription = null) },
                        trailingIcon = {
                            if (activeTab.desktopMode) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        onClick = {
                            overflowMenuExpanded = false
                            val nextDesktop = !activeTab.desktopMode
                            tabs = tabs.map { if (it.id == activeTab.id) it.copy(desktopMode = nextDesktop) else it }
                            webViews[activeTabId]?.let { wv ->
                                val defaultUa = WebSettings.getDefaultUserAgent(context)
                                wv.settings.userAgentString = if (nextDesktop) {
                                    defaultUa.replace("Mobile", "").replace("Android", "X11; Linux x86_64")
                                } else {
                                    defaultUa
                                }
                                wv.reload()
                            }
                        }
                    )
                    if (dualScreenActive && secondaryDisplay != null) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_span_mode)) },
                            leadingIcon = { Icon(Icons.Rounded.ViewStream, contentDescription = null) },
                            trailingIcon = {
                                if (spanMode) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            onClick = {
                                overflowMenuExpanded = false
                                spanMode = !spanMode
                                presentation?.updatePresentationMode(showSecondaryBar, spanMode && dualScreenActive)
                                val msgRes = if (spanMode) R.string.toast_span_on else R.string.toast_span_off
                                Toast.makeText(context, context.getString(msgRes), Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        if (spanSource == SpanSource.TOP) {
                                            R.string.action_span_source_top
                                        } else {
                                            R.string.action_span_source_bottom
                                        }
                                    )
                                )
                            },
                            leadingIcon = { Icon(Icons.Rounded.SwapVert, contentDescription = null) },
                            onClick = {
                                overflowMenuExpanded = false
                                val next = if (spanSource == SpanSource.TOP) SpanSource.BOTTOM else SpanSource.TOP
                                spanSource = next
                                prefs.spanSource = next
                                val labelRes = if (next == SpanSource.TOP) {
                                    R.string.action_span_source_top
                                } else {
                                    R.string.action_span_source_bottom
                                }
                                Toast.makeText(context, context.getString(labelRes), Toast.LENGTH_SHORT).show()
                                if (spanMode && spanEngine.isRunning()) {
                                    val targetUrl = if (next == SpanSource.BOTTOM) {
                                        secondaryUrl
                                    } else {
                                        activeTab.url
                                    }
                                    if (targetUrl.isNotBlank() && targetUrl != spanEngine.currentUrl()) {
                                        spanEngine.loadUrl(targetUrl)
                                    }
                                }
                            }
                        )
                        if (!spanMode) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_send_to_secondary)) },
                                leadingIcon = { Icon(Icons.Rounded.OpenInBrowser, contentDescription = null) },
                                onClick = {
                                    overflowMenuExpanded = false
                                    secondaryUrl = activeTab.url
                                    presentation?.loadUrl(activeTab.url)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.toast_sent_to_bottom),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_swap_screens)) },
                                leadingIcon = { Icon(Icons.Rounded.SwapVert, contentDescription = null) },
                                onClick = {
                                    overflowMenuExpanded = false
                                    val bottomUrl = presentation?.currentUrl() ?: secondaryUrl
                                    val topUrl = activeTab.url
                                    navigateActiveTab(bottomUrl)
                                    secondaryUrl = topUrl
                                    presentation?.loadUrl(topUrl)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.toast_swapped_screens),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_secondary_bar)) },
                                leadingIcon = { Icon(Icons.Rounded.Devices, contentDescription = null) },
                                trailingIcon = {
                                    if (showSecondaryBar) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                onClick = {
                                    overflowMenuExpanded = false
                                    showSecondaryBar = !showSecondaryBar
                                    prefs.showSecondaryBar = showSecondaryBar
                                    presentation?.updatePresentationMode(showSecondaryBar, false)
                                }
                            )
                        }
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_gamepad_guide)) },
                        leadingIcon = { Icon(Icons.Rounded.SportsEsports, contentDescription = null) },
                        onClick = {
                            overflowMenuExpanded = false
                            activePanel = BrowserPanel.GAMEPAD_GUIDE
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_settings)) },
                        leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                        onClick = {
                            overflowMenuExpanded = false
                            activePanel = BrowserPanel.SETTINGS
                        }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = findBarVisible,
            enter = expandVertically(tween(180, easing = StrongEaseOut)) + fadeIn(tween(140)),
            exit = shrinkVertically(tween(150, easing = StrongEaseOut)) + fadeOut(tween(110))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = findQuery,
                    onValueChange = { q ->
                        findQuery = q
                        val wv = webViews[activeTabId]
                        if (wv != null) {
                            if (q.isEmpty()) {
                                wv.clearMatches()
                                findMatchActive = 0
                                findMatchTotal = 0
                            } else {
                                wv.setFindListener { activeOrdinal, numberOfMatches, _ ->
                                    findMatchTotal = numberOfMatches
                                    findMatchActive = if (numberOfMatches > 0) activeOrdinal + 1 else 0
                                }
                                wv.findAllAsync(q)
                            }
                        }
                    },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (findQuery.isEmpty()) {
                            Text(
                                text = stringResource(R.string.find_placeholder),
                                color = Color(0xFF8A92A6),
                                fontSize = 14.sp
                            )
                        }
                        inner()
                    }
                )
                Text(
                    text = stringResource(R.string.find_matches, findMatchActive, findMatchTotal),
                    fontSize = 12.sp,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    color = Color.LightGray,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                TactileIconButton(
                    onClick = { webViews[activeTabId]?.findNext(false) },
                    size = 32.dp
                ) {
                    Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                TactileIconButton(
                    onClick = { webViews[activeTabId]?.findNext(true) },
                    size = 32.dp
                ) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                TactileIconButton(
                    onClick = {
                        findBarVisible = false
                        findQuery = ""
                        findMatchActive = 0
                        findMatchTotal = 0
                        webViews[activeTabId]?.clearMatches()
                    },
                    size = 32.dp
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_close), modifier = Modifier.size(18.dp))
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

        val animatedProgress by animateFloatAsState(
            targetValue = loadProgress,
            animationSpec = tween(durationMillis = 120, easing = LinearEasing),
            label = "webProgress"
        )
        if (loadProgress in 0.01f..0.99f) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            )
        }

        // Main WebView container (uses full vertical space below top bar)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding()
                .onGloballyPositioned { topViewportSize = it.size }
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = jsEnabled
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        CookieManager.getInstance().setAcceptCookie(true)
                    }
                },
                update = { container ->
                    // Keep active WebView synced
                    container.settings.javaScriptEnabled = jsEnabled
                    if (webViews[activeTab.id] !== container) {
                        webViews[activeTab.id] = container
                        container.webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val reqUrl = request?.url?.toString()
                                if (adBlockEnabled && AdBlocker.shouldBlockUrl(reqUrl)) {
                                    AdBlocker.recordBlocked()
                                    adBlockedCount = AdBlocker.blockedCount()
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
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                                if (!url.isNullOrBlank()) {
                                    val pageTitle = view?.title.orEmpty()
                                    addressText = url
                                    tabs = tabs.map {
                                        if (it.id == activeTab.id) it.copy(url = url, title = pageTitle) else it
                                    }
                                    UserScriptManager.injectScripts(
                                        view,
                                        url,
                                        forceDarkWeb,
                                        enableCopyScript,
                                        userScripts
                                    )
                                    if (!activeTab.isIncognito) {
                                        val entry = HistoryItem(title = pageTitle.ifBlank { url }, url = url)
                                        history = (listOf(entry) + history.filterNot { it.url == url }).take(100)
                                        prefs.saveHistory(history)
                                    }
                                }
                            }
                        }
                        container.webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadProgress = if (newProgress in 1..99) newProgress / 100f else 0f
                            }
                        }
                        if (container.url != activeTab.url) {
                            container.loadUrl(activeTab.url)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (spanMode && dualScreenActive) {
                AndroidView(
                    factory = { ctx -> SpanViewportView(ctx, spanEngine, isBottomViewport = false) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Modal Panels
    when (activePanel) {
        BrowserPanel.NONE -> Unit

        BrowserPanel.TABS -> {
            AlertDialog(
                onDismissRequest = { activePanel = BrowserPanel.NONE },
                title = { Text(stringResource(R.string.action_tabs)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { openTab(incognito = false) }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.action_new_tab))
                            }
                            OutlinedButton(onClick = { openTab(incognito = true) }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.action_incognito))
                            }
                        }
                        LazyColumn(modifier = Modifier.height(260.dp)) {
                            items(tabs, key = { it.id }) { tab ->
                                val isCurrent = tab.id == activeTab.id
                                Surface(
                                    color = if (isCurrent) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            activeTabId = tab.id
                                            addressText = tab.url
                                            activePanel = BrowserPanel.NONE
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = tab.title.ifBlank { stringResource(R.string.untitled_page) },
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = tab.url,
                                                fontSize = 11.sp,
                                                color = Color.Gray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(onClick = { closeTab(tab.id) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_close))
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activePanel = BrowserPanel.NONE }) {
                        Text(stringResource(R.string.action_close))
                    }
                }
            )
        }

        BrowserPanel.BOOKMARKS -> {
            AlertDialog(
                onDismissRequest = { activePanel = BrowserPanel.NONE },
                title = { Text(stringResource(R.string.action_bookmarks)) },
                text = {
                    if (bookmarks.isEmpty()) {
                        EmptyStateView(Icons.Rounded.BookmarkBorder, stringResource(R.string.no_bookmarks))
                    } else {
                        LazyColumn(modifier = Modifier.height(280.dp)) {
                            items(bookmarks, key = { it.url }) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            navigateActiveTab(item.url)
                                            activePanel = BrowserPanel.NONE
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(item.url, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    IconButton(onClick = {
                                        bookmarks = bookmarks.filterNot { it.url == item.url }
                                        prefs.saveBookmarks(bookmarks)
                                    }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.action_remove_bookmark))
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activePanel = BrowserPanel.NONE }) {
                        Text(stringResource(R.string.action_close))
                    }
                }
            )
        }

        BrowserPanel.HISTORY -> {
            AlertDialog(
                onDismissRequest = { activePanel = BrowserPanel.NONE },
                title = { Text(stringResource(R.string.action_history)) },
                text = {
                    if (history.isEmpty()) {
                        EmptyStateView(Icons.Rounded.History, stringResource(R.string.no_history))
                    } else {
                        LazyColumn(modifier = Modifier.height(280.dp)) {
                            items(history, key = { "${it.url}_${it.visitedAtMs}" }) { item ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            navigateActiveTab(item.url)
                                            activePanel = BrowserPanel.NONE
                                        }
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(item.url, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                },
                dismissButton = {
                    if (history.isNotEmpty()) {
                        TextButton(onClick = {
                            history = emptyList()
                            prefs.saveHistory(emptyList())
                        }) {
                            Text(stringResource(R.string.action_clear_history))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activePanel = BrowserPanel.NONE }) {
                        Text(stringResource(R.string.action_close))
                    }
                }
            )
        }

        BrowserPanel.USERSCRIPTS -> {
            var newName by remember { mutableStateOf("") }
            var newPattern by remember { mutableStateOf("*") }
            var newCode by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { activePanel = BrowserPanel.NONE },
                title = { Text(stringResource(R.string.userscripts_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text(stringResource(R.string.userscripts_name_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newPattern,
                            onValueChange = { newPattern = it },
                            label = { Text(stringResource(R.string.userscripts_pattern_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newCode,
                            onValueChange = { newCode = it },
                            label = { Text(stringResource(R.string.userscripts_code_hint)) },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (newName.isNotBlank() && newCode.isNotBlank()) {
                                    val added = UserScript(
                                        name = newName.trim(),
                                        matchPattern = newPattern.trim().ifEmpty { "*" },
                                        code = newCode
                                    )
                                    userScripts = listOf(added) + userScripts
                                    prefs.saveUserScripts(userScripts)
                                    newName = ""
                                    newPattern = "*"
                                    newCode = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.userscripts_add))
                        }
                        HorizontalDivider()
                        if (userScripts.isEmpty()) {
                            EmptyStateView(Icons.Rounded.Code, stringResource(R.string.userscripts_empty))
                        } else {
                            LazyColumn(modifier = Modifier.height(140.dp)) {
                                items(userScripts, key = { it.id }) { script ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(script.name, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(script.matchPattern, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Switch(
                                            checked = script.enabled,
                                            onCheckedChange = { enabled ->
                                                userScripts = userScripts.map {
                                                    if (it.id == script.id) it.copy(enabled = enabled) else it
                                                }
                                                prefs.saveUserScripts(userScripts)
                                            }
                                        )
                                        IconButton(onClick = {
                                            userScripts = userScripts.filterNot { it.id == script.id }
                                            prefs.saveUserScripts(userScripts)
                                        }) {
                                            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.action_close))
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activePanel = BrowserPanel.SETTINGS }) {
                        Text(stringResource(R.string.action_close))
                    }
                }
            )
        }

        BrowserPanel.GAMEPAD_GUIDE -> {
            val mappings = listOf(
                R.string.gamepad_key_dpad to R.string.gamepad_desc_dpad,
                R.string.gamepad_key_l1r1 to R.string.gamepad_desc_l1r1,
                R.string.gamepad_key_l2r2 to R.string.gamepad_desc_l2r2,
                R.string.gamepad_key_x to R.string.gamepad_desc_x,
                R.string.gamepad_key_y to R.string.gamepad_desc_y,
                R.string.gamepad_key_select to R.string.gamepad_desc_select,
                R.string.gamepad_key_start to R.string.gamepad_desc_start
            )
            AlertDialog(
                onDismissRequest = { activePanel = BrowserPanel.NONE },
                title = { Text(stringResource(R.string.gamepad_guide_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        mappings.forEach { (keyRes, descRes) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusChip(
                                    label = stringResource(keyRes),
                                    accentColor = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(descRes),
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activePanel = BrowserPanel.NONE }) {
                        Text(stringResource(R.string.action_close))
                    }
                }
            )
        }

        BrowserPanel.SETTINGS -> {
            val currentLang = remember(context) { LocaleHelper.currentLanguage(context) }
            val langs = AppLanguage.entries
            val engines = SearchEngine.entries
            AlertDialog(
                onDismissRequest = { activePanel = BrowserPanel.NONE },
                title = { Text(stringResource(R.string.settings_title)) },
                text = {
                    Column(
                        modifier = Modifier
                            .height(380.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Language switcher
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val next = langs[(langs.indexOf(currentLang) + 1) % langs.size]
                                    LocaleHelper.applyLanguage(context, next)
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_language), fontWeight = FontWeight.Medium)
                                Text(stringResource(R.string.settings_language_desc), fontSize = 12.sp, color = Color.Gray)
                            }
                            Text(stringResource(currentLang.labelRes), color = MaterialTheme.colorScheme.primary)
                        }
                        HorizontalDivider()

                        // Search engine
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val next = engines[(engines.indexOf(searchEngine) + 1) % engines.size]
                                    searchEngine = next
                                    prefs.searchEngine = next
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_search_engine), fontWeight = FontWeight.Medium)
                                Text(stringResource(R.string.settings_search_engine_desc), fontSize = 12.sp, color = Color.Gray)
                            }
                            Text(searchEngine.label, color = MaterialTheme.colorScheme.primary)
                        }
                        HorizontalDivider()

                        // Auto dual screen
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_auto_dual_screen), fontWeight = FontWeight.Medium)
                                Text(stringResource(R.string.settings_auto_dual_screen_desc), fontSize = 12.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = autoDualScreen,
                                onCheckedChange = {
                                    autoDualScreen = it
                                    prefs.autoDualScreen = it
                                }
                            )
                        }
                        HorizontalDivider()

                        // JavaScript
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_javascript), fontWeight = FontWeight.Medium)
                                Text(stringResource(R.string.settings_javascript_desc), fontSize = 12.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = jsEnabled,
                                onCheckedChange = {
                                    jsEnabled = it
                                    prefs.jsEnabled = it
                                    presentation?.updateJsEnabled(it)
                                }
                            )
                        }
                        HorizontalDivider()

                        // AdBlock
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_adblock), fontWeight = FontWeight.Medium)
                                Text(
                                    stringResource(R.string.settings_adblock_desc, adBlockedCount),
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            Switch(
                                checked = adBlockEnabled,
                                onCheckedChange = {
                                    adBlockEnabled = it
                                    prefs.adBlockEnabled = it
                                }
                            )
                        }
                        HorizontalDivider()

                        // Gamepad controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_gamepad), fontWeight = FontWeight.Medium)
                                Text(stringResource(R.string.settings_gamepad_desc), fontSize = 12.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = gamepadEnabled,
                                onCheckedChange = {
                                    gamepadEnabled = it
                                    prefs.gamepadEnabled = it
                                }
                            )
                        }
                        HorizontalDivider()

                        // Force Dark Web Mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_force_dark_web), fontWeight = FontWeight.Medium)
                                Text(stringResource(R.string.settings_force_dark_web_desc), fontSize = 12.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = forceDarkWeb,
                                onCheckedChange = {
                                    forceDarkWeb = it
                                    prefs.forceDarkWeb = it
                                    webViews[activeTabId]?.reload()
                                    presentation?.reloadPage()
                                }
                            )
                        }
                        HorizontalDivider()

                        // Enable Copy & Text Selection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_enable_copy), fontWeight = FontWeight.Medium)
                                Text(stringResource(R.string.settings_enable_copy_desc), fontSize = 12.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = enableCopyScript,
                                onCheckedChange = {
                                    enableCopyScript = it
                                    prefs.enableCopyScript = it
                                }
                            )
                        }
                        HorizontalDivider()

                        // Custom UserScripts manager
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { activePanel = BrowserPanel.USERSCRIPTS }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.settings_userscripts), fontWeight = FontWeight.Medium)
                            }
                            Text("${userScripts.size}", color = MaterialTheme.colorScheme.primary)
                        }
                        HorizontalDivider()

                        Text(
                            text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activePanel = BrowserPanel.NONE }) {
                        Text(stringResource(R.string.action_close))
                    }
                }
            )
        }
    }
}

private fun findSecondaryDisplay(dm: DisplayManager): Display? =
    dm.displays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
