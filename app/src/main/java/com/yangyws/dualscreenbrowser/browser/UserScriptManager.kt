package com.yangyws.dualscreenbrowser.browser

import android.webkit.WebView
import java.util.UUID

data class UserScript(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val matchPattern: String = "*",
    val code: String,
    val enabled: Boolean = true
)

object UserScriptManager {
    private const val FORCE_DARK_SCRIPT = """
        (function() {
            if (document.getElementById('dsb-force-dark-style')) return;
            var style = document.createElement('style');
            style.id = 'dsb-force-dark-style';
            style.textContent = 'html { filter: invert(90%) hue-rotate(180deg) !important; background: #111 !important; } ' +
                'img, video, picture, canvas, iframe, [style*="background-image"] { filter: invert(100%) hue-rotate(180deg) !important; }';
            (document.head || document.documentElement).appendChild(style);
        })();
    """

    private const val ENABLE_COPY_SCRIPT = """
        (function() {
            if (window.__dsbCopyEnabled) return;
            window.__dsbCopyEnabled = true;
            var style = document.createElement('style');
            style.textContent = '* { -webkit-user-select: text !important; user-select: text !important; }';
            (document.head || document.documentElement).appendChild(style);
            ['copy', 'cut', 'contextmenu', 'selectstart'].forEach(function(evt) {
                document.addEventListener(evt, function(e) { e.stopPropagation(); }, true);
            });
        })();
    """

    fun matchesUrl(pattern: String, url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val trimmed = pattern.trim()
        if (trimmed.isEmpty() || trimmed == "*") return true
        return url.contains(trimmed, ignoreCase = true)
    }

    fun injectScripts(
        webView: WebView?,
        url: String?,
        forceDark: Boolean,
        enableCopy: Boolean,
        customScripts: List<UserScript>
    ) {
        if (webView == null || url.isNullOrBlank()) return
        if (forceDark) {
            webView.evaluateJavascript(FORCE_DARK_SCRIPT, null)
        }
        if (enableCopy) {
            webView.evaluateJavascript(ENABLE_COPY_SCRIPT, null)
        }
        customScripts
            .filter { it.enabled && it.code.isNotBlank() && matchesUrl(it.matchPattern, url) }
            .forEach { script ->
                webView.evaluateJavascript(script.code, null)
            }
    }
}
