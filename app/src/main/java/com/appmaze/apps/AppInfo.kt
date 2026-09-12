package com.appmaze.apps

/**
 * Represents an installed application on the device.
 * Icons are loaded separately via [IconCache] to avoid blocking app discovery.
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean = false,
    val isEssential: Boolean = false
) {
    companion object {
        /** Package names that are considered essential and should not be scrambled by default. */
        val ESSENTIAL_PACKAGES = setOf(
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.android.contacts",
            "com.google.android.contacts",
            "com.android.settings",
            "com.android.emergency",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.mms",
            "com.google.android.gm", // Gmail
        )
    }
}
