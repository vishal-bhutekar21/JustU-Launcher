package com.justu.launcher.utils

import android.content.Context
import android.content.Intent
import com.justu.launcher.MindfulLaunchActivity

object AppLauncherInterceptor {

    fun launchAppMindfully(
        context: Context,
        intent: Intent,
        packageName: String,
        isFocusMode: Boolean = false,
        isBlocked: Boolean = false,
        favoritePackages: Set<String> = emptySet(),
        exemptPackages: Set<String> = emptySet()
    ) {
        val isFavorite = favoritePackages.contains(packageName)
        val isExempt = exemptPackages.contains(packageName)

        when {
            // Blocked app — always show restriction screen
            isBlocked -> {
                startMindful(context, intent, packageName, isFocusMode = false, isBlocked = true)
            }

            // Focus Mode Logic:
            // If Focus Mode is ON, only Favorite apps can open.
            // All other apps (even if exempt from timer) show the Blocked screen.
            isFocusMode -> {
                if (isFavorite) {
                    // Favorites open in Focus Mode. We check if they need the mindful timer.
                    if (isExempt) context.startActivity(intent)
                    else startMindful(context, intent, packageName, isFocusMode = false, isBlocked = false)
                } else {
                    // Non-favorites are blocked in Focus Mode
                    startMindful(context, intent, packageName, isFocusMode = true, isBlocked = false)
                }
            }

            // Normal Mode:
            // Exempt app — always open directly, skip mindful timer
            isExempt -> {
                context.startActivity(intent)
            }

            // Normal launch — show mindful 5s timer screen
            else -> {
                startMindful(context, intent, packageName, isFocusMode = false, isBlocked = false)
            }
        }
    }

    private fun startMindful(
        context: Context,
        intent: Intent,
        packageName: String,
        isFocusMode: Boolean,
        isBlocked: Boolean
    ) {
        val mindfulIntent = Intent(context, MindfulLaunchActivity::class.java).apply {
            putExtra("EXTRA_TARGET_INTENT", intent)
            putExtra("EXTRA_PACKAGE_NAME", packageName)
            putExtra("EXTRA_IS_FOCUS_MODE", isFocusMode)
            putExtra("EXTRA_IS_BLOCKED", isBlocked)
        }
        context.startActivity(mindfulIntent)
    }
}
