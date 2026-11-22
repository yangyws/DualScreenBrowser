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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
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
import androidx.compose.material.icons.rounded.Lock
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
import androidx.compose.material3.Slider
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.yangyws.dualscreenbrowser.browser.SpanSource
import com.yangyws.dualscreenbrowser.browser.UrlResolver
import com.yangyws.dualscreenbrowser.browser.UserScript
import com.yangyws.dualscreenbrowser.browser.UserScriptManager
import com.yangyws.dualscreenbrowser.system.AppLanguage
import com.yangyws.dualscreenbrowser.system.LocaleHelper
import com.yangyws.dualscreenbrowser.ui.EmptyStateView
import com.yangyws.dualscreenbrowser.ui.SidePanelSheet
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
    var hingeGapDp by remember { mutableIntStateOf(prefs.hingeGapDp) }
    var pendingHingeGapDp by remember { mutableIntStateOf(prefs.hingeGapDp) }
    var bottomBrightness by remember { mutableIntStateOf(prefs.bottomBrightness) }
    var bottomWarmth by remember { mutableIntStateOf(prefs.bottomWarmth) }
    var showSecondaryBar by remember { mutableStateOf(prefs.showSecondaryBar) }
    val lastBottomSyncAtMs = remember { LongArray(1) }
    var clearedHistoryBackup by remember { mutableStateOf<List<HistoryItem>?>(null) }
    val addressFocusRequester = remember { FocusRequester() }
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
    var presentationTrigger by remember { mutableIntStateOf(0) }
    var topWebViewTrigger by remember { mutableIntStateOf(0) }

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
    DisposableEffect(dualScreenActive, secondaryDisplay, jsEnabled, presentationTrigger) {
        val targetDisplay = secondaryDisplay
        if (dualScreenActive && targetDisplay != null) {
            val pres = SecondaryBrowserPresentation(
                outerContext = context.createDisplayContext(targetDisplay),
                display = targetDisplay,
                initialUrl = if (spanMode && spanSource == SpanSource.TOP) activeTab.url else secondaryUrl,
                jsEnabled = jsEnabled,
                searchEngineProvider = { searchEngine },
                adBlockEnabledProvider = { adBlockEnabled },
                userScriptsProvider = { Triple(forceDarkWeb, enableCopyScript, userScripts) },
                barVisibleProvider = { showSecondaryBar && !spanMode },
                spanModeProvider = { spanMode },
                spanOffsetCssProvider = {
                    if (spanMode && dualScreenActive) {
                        val density = context.resources.displayMetrics.density.coerceAtLeast(1f)
                        webViews[activeTabId]?.let { v ->
                            ((v.scrollY + v.height) / density.toDouble()) + hingeGapDp
                        }
                    } else {
                        null
                    }
                },
                onSecondarySpanScrollCss = { bottomCssY ->
                    if (spanMode && dualScreenActive) {
                        webViews[activeTabId]?.let { topWv ->
                            val density = context.resources.displayMetrics.density.coerceAtLeast(1f)
                            val targetTopPx = ((bottomCssY - hingeGapDp) * density - topWv.height).toInt().coerceAtLeast(0)
                            if (kotlin.math.abs(topWv.scrollY - targetTopPx) > 4) {
                                lastBottomSyncAtMs[0] = SystemClock.uptimeMillis()
                                topWv.scrollTo(topWv.scrollX, targetTopPx)
                            }
                        }
                    }
                },
                onSpanBottomUrlChanged = { bottomUrl ->
                    if (spanMode && dualScreenActive && webViews[activeTabId]?.url != bottomUrl) {
                        webViews[activeTabId]?.loadUrl(bottomUrl)
                        addressText = bottomUrl
                    }
                },
                bottomColorProvider = { bottomBrightness to bottomWarmth },
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

    LaunchedEffect(bottomBrightness, bottomWarmth, presentation) {
        presentation?.updateBottomColor(bottomBrightness, bottomWarmth)
    }

    LaunchedEffect(isEditingText) {
        if (isEditingText) {
            runCatching { addressFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(spanMode, dualScreenActive, presentation) {
        if (spanMode && dualScreenActive) {
            val targetUrl = if (spanSource == SpanSource.BOTTOM) {
                secondaryUrl.ifBlank { activeTab.url }
            } else {
                activeTab.url
            }
            if (targetUrl.isNotBlank()) {
                if (webViews[activeTabId]?.url != targetUrl) {
                    webViews[activeTabId]?.loadUrl(targetUrl)
                }
                if (presentation?.currentUrl() != targetUrl) {
                    presentation?.loadUrl(targetUrl)
                }
            }
        }
    }

    // Cleanup WebViews when activity disposes
    DisposableEffect(Unit) {
        onDispose {
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
        webViews[activeTabId]?.loadUrl(target)
        if (spanMode && dualScreenActive) {
            presentation?.loadUrl(target)
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
                if (!spanMode && gamepadTarget == GamepadTargetScreen.BOTTOM && dualScreenActive && presentation != null) {
                    presentation?.scrollByY(dy)
                } else {
                    webViews[activeTabId]?.scrollBy(0, dy)
                }
            }

            override fun goBackActive() {
                if (!spanMode && gamepadTarget == GamepadTargetScreen.BOTTOM && dualScreenActive && presentation != null) {
                    presentation?.goBackIfPossible()
                } else {
                    webViews[activeTabId]?.let { if (it.canGoBack()) it.goBack() }
                }
            }

            override fun goForwardActive() {
                if (!spanMode && gamepadTarget == GamepadTargetScreen.BOTTOM && dualScreenActive && presentation != null) {
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
                if (!spanMode && gamepadTarget == GamepadTargetScreen.BOTTOM && dualScreenActive && presentation != null) {
                    presentation?.reloadPage()
                } else {
                    webViews[activeTabId]?.reload()
                    if (spanMode && dualScreenActive) presentation?.reloadPage()
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

            override fun setSpanSource(source: SpanSource) {
                spanSource = source
                prefs.spanSource = source
                val labelRes = if (source == SpanSource.TOP) {
                    R.string.action_span_source_top
                } else {
                    R.string.action_span_source_bottom
                }
                Toast.makeText(context, context.getString(labelRes), Toast.LENGTH_SHORT).show()
                if (spanMode && dualScreenActive) {
                    val targetUrl = if (source == SpanSource.BOTTOM) secondaryUrl else activeTab.url
                    if (targetUrl.isNotBlank()) {
                        webViews[activeTabId]?.loadUrl(targetUrl)
                        presentation?.loadUrl(targetUrl)
                    }
                }
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
                onClick = { webViews[activeTabId]?.let { if (it.canGoBack()) it.goBack() } },
                enabled = canGoBack
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back), modifier = Modifier.size(20.dp))
            }
            TactileIconButton(
                onClick = { webViews[activeTabId]?.let { if (it.canGoForward()) it.goForward() } },
                enabled = canGoForward
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = stringResource(R.string.action_forward), modifier = Modifier.size(20.dp))
            }
            TactileIconButton(
                onClick = {
                    val wv = webViews[activeTabId]
                    if (loadProgress in 0.01f..0.99f) wv?.stopLoading() else wv?.reload()
                    if (spanMode && dualScreenActive) presentation?.reloadPage()
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (activeTab.isIncognito) {
                        Icon(
                            Icons.Rounded.VisibilityOff,
                            contentDescription = stringResource(R.string.action_incognito),
                            tint = Color(0xFFCE93D8),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                    } else if (!isEditingText && UrlResolver.isHttps(addressText)) {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                    }

                    if (!isEditingText && addressText.isNotBlank()) {
                        Text(
                            text = UrlResolver.displayHost(addressText),
                            color = Color.White,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    isEditingText = true
                                    addressFocusRequester.requestFocus()
                                }
                        )
                    } else {
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
                                .focusRequester(addressFocusRequester)
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
                    onDismissRequest = { overflowMenuExpanded = false },
                    containerColor = Color(0xFF1E222B)
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
                                val next = if (spanSource == SpanSource.TOP) SpanSource.BOTTOM else SpanSource.TOP
                                gamepadActions.setSpanSource(next)
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
                        text = { Text(stringResource(R.string.action_reopen_top)) },
                        leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                        onClick = {
                            overflowMenuExpanded = false
                            webViews.remove(activeTabId)?.destroy()
                            topWebViewTrigger++
                        }
                    )
                    if (secondaryDisplay != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_reopen_bottom)) },
                            leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                            onClick = {
                                overflowMenuExpanded = false
                                dualScreenActive = true
                                presentationTrigger++
                            }
                        )
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
        ) {
            androidx.compose.runtime.key(topWebViewTrigger) {
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
                        container.setOnScrollChangeListener { v, _, scrollY, _, _ ->
                            if (spanMode && dualScreenActive &&
                                SystemClock.uptimeMillis() - lastBottomSyncAtMs[0] > 250L
                            ) {
                                val density = context.resources.displayMetrics.density.coerceAtLeast(1f)
                                presentation?.syncSpanScrollCssY(((scrollY + v.height) / density.toDouble()) + hingeGapDp)
                            }
                        }
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
                                    if (spanMode && dualScreenActive) {
                                        if (presentation?.currentUrl() != url) {
                                            presentation?.loadUrl(url)
                                        }
                                        val density = context.resources.displayMetrics.density.coerceAtLeast(1f)
                                        view?.let { v ->
                                            presentation?.syncSpanScrollCssY(((v.scrollY + v.height) / density.toDouble()) + hingeGapDp)
                                        }
                                    }
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
            }
        }
    }

    // Docked Right Side Panel Sheet
    val panelTitle = when (activePanel) {
        BrowserPanel.NONE -> ""
        BrowserPanel.TABS -> stringResource(R.string.action_tabs)
        BrowserPanel.BOOKMARKS -> stringResource(R.string.action_bookmarks)
        BrowserPanel.HISTORY -> stringResource(R.string.action_history)
        BrowserPanel.USERSCRIPTS -> stringResource(R.string.userscripts_title)
        BrowserPanel.GAMEPAD_GUIDE -> stringResource(R.string.gamepad_guide_title)
        BrowserPanel.SETTINGS -> stringResource(R.string.settings_title)
    }

    SidePanelSheet(
        visible = activePanel != BrowserPanel.NONE,
        title = panelTitle,
        onDismiss = { activePanel = BrowserPanel.NONE },
        dimBackground = activePanel != BrowserPanel.SETTINGS,
        compactWidth = activePanel == BrowserPanel.SETTINGS,
        headerActions = {
            if (activePanel == BrowserPanel.HISTORY && history.isNotEmpty()) {
                TextButton(onClick = {
                    clearedHistoryBackup = history
                    history = emptyList()
                    prefs.saveHistory(emptyList())
                }) {
                    Text(stringResource(R.string.action_clear_history), fontSize = 12.sp)
                }
            }
        }
    ) {
        when (activePanel) {
            BrowserPanel.NONE -> Unit

            BrowserPanel.TABS -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { openTab(incognito = false) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.action_new_tab), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(onClick = { openTab(incognito = true) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.action_incognito), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(10.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    gridItems(tabs, key = { it.id }) { tab ->
                        val isCurrent = tab.id == activeTab.id
                        Surface(
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.08f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    activeTabId = tab.id
                                    addressText = tab.url
                                    activePanel = BrowserPanel.NONE
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (tab.isIncognito) {
                                        StatusChip(
                                            label = stringResource(R.string.action_incognito),
                                            accentColor = Color(0xFFCE93D8)
                                        )
                                    } else {
                                        Text(
                                            text = UrlResolver.displayHost(tab.url).ifBlank { "New" },
                                            fontSize = 11.sp,
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.LightGray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    TactileIconButton(onClick = { closeTab(tab.id) }, size = 24.dp) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = stringResource(R.string.action_close),
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = tab.title.ifBlank { stringResource(R.string.untitled_page) },
                                    fontSize = 13.sp,
                                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                    color = Color.White,
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
                        }
                    }
                }
            }

            BrowserPanel.BOOKMARKS -> {
                if (bookmarks.isEmpty()) {
                    EmptyStateView(Icons.Rounded.BookmarkBorder, stringResource(R.string.no_bookmarks))
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
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
                                    Text(item.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(item.url, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                TactileIconButton(
                                    onClick = {
                                        bookmarks = bookmarks.filterNot { it.url == item.url }
                                        prefs.saveBookmarks(bookmarks)
                                    },
                                    size = 32.dp
                                ) {
                                    Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.action_remove_bookmark), tint = Color.LightGray, modifier = Modifier.size(18.dp))
                                }
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                        }
                    }
                }
            }

            BrowserPanel.HISTORY -> {
                val backup = clearedHistoryBackup
                if (history.isEmpty() && backup != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.history_cleared_prompt), color = Color.White, fontSize = 13.sp)
                            TextButton(onClick = {
                                history = backup
                                prefs.saveHistory(backup)
                                clearedHistoryBackup = null
                            }) {
                                Text(stringResource(R.string.action_undo), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                if (history.isEmpty()) {
                    EmptyStateView(Icons.Rounded.History, stringResource(R.string.no_history))
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
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
                                Text(item.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.url, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                        }
                    }
                }
            }

            BrowserPanel.USERSCRIPTS -> {
                var newName by remember { mutableStateOf("") }
                var newPattern by remember { mutableStateOf("*") }
                var newCode by remember { mutableStateOf("") }
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
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
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    if (userScripts.isEmpty()) {
                        EmptyStateView(Icons.Rounded.Code, stringResource(R.string.userscripts_empty))
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(userScripts, key = { it.id }) { script ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(script.name, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                                    TactileIconButton(
                                        onClick = {
                                            userScripts = userScripts.filterNot { it.id == script.id }
                                            prefs.saveUserScripts(userScripts)
                                        },
                                        size = 32.dp
                                    ) {
                                        Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.action_close), tint = Color.LightGray, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            BrowserPanel.GAMEPAD_GUIDE -> {
                val mappings = listOf(
                    R.string.gamepad_key_dpad to R.string.gamepad_desc_dpad,
                    R.string.gamepad_key_l1r1 to R.string.gamepad_desc_l1r1,
                    R.string.gamepad_key_l2r2 to R.string.gamepad_desc_l2r2,
                    R.string.gamepad_key_x to R.string.gamepad_desc_x,
                    R.string.gamepad_key_y to R.string.gamepad_desc_y,
                    R.string.gamepad_key_select to R.string.gamepad_desc_select,
                    R.string.gamepad_key_start to R.string.gamepad_desc_start,
                    R.string.gamepad_key_start_dpad to R.string.gamepad_desc_start_dpad
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
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
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            BrowserPanel.SETTINGS -> {
                val currentLang = remember(context) { LocaleHelper.currentLanguage(context) }
                val langs = AppLanguage.entries
                val engines = SearchEngine.entries
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                            Text(stringResource(R.string.settings_language), fontWeight = FontWeight.Medium, color = Color.White)
                            Text(stringResource(R.string.settings_language_desc), fontSize = 12.sp, color = Color.Gray)
                        }
                        Text(stringResource(currentLang.labelRes), color = MaterialTheme.colorScheme.primary)
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

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
                            Text(stringResource(R.string.settings_search_engine), fontWeight = FontWeight.Medium, color = Color.White)
                            Text(stringResource(R.string.settings_search_engine_desc), fontSize = 12.sp, color = Color.Gray)
                        }
                        Text(searchEngine.label, color = MaterialTheme.colorScheme.primary)
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    // Hinge gap compensation slider
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.settings_hinge_gap), fontWeight = FontWeight.Medium, color = Color.White)
                        Text(
                            stringResource(R.string.settings_hinge_gap_desc, pendingHingeGapDp),
                            fontSize = 12.sp,
                            color = Color.Gray,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                        Slider(
                            value = pendingHingeGapDp.toFloat(),
                            onValueChange = { pendingHingeGapDp = it.toInt() },
                            onValueChangeFinished = {
                                hingeGapDp = pendingHingeGapDp
                                prefs.hingeGapDp = pendingHingeGapDp
                            },
                            valueRange = 0f..48f
                        )
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    // Bottom screen brightness & warmth calibration
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_bottom_brightness), fontWeight = FontWeight.Medium, color = Color.White)
                                Text(
                                    stringResource(R.string.settings_bottom_brightness_desc, bottomBrightness),
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    style = TextStyle(fontFeatureSettings = "tnum")
                                )
                            }
                            if (bottomBrightness != 0 || bottomWarmth != 0) {
                                TextButton(onClick = {
                                    bottomBrightness = 0
                                    bottomWarmth = 0
                                    prefs.bottomBrightness = 0
                                    prefs.bottomWarmth = 0
                                }) {
                                    Text(stringResource(R.string.action_reset), fontSize = 12.sp)
                                }
                            }
                        }
                        Slider(
                            value = bottomBrightness.toFloat(),
                            onValueChange = {
                                val v = it.toInt()
                                bottomBrightness = v
                                prefs.bottomBrightness = v
                            },
                            valueRange = -30f..30f
                        )
                        Text(stringResource(R.string.settings_bottom_warmth), fontWeight = FontWeight.Medium, color = Color.White)
                        Text(
                            stringResource(R.string.settings_bottom_warmth_desc, bottomWarmth),
                            fontSize = 12.sp,
                            color = Color.Gray,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                        Slider(
                            value = bottomWarmth.toFloat(),
                            onValueChange = {
                                val v = it.toInt()
                                bottomWarmth = v
                                prefs.bottomWarmth = v
                            },
                            valueRange = -30f..30f
                        )
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_javascript), fontWeight = FontWeight.Medium, color = Color.White)
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
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_adblock), fontWeight = FontWeight.Medium, color = Color.White)
                            Text(
                                stringResource(R.string.settings_adblock_desc, adBlockedCount),
                                fontSize = 12.sp,
                                color = Color.Gray,
                                style = TextStyle(fontFeatureSettings = "tnum")
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
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_gamepad), fontWeight = FontWeight.Medium, color = Color.White)
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
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_force_dark_web), fontWeight = FontWeight.Medium, color = Color.White)
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
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_enable_copy), fontWeight = FontWeight.Medium, color = Color.White)
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
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activePanel = BrowserPanel.USERSCRIPTS }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Code, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_userscripts), fontWeight = FontWeight.Medium, color = Color.White)
                        }
                        Text("${userScripts.size}", color = MaterialTheme.colorScheme.primary, style = TextStyle(fontFeatureSettings = "tnum"))
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    Text(
                        text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

private fun findSecondaryDisplay(dm: DisplayManager): Display? =
    dm.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION).firstOrNull()
        ?: dm.displays.firstOrNull {
            it.displayId != Display.DEFAULT_DISPLAY && (it.flags and Display.FLAG_PRIVATE) == 0
        }
