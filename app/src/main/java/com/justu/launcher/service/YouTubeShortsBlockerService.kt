package com.justu.launcher.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.justu.launcher.ShortsBlockedActivity
import com.justu.launcher.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Detects YouTube Shorts in ALL contexts:
 *
 *  1. YouTube App — via Activity class name (most reliable, obfuscation-proof)
 *     YouTube's Shorts player Activity always contains "Reel" or "ShortsPlayer" in its class name.
 *
 *  2. YouTube App — via URL bar text if class name doesn't match
 *     Some YouTube builds show a /shorts/ URL in an address-bar-style node.
 *
 *  3. Browsers (Chrome, Firefox, Samsung Internet, Edge, Opera, Brave, etc.)
 *     Scans the address bar node for "/shorts/" in the URL text.
 *
 *  4. Mini-player / picture-in-picture fallback
 *     Scans visible text on screen for Shorts-specific patterns.
 */
@AndroidEntryPoint
class YouTubeShortsBlockerService : AccessibilityService() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var blockingEnabled = false
    @Volatile private var lastBlockedTimeMs = 0L
    private val debounceDurationMs = 2500L

    // Packages we want to fully intercept (URL scanning + class-name scanning)
    private val youtubePkg = "com.google.android.youtube"
    private val youtubeKidsPkg = "com.google.android.apps.youtube.kids"
    private val browserPackages = setOf(
        "com.android.chrome",
        "org.mozilla.firefox",
        "org.mozilla.fenix",
        "com.microsoft.emmx",           // Edge
        "com.opera.browser",
        "com.opera.mini.native",
        "com.brave.browser",
        "com.sec.android.app.sbrowser", // Samsung Internet
        "com.UCMobile.intl",
        "com.duckduckgo.mobile.android",
        "com.kiwibrowser.browser",
        "com.vivaldi.browser"
    )
    private val watchedPackages = setOf(youtubePkg, youtubeKidsPkg) + browserPackages

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes =
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_SCROLLED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
            // No packageNames filter — we handle all packages ourselves so we can also catch browsers
        }

        serviceScope.launch {
            settingsRepository.homeSettings.collect { settings ->
                blockingEnabled = settings.blockYoutubeShorts
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!blockingEnabled) return

        val pkg = event?.packageName?.toString() ?: return
        if (!watchedPackages.contains(pkg)) return

        // Debounce: don't show the screen repeatedly within 2.5 seconds
        val now = System.currentTimeMillis()
        if (now - lastBlockedTimeMs < debounceDurationMs) return

        val isYouTubeApp = pkg == youtubePkg || pkg == youtubeKidsPkg

        if (isYouTubeApp) {
            handleYouTubeApp(event)
        } else {
            handleBrowser(event)
        }
    }

    // ─── YouTube App Detection ────────────────────────────────────────────────

    private fun handleYouTubeApp(event: AccessibilityEvent) {
        // STRATEGY 1: Activity/Window class name
        // YouTube Shorts opens in a specific Activity. The class name contains "Reel"
        // or "ShortsPlayer" regardless of obfuscation level. This is the most reliable signal.
        val className = event.className?.toString() ?: ""
        if (isShortsClassName(className)) {
            triggerBlock()
            return
        }

        // STRATEGY 2: Scan the accessibility node tree
        // Fallback if class name doesn't help (some YouTube variants).
        val root = rootInActiveWindow ?: return
        if (containsShortsInTree(root, isYouTubeApp = true)) {
            triggerBlock()
        }
    }

    /**
     * Class names that confirm YouTube Shorts is active.
     * YouTube may obfuscate class names but the Shorts host activity
     * consistently includes one of these fragments.
     */
    private fun isShortsClassName(className: String): Boolean {
        val lower = className.lowercase()
        return lower.contains("reelwatchfragment") ||
               lower.contains("reel_watch") ||
               lower.contains("shortsplayer") ||
               lower.contains("shortswatchfragment") ||
               lower.contains("reelsfragment") ||
               lower.contains("reelwatchplayerfragment")
    }

    // ─── Browser Detection ─────────────────────────────────────────────────────

    private fun handleBrowser(event: AccessibilityEvent) {
        // Only check on window change events for browsers — content changes are too noisy
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val root = rootInActiveWindow ?: return
        if (containsShortsInTree(root, isYouTubeApp = false)) {
            triggerBlock()
        }
    }

    // ─── Tree Scanner ─────────────────────────────────────────────────────────

    /**
     * Walks the accessibility node tree looking for Shorts indicators.
     *
     * For YouTube app: looks for known Shorts view IDs and selected Shorts tab.
     * For browsers: looks for the URL address bar containing "/shorts/".
     */
    private fun containsShortsInTree(node: AccessibilityNodeInfo?, isYouTubeApp: Boolean): Boolean {
        if (node == null) return false

        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val text = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""

        if (isYouTubeApp) {
            // View ID signals (YouTube app internal IDs)
            if (viewId.contains("reel_watch_fragment") ||
                viewId.contains("reel_recycler") ||
                viewId.contains("shorts_player") ||
                viewId.contains("reel_player")) {
                return true
            }

            // The "Shorts" tab: only count it if it's the SELECTED/FOCUSED tab
            if (text.equals("Shorts", ignoreCase = false) &&
                (node.isSelected || node.isChecked || node.isFocused)) {
                return true
            }

            // URL text inside YouTube that contains /shorts/
            if (text.contains("/shorts/", ignoreCase = true)) {
                return true
            }
        } else {
            // Browser: find the URL bar and check its text
            val isUrlBar = viewId.contains("url_bar") ||
                           viewId.contains("location_bar") ||
                           viewId.contains("address_bar") ||
                           viewId.contains("url_field") ||
                           viewId.contains("omnibox") ||
                           viewId.contains("search_field") ||
                           contentDesc.contains("address bar") ||
                           contentDesc.contains("url") ||
                           contentDesc.contains("search")

            if (isUrlBar && text.contains("/shorts/", ignoreCase = true)) {
                return true
            }

            // Fallback: any text node on screen that's clearly a /shorts/ URL
            if (text.matches(Regex(".*youtube\\.com/shorts/[A-Za-z0-9_\\-]+.*"))) {
                return true
            }
        }

        // Recurse — limit depth to avoid performance issues
        val maxDepth = 8
        return recurse(node, isYouTubeApp, depth = 0, maxDepth = maxDepth)
    }

    private fun recurse(
        node: AccessibilityNodeInfo,
        isYouTubeApp: Boolean,
        depth: Int,
        maxDepth: Int
    ): Boolean {
        if (depth >= maxDepth) return false
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (containsShortsInTree(child, isYouTubeApp)) return true
        }
        return false
    }

    // ─── Block Action ─────────────────────────────────────────────────────────

    private fun triggerBlock() {
        lastBlockedTimeMs = System.currentTimeMillis()
        val intent = Intent(this, ShortsBlockedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    override fun onInterrupt() { /* required */ }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
