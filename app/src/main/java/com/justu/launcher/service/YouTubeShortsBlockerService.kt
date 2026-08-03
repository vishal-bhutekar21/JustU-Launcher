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
 * Accessibility Service that detects YouTube Shorts in all forms:
 *   1. Full-screen vertical Shorts feed
 *   2. Shorts mini-player (picture-in-picture / bottom sheet)
 *   3. Shorts shelf on the YouTube home feed
 *
 * When detected and the toggle is ON:
 *   → Launches ShortsBlockedActivity (full-screen warning)
 *   → User can then choose to go to YouTube Home or exit YouTube entirely
 *
 * The service reads the toggle state live from DataStore so changes take
 * effect immediately without a restart.
 */
@AndroidEntryPoint
class YouTubeShortsBlockerService : AccessibilityService() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var blockingEnabled = false

    // Debounce: don't re-trigger the warning screen if we already fired it recently
    @Volatile
    private var lastBlockedTimeMs = 0L
    private val debounceMs = 2000L

    override fun onServiceConnected() {
        super.onServiceConnected()

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes =
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            // FLAG_REPORT_VIEW_IDS: needed to read viewIdResourceName
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            packageNames = arrayOf(YOUTUBE_PACKAGE)
            notificationTimeout = 200
        }

        serviceScope.launch {
            settingsRepository.homeSettings.collect { settings ->
                blockingEnabled = settings.blockYoutubeShorts
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!blockingEnabled) return
        if (event?.packageName?.toString() != YOUTUBE_PACKAGE) return

        // Only act on meaningful window/content transitions
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return

        val root = rootInActiveWindow ?: return

        val shortsType = detectShortsType(root)
        if (shortsType != ShortsType.NONE) {
            val now = System.currentTimeMillis()
            if (now - lastBlockedTimeMs < debounceMs) return  // already handling it
            lastBlockedTimeMs = now

            when (shortsType) {
                ShortsType.FULLSCREEN, ShortsType.SHELF -> launchBlockedScreen()
                ShortsType.MINIPLAYER -> {
                    // For mini-player: close it first, then launch warning
                    collapseMiniPlayer(root)
                    launchBlockedScreen()
                }
                ShortsType.NONE -> Unit
            }
        }
    }

    /**
     * Checks the view tree for Shorts indicators.
     * Returns the most severe ShortsType found.
     *
     * Known view IDs across YouTube versions:
     *  - reel_watch_fragment      → full-screen Shorts player
     *  - shorts_shelf_item_title  → Shorts shelf on home feed
     *  - reel_recycler_view       → Shorts vertical scroll feed
     *  - mini_player_layout       → Shorts in mini-player
     *  - inline_shorts            → Shorts embedded in feed
     */
    private fun detectShortsType(root: AccessibilityNodeInfo): ShortsType {
        return scanNode(root)
    }

    private fun scanNode(node: AccessibilityNodeInfo?): ShortsType {
        if (node == null) return ShortsType.NONE

        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        val text = node.text?.toString() ?: ""

        // Full-screen Shorts player (highest priority)
        if (viewId.contains("reel_watch_fragment") ||
            viewId.contains("reel_recycler_view") ||
            viewId.contains("shorts_player_view")) {
            return ShortsType.FULLSCREEN
        }

        // Shorts mini-player / picture-in-picture
        if (viewId.contains("mini_player") && (
            contentDesc.contains("shorts") ||
            viewId.contains("shorts"))
        ) {
            return ShortsType.MINIPLAYER
        }

        // Shorts tab label (exact match to avoid catching "How to get more Shorts views")
        if (text == "Shorts" && node.isClickable) {
            // Only flag if this tab appears to be selected (focused/checked)
            if (node.isFocused || node.isSelected || node.isChecked) {
                return ShortsType.FULLSCREEN
            }
        }

        // Shorts shelf on home feed
        if (viewId.contains("shorts_shelf") ||
            viewId.contains("inline_shorts") ||
            viewId.contains("shorts_shelf_item")) {
            return ShortsType.SHELF
        }

        // Recurse into children
        var found = ShortsType.NONE
        for (i in 0 until node.childCount) {
            val childResult = scanNode(node.getChild(i))
            if (childResult.priority > found.priority) {
                found = childResult
            }
            if (found == ShortsType.FULLSCREEN) break  // can't do better
        }
        return found
    }

    /**
     * Attempts to close/collapse the Shorts mini-player by finding its
     * close button and performing a click action.
     */
    private fun collapseMiniPlayer(root: AccessibilityNodeInfo) {
        fun findAndClick(node: AccessibilityNodeInfo?): Boolean {
            if (node == null) return false
            val viewId = node.viewIdResourceName?.lowercase() ?: ""
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            if ((viewId.contains("close") || desc.contains("close") || desc.contains("dismiss"))
                && viewId.contains("mini")) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
            for (i in 0 until node.childCount) {
                if (findAndClick(node.getChild(i))) return true
            }
            return false
        }
        findAndClick(root)
    }

    /**
     * Launches the full-screen warning screen.
     * Uses FLAG_ACTIVITY_NEW_TASK since we're calling from a Service.
     */
    private fun launchBlockedScreen() {
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

    companion object {
        private const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    }

    /**
     * Priority order: FULLSCREEN > MINIPLAYER > SHELF > NONE
     * Used to return the most severe detection from the tree scan.
     */
    private enum class ShortsType(val priority: Int) {
        NONE(0),
        SHELF(1),
        MINIPLAYER(2),
        FULLSCREEN(3)
    }
}
