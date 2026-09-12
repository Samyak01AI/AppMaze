package com.appmaze.apps

import android.content.Context
import android.content.pm.PackageManager
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Thread-safe LRU cache for app icons.
 * Icons are loaded asynchronously on IO dispatcher and cached as ImageBitmap.
 * Avoids repeated Drawable → Bitmap → ImageBitmap conversions that cause
 * frame drops during scrolling.
 */
class IconCache(context: Context) {

    private val packageManager: PackageManager = context.packageManager

    // Size cache to ~1/8 of available heap — each 96×96 ARGB bitmap ≈ 36 KB
    private val maxCacheSize = (Runtime.getRuntime().maxMemory() / 8 / (96 * 96 * 4)).toInt()
        .coerceIn(50, 500)

    private val cache = LruCache<String, ImageBitmap>(maxCacheSize)

    // Per-package mutex to avoid duplicate concurrent loads
    private val loadMutexes = mutableMapOf<String, Mutex>()
    private val mapLock = Mutex()

    /**
     * Returns the cached icon or loads it asynchronously.
     * Safe to call from any coroutine context — heavy work runs on IO.
     */
    suspend fun getIcon(packageName: String): ImageBitmap? {
        // Fast path: check cache without lock
        cache.get(packageName)?.let { return it }

        // Get or create a per-package mutex to avoid duplicate loads
        val mutex = mapLock.withLock {
            loadMutexes.getOrPut(packageName) { Mutex() }
        }

        return mutex.withLock {
            // Double-check after acquiring lock
            cache.get(packageName)?.let { return it }

            // Load on IO dispatcher
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    val drawable = packageManager.getApplicationIcon(appInfo)
                    drawable.toBitmap(96, 96).asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }

            if (bitmap != null) {
                cache.put(packageName, bitmap)
            }

            // Clean up mutex map periodically
            if (loadMutexes.size > maxCacheSize * 2) {
                mapLock.withLock {
                    loadMutexes.keys.retainAll(cache.snapshot().keys)
                }
            }

            bitmap
        }
    }

    /**
     * Pre-warms the cache for a list of packages. Call on startup.
     */
    suspend fun preWarm(packageNames: List<String>) {
        withContext(Dispatchers.IO) {
            for (pkg in packageNames) {
                if (cache.get(pkg) == null) {
                    try {
                        val appInfo = packageManager.getApplicationInfo(pkg, 0)
                        val drawable = packageManager.getApplicationIcon(appInfo)
                        val bitmap = drawable.toBitmap(96, 96).asImageBitmap()
                        cache.put(pkg, bitmap)
                    } catch (_: Exception) {
                        // Skip unavailable packages
                    }
                }
            }
        }
    }

    /**
     * Removes a single package from the cache.
     */
    fun evict(packageName: String) {
        cache.remove(packageName)
    }

    /**
     * Clears the entire cache. Call when apps are installed/uninstalled.
     */
    fun clear() {
        cache.evictAll()
    }
}
