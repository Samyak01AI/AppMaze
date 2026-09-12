package com.appmaze.usage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Repository for querying screen-time data from UsageStatsManager.
 */
class UsageStatsRepository(private val context: Context) {

    private val usageStatsManager: UsageStatsManager? =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    /**
     * Checks whether Usage Access permission is granted.
     */
    fun hasUsagePermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Returns an intent to open Usage Access settings.
     */
    fun getUsageSettingsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }

    /**
     * Gets usage stats for today.
     * Returns a map of packageName -> usageTimeInMinutes.
     */
    suspend fun getTodayUsage(): Map<String, Long> = withContext(Dispatchers.IO) {
        if (!hasUsagePermission() || usageStatsManager == null) return@withContext emptyMap()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            calendar.timeInMillis,
            System.currentTimeMillis()
        )

        stats?.filter { it.totalTimeInForeground > 0 }
            ?.associate { it.packageName to (it.totalTimeInForeground / 60000L) }
            ?: emptyMap()
    }

    /**
     * Gets usage stats for a given number of past days.
     * Returns a map of date-string -> map of packageName -> usageMinutes.
     */
    suspend fun getUsageForDays(days: Int): Map<String, Map<String, Long>> = withContext(Dispatchers.IO) {
        if (!hasUsagePermission() || usageStatsManager == null) return@withContext emptyMap()

        val result = mutableMapOf<String, Map<String, Long>>()
        val calendar = Calendar.getInstance()

        for (i in 0 until days) {
            val endCal = calendar.clone() as Calendar
            endCal.set(Calendar.HOUR_OF_DAY, 23)
            endCal.set(Calendar.MINUTE, 59)
            endCal.set(Calendar.SECOND, 59)

            val startCal = calendar.clone() as Calendar
            startCal.set(Calendar.HOUR_OF_DAY, 0)
            startCal.set(Calendar.MINUTE, 0)
            startCal.set(Calendar.SECOND, 0)

            val dateStr = String.format(
                "%04d-%02d-%02d",
                startCal.get(Calendar.YEAR),
                startCal.get(Calendar.MONTH) + 1,
                startCal.get(Calendar.DAY_OF_MONTH)
            )

            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startCal.timeInMillis,
                endCal.timeInMillis
            )

            val dayUsage = stats?.filter { it.totalTimeInForeground > 0 }
                ?.associate { it.packageName to (it.totalTimeInForeground / 60000L) }
                ?: emptyMap()

            result[dateStr] = dayUsage
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        result
    }

    /**
     * Gets the total screen time today in minutes.
     */
    suspend fun getTotalScreenTimeToday(): Long {
        return getTodayUsage().values.sum()
    }

    /**
     * Gets the top N most used apps today by usage time in minutes.
     */
    suspend fun getTopAppsToday(n: Int = 5): List<Pair<String, Long>> {
        return getTodayUsage().entries
            .sortedByDescending { it.value }
            .take(n)
            .map { it.key to it.value }
    }
}
