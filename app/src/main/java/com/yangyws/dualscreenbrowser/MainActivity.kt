package com.yangyws.dualscreenbrowser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.yangyws.dualscreenbrowser.browser.BookmarkItem
import com.yangyws.dualscreenbrowser.browser.BrowserPrefs
import com.yangyws.dualscreenbrowser.browser.BrowserTab
import com.yangyws.dualscreenbrowser.browser.HistoryItem
import com.yangyws.dualscreenbrowser.browser.SearchEngine
import com.yangyws.dualscreenbrowser.browser.SecondaryBrowserPresentation
import com.yangyws.dualscreenbrowser.browser.UrlResolver
import com.yangyws.dualscreenbrowser.system.AppLanguage
import com.yangyws.dualscreenbrowser.system.LocaleHelper

private enum class BrowserPanel {
    NONE,
    TABS,
    BOOKMARKS,
    HISTORY,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    private var launchUrlState by mutableStateOf<String?>(null)

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
                    onConsumeExternalUrl = { launchUrlState = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.dataString?.let { launchUrlState = it }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun DualScreenBrowserApp(
    externalUrl: String?,
    onConsumeExternalUrl: () -> Unit
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
        mutableStateOf(autoDualScreen && secondaryDisplay != null)
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
                if (autoDualScreen && secondaryDisplay != null) {
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
                searchEngineProvider = { searchEngine },
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { webViews[activeTabId]?.let { if (it.canGoBack()) it.goBack() } },
                enabled = canGoBack,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            IconButton(
                onClick = { webViews[activeTabId]?.let { if (it.canGoForward()) it.goForward() } },
                enabled = canGoForward,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = stringResource(R.string.action_forward))
            }
            IconButton(
                onClick = {
                    val wv = webViews[activeTabId]
                    if (loadProgress in 0.01f..0.99f) wv?.stopLoading() else wv?.reload()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (loadProgress in 0.01f..0.99f) Icons.Rounded.Close else Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.action_refresh)
                )
            }

            // Address input box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (activeTab.isIncognito) Color(0xFF2D223B) else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (activeTab.isIncognito) {
                        Icon(
                            Icons.Rounded.VisibilityOff,
                            contentDescription = stringResource(R.string.action_incognito),
                            tint = Color(0xFFCE93D8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    BasicTextField(
                        value = addressText,
                        onValueChange = { addressText = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = { navigateActiveTab(addressText) }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (addressText.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.url_placeholder),
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                            inner()
                        }
                    )
                }
            }

            IconButton(
                onClick = {
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
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = stringResource(R.string.action_add_bookmark),
                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else Color.White
                )
            }

            IconButton(
                onClick = {
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
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Computer,
                    contentDescription = stringResource(R.string.action_desktop_mode),
                    tint = if (activeTab.desktopMode) MaterialTheme.colorScheme.primary else Color.White
                )
            }
        }

        if (loadProgress in 0.01f..0.99f) {
            LinearProgressIndicator(
                progress = { loadProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            )
        }

        // Main WebView container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
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

        // Bottom Toolbar (Tabs, Bookmarks, History, Dual-Screen Controls, Settings)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { activePanel = BrowserPanel.TABS }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Tab, contentDescription = stringResource(R.string.action_tabs))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.action_tabs_count, tabs.size),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            IconButton(onClick = { activePanel = BrowserPanel.BOOKMARKS }) {
                Icon(Icons.Rounded.Bookmark, contentDescription = stringResource(R.string.action_bookmarks))
            }
            IconButton(onClick = { activePanel = BrowserPanel.HISTORY }) {
                Icon(Icons.Rounded.History, contentDescription = stringResource(R.string.action_history))
            }
            IconButton(
                onClick = {
                    if (secondaryDisplay == null) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.secondary_screen_not_found),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        dualScreenActive = !dualScreenActive
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Devices,
                    contentDescription = stringResource(
                        if (dualScreenActive) R.string.action_dual_screen_on else R.string.action_dual_screen_off
                    ),
                    tint = if (dualScreenActive) MaterialTheme.colorScheme.primary else Color.White
                )
            }
            if (dualScreenActive && secondaryDisplay != null) {
                IconButton(
                    onClick = {
                        secondaryUrl = activeTab.url
                        presentation?.loadUrl(activeTab.url)
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_sent_to_bottom),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenInBrowser,
                        contentDescription = stringResource(R.string.action_send_to_secondary)
                    )
                }
                IconButton(
                    onClick = {
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
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SwapVert,
                        contentDescription = stringResource(R.string.action_swap_screens)
                    )
                }
            }
            IconButton(onClick = { activePanel = BrowserPanel.SETTINGS }) {
                Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.action_settings))
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
                        Text(stringResource(R.string.no_bookmarks), color = Color.Gray)
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
                        Text(stringResource(R.string.no_history), color = Color.Gray)
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

        BrowserPanel.SETTINGS -> {
            val currentLang = remember(context) { LocaleHelper.currentLanguage(context) }
            val langs = AppLanguage.entries
            val engines = SearchEngine.entries
            AlertDialog(
                onDismissRequest = { activePanel = BrowserPanel.NONE },
                title = { Text(stringResource(R.string.settings_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
