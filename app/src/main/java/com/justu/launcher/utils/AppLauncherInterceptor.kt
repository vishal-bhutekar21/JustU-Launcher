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
        favoritePackages: Set<String> = emptySet()
    ) {
        val isFavorite = favoritePackages.contains(packageName)

        when {
            // Blocked app — always show restriction screen
            isBlocked -> {
                val mindfulIntent = Intent(context, Class.forName("com.justu.launcher.MindfulLaunchActivity")).apply {
                    putExtra("EXTRA_TARGET_INTENT", intent)
                    putExtra("EXTRA_PACKAGE_NAME", packageName)
                    putExtra("EXTRA_IS_FOCUS_MODE", false)
                    putExtra("EXTRA_IS_BLOCKED", true)
                }
                context.startActivity(mindfulIntent)
            }

            // Focus Mode ON and app is NOT a favorite — block it
            isFocusMode && !isFavorite -> {
                val mindfulIntent = Intent(context, Class.forName("com.justu.launcher.MindfulLaunchActivity")).apply {
                    putExtra("EXTRA_TARGET_INTENT", intent)
                    putExtra("EXTRA_PACKAGE_NAME", packageName)
                    putExtra("EXTRA_IS_FOCUS_MODE", true)
                    putExtra("EXTRA_IS_BLOCKED", false)
                }
                context.startActivity(mindfulIntent)
            }

            // Focus Mode ON and app IS a favorite — launch directly, no mindful screen
            isFocusMode && isFavorite -> {
                context.startActivity(intent)
            }

            // Normal launch — show mindful thought screen
            else -> {
                val mindfulIntent = Intent(context, Class.forName("com.justu.launcher.MindfulLaunchActivity")).apply {
                    putExtra("EXTRA_TARGET_INTENT", intent)
                    putExtra("EXTRA_PACKAGE_NAME", packageName)
                    putExtra("EXTRA_IS_FOCUS_MODE", false)
                    putExtra("EXTRA_IS_BLOCKED", false)
                }
                context.startActivity(mindfulIntent)
            }
        }
    }
}
