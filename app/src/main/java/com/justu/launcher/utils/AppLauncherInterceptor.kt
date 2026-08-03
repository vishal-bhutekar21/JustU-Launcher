package com.justu.launcher.utils

import android.content.Context
import android.content.Intent

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
            // Blocked app — always show restriction screen regardless of exemption
            isBlocked -> {
                startMindful(context, intent, packageName, isFocusMode = false, isBlocked = true)
            }

            // Exempt app — always open directly, skip mindful timer
            isExempt -> {
                context.startActivity(intent)
            }

            // Focus Mode ON and app is NOT a favorite — show mindful screen
            isFocusMode && !isFavorite -> {
                startMindful(context, intent, packageName, isFocusMode = true, isBlocked = false)
            }

            // Focus Mode ON and app IS a favorite — open directly
            isFocusMode && isFavorite -> {
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
        val mindfulIntent = Intent(context, Class.forName("com.justu.launcher.MindfulLaunchActivity")).apply {
            putExtra("EXTRA_TARGET_INTENT", intent)
            putExtra("EXTRA_PACKAGE_NAME", packageName)
            putExtra("EXTRA_IS_FOCUS_MODE", isFocusMode)
            putExtra("EXTRA_IS_BLOCKED", isBlocked)
        }
        context.startActivity(mindfulIntent)
    }
}
