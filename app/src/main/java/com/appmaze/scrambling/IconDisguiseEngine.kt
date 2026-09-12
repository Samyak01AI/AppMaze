package com.appmaze.scrambling

import com.appmaze.apps.AppInfo
import com.appmaze.launcher.LauncherAppItem

/**
 * Computes icon disguise mappings: for each scrambled distracting app,
 * picks a low-usage "donor" app whose icon will be displayed instead.
 *
 * Design principles:
 *  - Donors must be non-distracting, low-usage, and not essential
 *  - Each donor is used at most once per round
 *  - If no eligible donors remain, that app simply keeps its real icon
 *  - Mappings are ephemeral (in-memory only, recomputed per scramble)
 *
 * All methods are pure functions with no side effects or Android dependencies.
 */
class IconDisguiseEngine {

    /**
     * Computes a disguise mapping for the given set of scrambled packages.
     *
     * @param scrambledPackages packages that were just position-scrambled
     * @param allApps           the full list of launcher items (with usage data)
     * @param usageMap          packageName → daily usage minutes
     * @param currentDisguises  existing disguise map (realPkg → donorPkg) — used to
     *                          keep stable assignments for packages NOT in [scrambledPackages]
     * @return a new map of realPkg → donorPkg
     */
    fun computeDisguises(
        scrambledPackages: Set<String>,
        allApps: List<LauncherAppItem>,
        usageMap: Map<String, Long>,
        currentDisguises: Map<String, String> = emptyMap()
    ): Map<String, String> {

        if (scrambledPackages.isEmpty() || allApps.isEmpty()) return currentDisguises

        // Collect all currently used donors (for packages NOT being re-assigned)
        val retainedDisguises = currentDisguises.filter { it.key !in scrambledPackages }
        val usedDonors = retainedDisguises.values.toMutableSet()

        // Build the eligible donor pool: non-distracting, non-essential, sorted by usage ASC
        val donorPool = allApps
            .filter { item ->
                !item.isDistracting &&
                !item.isEssential &&
                item.packageName !in AppInfo.ESSENTIAL_PACKAGES &&
                item.packageName !in scrambledPackages
            }
            .sortedBy { usageMap[it.packageName] ?: 0L }
            .map { it.packageName }
            .toMutableList()

        // Remove already-used donors
        donorPool.removeAll(usedDonors)

        val result = retainedDisguises.toMutableMap()

        for (pkg in scrambledPackages) {
            // Only disguise apps that are actually distracting
            val appItem = allApps.find { it.packageName == pkg }
            if (appItem == null || !appItem.isDistracting) continue

            // Pick the least-used available donor
            val donor = donorPool.firstOrNull { it != pkg }
            if (donor != null) {
                result[pkg] = donor
                donorPool.remove(donor)
                usedDonors.add(donor)
            }
            // If no donor available, skip — app keeps real icon (graceful degradation)
        }

        return result
    }

    /**
     * Removes disguises for packages that are no longer distracting.
     */
    fun pruneDisguises(
        currentDisguises: Map<String, String>,
        distractingPackages: Set<String>
    ): Map<String, String> {
        return currentDisguises.filter { it.key in distractingPackages }
    }
}
