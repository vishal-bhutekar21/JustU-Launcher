package com.justu.launcher.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun getTodaysUsageTime(): Long = withContext(Dispatchers.IO) {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis
        
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        
        if (stats.isNullOrEmpty()) {
            return@withContext 0L
        }
        
        var totalUsage = 0L
        for (stat in stats) {
            if (stat.totalTimeInForeground > 0 && stat.packageName != context.packageName) {
                totalUsage += stat.totalTimeInForeground
            }
        }
        
        totalUsage
    }

    suspend fun getTopUsedApps(): List<Pair<String, Long>> = withContext(Dispatchers.IO) {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm = context.packageManager
        
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis
        
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        
        if (stats.isNullOrEmpty()) return@withContext emptyList()
        
        val appUsageMap = mutableMapOf<String, Long>()
        
        for (stat in stats) {
            if (stat.totalTimeInForeground > 0 && stat.packageName != context.packageName) {
                appUsageMap[stat.packageName] = appUsageMap.getOrDefault(stat.packageName, 0L) + stat.totalTimeInForeground
            }
        }
        
        appUsageMap.entries
            .sortedByDescending { it.value }
            .take(5)
            .mapNotNull { entry ->
                try {
                    val label = pm.getApplicationInfo(entry.key, 0).loadLabel(pm).toString()
                    Pair(label, entry.value)
                } catch (e: Exception) {
                    null
                }
            }
    }
}
