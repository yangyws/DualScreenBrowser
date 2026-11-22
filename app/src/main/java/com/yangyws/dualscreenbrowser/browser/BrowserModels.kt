package com.yangyws.dualscreenbrowser.browser

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.util.UUID

enum class SpanSource {
    TOP,
    BOTTOM
}

enum class SearchEngine(val id: String, val label: String, val searchUrlTemplate: String) {
    GOOGLE("google", "Google", "https://www.google.com/search?q=%s"),
    DUCKDUCKGO("ddg", "DuckDuckGo", "https://duckduckgo.com/?q=%s"),
    BING("bing", "Bing", "https://www.bing.com/search?q=%s");

    fun buildQueryUrl(query: String): String {
        val encoded = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
        return searchUrlTemplate.replace("%s", encoded)
    }

    companion object {
        fun fromId(id: String?): SearchEngine =
            entries.firstOrNull { it.id == id } ?: GOOGLE
    }
}

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val url: String = DEFAULT_HOME_URL,
    val title: String = "",
    val isIncognito: Boolean = false,
    val desktopMode: Boolean = false
) {
    companion object {
        const val DEFAULT_HOME_URL = "https://www.google.com"
    }
}

data class BookmarkItem(
    val title: String,
    val url: String,
    val addedAtMs: Long = System.currentTimeMillis()
)

data class HistoryItem(
    val title: String,
    val url: String,
    val visitedAtMs: Long = System.currentTimeMillis()
)

object UrlResolver {
    fun resolveInput(raw: String, searchEngine: SearchEngine): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return BrowserTab.DEFAULT_HOME_URL
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("about:", ignoreCase = true)
        ) {
            return trimmed
        }
        val looksLikeHost = !trimmed.contains(' ') &&
            (trimmed.contains('.') || trimmed.startsWith("localhost", ignoreCase = true))
        return if (looksLikeHost) {
            "https://$trimmed"
        } else {
            searchEngine.buildQueryUrl(trimmed)
        }
    }

    fun isHttps(url: String): Boolean = url.startsWith("https://", ignoreCase = true)

    fun displayHost(url: String): String {
        if (url.isBlank()) return ""
        val host = runCatching { URI(url).host }.getOrNull()
        return if (!host.isNullOrBlank()) host.removePrefix("www.") else url
    }
}

class BrowserPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("dual_screen_browser_prefs", Context.MODE_PRIVATE)

    var autoDualScreen: Boolean
        get() = prefs.getBoolean("auto_dual_screen", true)
        set(value) = prefs.edit().putBoolean("auto_dual_screen", value).apply()

    var jsEnabled: Boolean
        get() = prefs.getBoolean("js_enabled", true)
        set(value) = prefs.edit().putBoolean("js_enabled", value).apply()

    var adBlockEnabled: Boolean
        get() = prefs.getBoolean("ad_block_enabled", true)
        set(value) = prefs.edit().putBoolean("ad_block_enabled", value).apply()

    var gamepadEnabled: Boolean
        get() = prefs.getBoolean("gamepad_enabled", true)
        set(value) = prefs.edit().putBoolean("gamepad_enabled", value).apply()

    var forceDarkWeb: Boolean
        get() = prefs.getBoolean("force_dark_web", false)
        set(value) = prefs.edit().putBoolean("force_dark_web", value).apply()

    var enableCopyScript: Boolean
        get() = prefs.getBoolean("enable_copy_script", true)
        set(value) = prefs.edit().putBoolean("enable_copy_script", value).apply()

    var showSecondaryBar: Boolean
        get() = prefs.getBoolean("show_secondary_bar", true)
        set(value) = prefs.edit().putBoolean("show_secondary_bar", value).apply()

    var spanSource: SpanSource
        get() = if (prefs.getString("span_source", SpanSource.TOP.name) == SpanSource.BOTTOM.name) {
            SpanSource.BOTTOM
        } else {
            SpanSource.TOP
        }
        set(value) = prefs.edit().putString("span_source", value.name).apply()

    var hingeGapDp: Int
        get() = prefs.getInt("hinge_gap_dp", 12).coerceIn(0, 48)
        set(value) = prefs.edit().putInt("hinge_gap_dp", value.coerceIn(0, 48)).apply()

    var searchEngine: SearchEngine
        get() = SearchEngine.fromId(prefs.getString("search_engine", SearchEngine.GOOGLE.id))
        set(value) = prefs.edit().putString("search_engine", value.id).apply()

    fun loadBookmarks(): List<BookmarkItem> {
        val raw = prefs.getString("bookmarks_v1", "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.lines().mapNotNull { line ->
            val parts = line.split("\t")
            if (parts.size >= 2 && parts[1].isNotBlank()) {
                BookmarkItem(
                    title = parts[0].ifBlank { parts[1] },
                    url = parts[1],
                    addedAtMs = parts.getOrNull(2)?.toLongOrNull() ?: 0L
                )
            } else null
        }
    }

    fun saveBookmarks(items: List<BookmarkItem>) {
        val serialized = items.joinToString("\n") { item ->
            "${item.title.replace("\t", " ").replace("\n", " ")}\t${item.url}\t${item.addedAtMs}"
        }
        prefs.edit().putString("bookmarks_v1", serialized).apply()
    }

    fun loadHistory(): List<HistoryItem> {
        val raw = prefs.getString("history_v1", "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.lines().mapNotNull { line ->
            val parts = line.split("\t")
            if (parts.size >= 2 && parts[1].isNotBlank()) {
                HistoryItem(
                    title = parts[0].ifBlank { parts[1] },
                    url = parts[1],
                    visitedAtMs = parts.getOrNull(2)?.toLongOrNull() ?: 0L
                )
            } else null
        }
    }

    fun saveHistory(items: List<HistoryItem>) {
        val serialized = items.take(100).joinToString("\n") { item ->
            "${item.title.replace("\t", " ").replace("\n", " ")}\t${item.url}\t${item.visitedAtMs}"
        }
        prefs.edit().putString("history_v1", serialized).apply()
    }

    fun loadUserScripts(): List<UserScript> {
        val raw = prefs.getString("user_scripts_v1", "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        UserScript(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            name = obj.optString("name", "Script"),
                            matchPattern = obj.optString("matchPattern", "*"),
                            code = obj.optString("code", ""),
                            enabled = obj.optBoolean("enabled", true)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveUserScripts(scripts: List<UserScript>) {
        val arr = JSONArray()
        scripts.forEach { s ->
            arr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("matchPattern", s.matchPattern)
                    put("code", s.code)
                    put("enabled", s.enabled)
                }
            )
        }
        prefs.edit().putString("user_scripts_v1", arr.toString()).apply()
    }
}
