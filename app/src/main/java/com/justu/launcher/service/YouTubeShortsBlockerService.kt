package com.justu.launcher.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.justu.launcher.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class YouTubeShortsBlockerService : AccessibilityService() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Live-updated from DataStore whenever the user toggles the setting
    @Volatile
    private var blockingEnabled = false

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Configure to only watch YouTube window events
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            packageNames = arrayOf(YOUTUBE_PACKAGE)
            notificationTimeout = 150
        }

        // Observe the toggle in real-time
        serviceScope.launch {
            settingsRepository.homeSettings.collect { settings ->
                blockingEnabled = settings.blockYoutubeShorts
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Do nothing if the toggle is OFF
        if (!blockingEnabled) return
        if (event?.packageName?.toString() != YOUTUBE_PACKAGE) return

        val root = rootInActiveWindow ?: return
        if (isShortsVisible(root)) {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    /**
     * Walks the accessibility node tree looking for any node whose view ID,
     * content description, or text matches known Shorts identifiers.
     *
     * YouTube's Shorts feed uses these IDs/labels across versions:
     *   - reel_watch_fragment  (Shorts fullscreen player)
     *   - shorts_shelf         (Shorts shelf on home feed)
     *   - shorts_container     (wrapper in feed)
     *   - "Shorts"             (tab label shown to user)
     */
    private fun isShortsVisible(node: AccessibilityNodeInfo): Boolean {
        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val desc   = node.contentDescription?.toString()?.lowercase() ?: ""
        val text   = node.text?.toString() ?: ""

        val matchesShorts =
            viewId.contains("reel_watch_fragment") ||
            viewId.contains("shorts_shelf") ||
            viewId.contains("shorts_container") ||
            desc.contains("shorts") ||
            text == "Shorts"  // exact match on the tab label only, avoids false positives

        if (matchesShorts) return true

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (isShortsVisible(child)) return true
        }
        return false
    }

    override fun onInterrupt() { /* required by AccessibilityService */ }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    }
}
