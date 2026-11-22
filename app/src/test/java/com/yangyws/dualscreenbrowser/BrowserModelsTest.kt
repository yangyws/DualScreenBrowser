package com.yangyws.dualscreenbrowser

import com.yangyws.dualscreenbrowser.browser.AdBlocker
import com.yangyws.dualscreenbrowser.browser.SearchEngine
import com.yangyws.dualscreenbrowser.browser.UrlResolver
import com.yangyws.dualscreenbrowser.browser.UserScriptManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserModelsTest {
    @Test
    fun `UrlResolver resolves full urls, domains, and search queries`() {
        assertEquals(
            "https://example.com/path",
            UrlResolver.resolveInput("https://example.com/path", SearchEngine.GOOGLE)
        )
        assertEquals(
            "https://github.com",
            UrlResolver.resolveInput("github.com", SearchEngine.GOOGLE)
        )
        assertEquals(
            "https://www.google.com/search?q=ayn%20thor",
            UrlResolver.resolveInput("ayn thor", SearchEngine.GOOGLE)
        )
    }

    @Test
    fun `AdBlocker matches ad domains and subdomains without blocking normal hosts`() {
        assertTrue(AdBlocker.isAdHost("doubleclick.net"))
        assertTrue(AdBlocker.isAdHost("ad.doubleclick.net"))
        assertTrue(AdBlocker.isAdHost("pagead2.googlesyndication.com"))
        assertFalse(AdBlocker.isAdHost("github.com"))
        assertFalse(AdBlocker.isAdHost("developer.android.com"))
    }

    @Test
    fun `UserScriptManager matches wildcard and substring URL patterns`() {
        assertTrue(UserScriptManager.matchesUrl("*", "https://example.com"))
        assertTrue(UserScriptManager.matchesUrl("github.com", "https://github.com/yangyws"))
        assertFalse(UserScriptManager.matchesUrl("youtube.com", "https://github.com/yangyws"))
    }

    @Test
    fun `UrlResolver extracts displayHost and detects HTTPS`() {
        assertEquals("github.com", UrlResolver.displayHost("https://www.github.com/yangyws?tab=1"))
        assertEquals("example.org", UrlResolver.displayHost("http://example.org/path"))
        assertTrue(UrlResolver.isHttps("https://www.google.com"))
        assertFalse(UrlResolver.isHttps("http://localhost:8080"))
    }
}
