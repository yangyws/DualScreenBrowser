package com.yangyws.dualscreenbrowser.browser

import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicInteger

object AdBlocker {
    private val blockedCounter = AtomicInteger(0)

    private val defaultBlockedDomains = setOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "google-analytics.com",
        "googletagmanager.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        "ads.yahoo.com",
        "adnxs.com",
        "criteo.com",
        "taboola.com",
        "outbrain.com",
        "pubmatic.com",
        "rubiconproject.com",
        "openx.net",
        "scorecardresearch.com",
        "quantserve.com",
        "moatads.com",
        "amazon-adsystem.com",
        "ads-twitter.com",
        "app-measurement.com",
        "connect.facebook.net",
        "pixel.facebook.com",
        "ads.tiktok.com",
        "analytics.tiktok.com",
        "popads.net",
        "propellerads.com",
        "adsterra.com"
    )

    fun isAdHost(host: String?, customBlockedHosts: Set<String> = emptySet()): Boolean {
        if (host.isNullOrBlank()) return false
        val normalized = host.lowercase().trim()
        return matchesDomainSet(normalized, defaultBlockedDomains) ||
            matchesDomainSet(normalized, customBlockedHosts)
    }

    private fun matchesDomainSet(host: String, domains: Set<String>): Boolean {
        for (domain in domains) {
            if (host == domain || host.endsWith(".$domain")) {
                return true
            }
        }
        return false
    }

    fun shouldBlockUrl(url: String?, customBlockedHosts: Set<String> = emptySet()): Boolean {
        if (url.isNullOrBlank()) return false
        val host = runCatching { Uri.parse(url).host }.getOrNull() ?: return false
        return isAdHost(host, customBlockedHosts)
    }

    fun recordBlocked(): Int = blockedCounter.incrementAndGet()

    fun blockedCount(): Int = blockedCounter.get()

    fun resetBlockedCount() {
        blockedCounter.set(0)
    }

    fun emptyResponse(): WebResourceResponse =
        WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
}
