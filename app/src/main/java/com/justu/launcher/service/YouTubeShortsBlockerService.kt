package com.justu.launcher.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.justu.launcher.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class YouTubeShortsBlockerService : AccessibilityService() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isBlockingEnabled = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            packageNames = arrayOf("com.google.android.youtube")
            notificationTimeout = 100
        }
        serviceInfo = info

        // Observe toggle from settings
        serviceScope.launch {
            settingsRepository.homeSettings.collect { settings ->
                isBlockingEnabled = settings.blockYoutubeShorts
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isBlockingEnabled) return
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName != "com.google.android.youtube") return

        // Check if the Shorts tab/feed is visible by inspecting the node tree
        val rootNode = rootInActiveWindow ?: return
        if (isShortsScreenVisible(rootNode)) {
            // Kick the user back to the home screen
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    private fun isShortsScreenVisible(node: AccessibilityNodeInfo): Boolean {
        // Look for "Shorts" content description or view IDs used by the YouTube Shorts tab/feed
        val shortsIndicators = listOf(
            "shorts",
            "Shorts",
            "reel_watch_fragment",
            "shorts_container",
            "shorts_shelf"
        )

        fun searchNode(n: AccessibilityNodeInfo?): Boolean {
            if (n == null) return false
            val desc = n.contentDescription?.toString()?.lowercase() ?: ""
            val viewId = n.viewIdResourceName?.lowercase() ?: ""
            val text = n.text?.toString()?.lowercase() ?: ""
            if (shortsIndicators.any { indicator ->
                    desc.contains(indicator.lowercase()) ||
                    viewId.contains(indicator.lowercase()) ||
                    text == indicator.lowercase()
                }) {
                return true
            }
            for (i in 0 until n.childCount) {
                if (searchNode(n.getChild(i))) return true
            }
            return false
        }

        return searchNode(node)
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
