package com.justu.launcher.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.justu.launcher.data.model.AppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var cachedApps: List<AppInfo>? = null

    suspend fun getInstalledApps(forceRefresh: Boolean = false): List<AppInfo> = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedApps != null) {
            return@withContext cachedApps!!
        }

        val packageManager = context.packageManager
        
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)
        
        val apps = resolveInfos.mapNotNull { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            val label = resolveInfo.loadLabel(packageManager).toString()
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            
            // Avoid listing our own launcher
            if (packageName == context.packageName) return@mapNotNull null
            
            val isSystemApp = try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }

            AppInfo(
                packageName = packageName,
                label = label,
                launchIntent = launchIntent,
                isSystemApp = isSystemApp
            )
        }.sortedBy { it.label.lowercase() }

        cachedApps = apps
        return@withContext apps
    }
}
