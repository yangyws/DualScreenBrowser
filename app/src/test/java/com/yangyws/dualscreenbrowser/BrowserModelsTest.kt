package com.yangyws.dualscreenbrowser

import com.yangyws.dualscreenbrowser.browser.SearchEngine
import com.yangyws.dualscreenbrowser.browser.UrlResolver
import org.junit.Assert.assertEquals
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
}
