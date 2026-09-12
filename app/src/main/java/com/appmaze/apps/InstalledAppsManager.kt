package com.appmaze.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Discovers and manages installed launchable applications.
 * Icon loading is delegated to [IconCache] for async, cached access.
 */
class InstalledAppsManager(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private var cachedApps: List<AppInfo>? = null
    private var cacheTimestamp: Long = 0L

    companion object {
        /** Don't re-query PackageManager within this window */
        private const val CACHE_VALIDITY_MS = 30_000L
    }

    /**
     * Retrieves all installed launchable applications, excluding Shuffle itself.
     * No icon loading — that's handled by [IconCache].
     */
    suspend fun getInstalledApps(forceRefresh: Boolean = false): List<AppInfo> {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedApps != null && (now - cacheTimestamp) < CACHE_VALIDITY_MS) {
            return cachedApps!!
        }
        return withContext(Dispatchers.IO) {
            val mainIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfoList: List<ResolveInfo> = packageManager.queryIntentActivities(mainIntent, 0)

            val apps = resolveInfoList
                .filter { it.activityInfo.packageName != context.packageName }
                .mapNotNull { resolveInfo ->
                    try {
                        val packageName = resolveInfo.activityInfo.packageName
                        val appName = resolveInfo.loadLabel(packageManager).toString()
                        val isSystemApp = (resolveInfo.activityInfo.applicationInfo.flags and
                                android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                        val isEssential = packageName in AppInfo.ESSENTIAL_PACKAGES

                        AppInfo(
                            packageName = packageName,
                            appName = appName,
                            isSystemApp = isSystemApp,
                            isEssential = isEssential
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                .sortedBy { it.appName.lowercase() }

            cachedApps = apps
            cacheTimestamp = now
            apps
        }
    }

    /**
     * Clears the cache so next call will re-query PackageManager.
     */
    fun invalidateCache() {
        cachedApps = null
        cacheTimestamp = 0L
    }

    /**
     * Gets the launch intent for a specific package.
     */
    fun getLaunchIntent(packageName: String): Intent? {
        return packageManager.getLaunchIntentForPackage(packageName)
    }

    /**
     * Checks if a package is still installed.
     */
    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
