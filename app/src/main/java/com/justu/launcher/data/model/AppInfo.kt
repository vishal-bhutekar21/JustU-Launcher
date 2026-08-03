package com.justu.launcher.data.model

import android.content.Intent

data class AppInfo(
    val packageName: String,
    val label: String,
    val launchIntent: Intent? = null,
    val isSystemApp: Boolean = false
)
