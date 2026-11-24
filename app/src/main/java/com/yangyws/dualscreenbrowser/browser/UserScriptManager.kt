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

    private const val VIEWPORT_UNIT_FIX_SCRIPT = """
        (function() {
            if (window.__dsbVhFixInstalled) return;
            window.__dsbVhFixInstalled = true;
            function probeZero(unit) {
                var root = document.body || document.documentElement;
                if (!root) return false;
                var d = document.createElement('div');
                d.style.cssText = 'position:fixed;height:100' + unit + ';visibility:hidden;pointer-events:none';
                root.appendChild(d);
                var h = d.getBoundingClientRect().height;
                d.remove();
                return h === 0 && window.innerHeight > 0;
            }
            function fixEl(el) {
                if (!el || !el.getAttribute) return;
                var s = el.getAttribute('style');
                if (!s || s.indexOf('vh') === -1) return;
                var ih = window.innerHeight;
                if (ih <= 0) return;
                var fixed = s.replace(/([0-9.]+)(d|s|l)?vh/gi, function(_, n) {
                    return (parseFloat(n) * ih / 100) + 'px';
                });
                if (fixed !== s) el.setAttribute('style', fixed);
            }
            function install() {
                if (!probeZero('dvh') && !probeZero('vh')) return;
                document.querySelectorAll('[style*="vh"]').forEach(fixEl);
                var root = document.body || document.documentElement;
                if (root && !window.__dsbVhObs) {
                    window.__dsbVhObs = new MutationObserver(function(muts) {
                        for (var i = 0; i < muts.length; i++) {
                            var m = muts[i];
                            if (m.type === 'attributes' && m.target) fixEl(m.target);
                            if (m.addedNodes) {
                                for (var j = 0; j < m.addedNodes.length; j++) {
                                    var n = m.addedNodes[j];
                                    if (n.nodeType === 1) {
                                        fixEl(n);
                                        if (n.querySelectorAll) n.querySelectorAll('[style*="vh"]').forEach(fixEl);
                                    }
                                }
                            }
                        }
                    });
                    window.__dsbVhObs.observe(root, {
                        attributes: true, attributeFilter: ['style'], childList: true, subtree: true
                    });
                }
            }
            install();
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
        webView.evaluateJavascript(VIEWPORT_UNIT_FIX_SCRIPT, null)
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
