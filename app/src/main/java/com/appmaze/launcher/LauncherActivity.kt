package com.appmaze.launcher

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.appmaze.ui.screens.*
import com.appmaze.ui.theme.ShuffleTheme
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

class LauncherActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestMaxRefreshRate()
        Purchases.logLevel = LogLevel.DEBUG
        Purchases.configure(
            PurchasesConfiguration.Builder(this, "test_XcCgXTVNNyDIlNNyxrcDEmKInwx")
            .build())
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            ShuffleTheme(themeMode = uiState.themeMode) {
                val navController = rememberNavController()

                // Determine start destination
                val startDestination = if (uiState.onboardingComplete) "launcher" else "onboarding"

                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(180)
                        ) + fadeIn(animationSpec = tween(140))
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { -it / 4 },
                            animationSpec = tween(180)
                        ) + fadeOut(animationSpec = tween(140))
                    },
                    popEnterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { -it / 4 },
                            animationSpec = tween(180)
                        ) + fadeIn(animationSpec = tween(140))
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(180)
                        ) + fadeOut(animationSpec = tween(140))
                    }
                ) {
                    composable("onboarding") {
                        OnboardingScreen(
                            uiState = uiState,
                            onToggleDistracting = { viewModel.toggleDistracting(it) },
                            onIntensitySelected = { viewModel.setScramblingIntensity(it) },
                            onComplete = {
                                viewModel.setOnboardingComplete()
                                navController.navigate("launcher") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            },
                            onRequestUsagePermission = {
                                startActivity(viewModel.getUsageSettingsIntent())
                            }
                        )
                    }

                    composable("launcher") {
                        LauncherScreen(
                            uiState = uiState,
                            onAppClick = { viewModel.launchApp(it) },
                            onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                            onSearchActiveChanged = { viewModel.setSearchActive(it) },
                            onNavigateToDashboard = {
                                navController.navigate("dashboard")
                            },
                            onNavigateToSettings = {
                                navController.navigate("settings")
                            },
                            onNavigateToFocus = {
                                navController.navigate("focus")
                            },
                            onToggleDistracting = { viewModel.toggleDistracting(it) },
                            formatScreenTime = { viewModel.formatScreenTime(it) }
                        )

                        // Intervention dialog
                        uiState.showInterventionFor?.let { app ->
                            InterventionDialog(
                                app = app,
                                formatScreenTime = { viewModel.formatScreenTime(it) },
                                onConfirmOpen = { viewModel.confirmLaunch(app.packageName) },
                                onDismiss = { viewModel.dismissIntervention() }
                            )
                        }
                    }

                    composable("focus") {
                        FocusScreen(
                            uiState = uiState,
                            onNavigateBack = { navController.popBackStack() },
                            onStartSession = { viewModel.startFocusSession(it) },
                            onEndSession = { viewModel.endFocusSession() },
                            onDismissCompletion = { viewModel.dismissFocusCompletion() },
                            formatScreenTime = { viewModel.formatScreenTime(it) }
                        )
                    }

                    composable("dashboard") {
                        DashboardScreen(
                            uiState = uiState,
                            formatScreenTime = { viewModel.formatScreenTime(it) },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            uiState = uiState,
                            onNavigateBack = { navController.popBackStack() },
                            onScramblingEnabledChanged = { viewModel.setScramblingEnabled(it) },
                            onScramblingIntensityChanged = { viewModel.setScramblingIntensity(it) },
                            onThemeModeChanged = { viewModel.setThemeMode(it) },
                            onGridColumnsChanged = { viewModel.setGridColumns(it) },
                            onShowLabelsChanged = { viewModel.setShowLabels(it) },
                            onInterventionEnabledChanged = { viewModel.setInterventionEnabled(it) },
                            onScramblingThresholdChanged = { viewModel.setScramblingThreshold(it) },
                            onScramblingFrequencyChanged = { viewModel.setScramblingFrequency(it) },
                            onDemoModeChanged = { viewModel.setDemoMode(it) },
                            onFrictionLevelChanged = { viewModel.setFrictionLevel(it) },
                            onScrambleStrategyChanged = { viewModel.setScrambleStrategy(it) },
                            onAntiRoutineChanged = { viewModel.setAntiRoutineEnabled(it) },
                            onIconDisguiseChanged = { viewModel.setIconDisguiseEnabled(it) },
                            onResetPositions = { viewModel.resetAppPositions() },
                            onClearStatistics = { viewModel.clearAllStatistics() },
                            onScrambleNow = { viewModel.scrambleNow(force = true) },
                            onRequestUsagePermission = {
                                startActivity(viewModel.getUsageSettingsIntent())
                            },
                            onNavigateToManageApps = {
                                navController.navigate("manage_apps")
                            }
                        )
                    }

                    composable("manage_apps") {
                        ManageAppsScreen(
                            uiState = uiState,
                            onNavigateBack = { navController.popBackStack() },
                            onToggleDistracting = { viewModel.toggleDistracting(it) }
                        )
                    }

                    composable("demo") {
                        DemoScreen(
                            uiState = uiState,
                            onNavigateBack = { navController.popBackStack() },
                            onAddUsage = { pkg, mins -> viewModel.demoAddUsage(pkg, mins) },
                            onToggleDistracting = { viewModel.toggleDistracting(it) },
                            onScrambleNow = { viewModel.scrambleNow(force = true) },
                            onResetLayout = { viewModel.demoResetLayout() },
                            formatScreenTime = { viewModel.formatScreenTime(it) }
                        )
                    }
                }
            }
        }
    }

    private var maxRefreshRateConfigured = false

    /**
     * Request the maximum available refresh rate for the display (e.g. 90Hz/120Hz/144Hz).
     *
     * IMPORTANT: We do NOT set `preferredDisplayModeId`!
     * Setting `preferredDisplayModeId` forces a physical display mode change that overrides
     * `preferredRefreshRate` and causes adaptive refresh rate engines (Samsung One UI, Pixel LTPO)
     * to drop back to 60Hz or drop frames during mode renegotiation.
     */
    private fun requestMaxRefreshRate() {
        if (maxRefreshRateConfigured) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val display = display ?: return
                val currentMode = display.mode
                // Match current physical dimensions to prevent display mode switch failure
                val matchingModes = display.supportedModes.filter {
                    it.physicalWidth == currentMode.physicalWidth &&
                    it.physicalHeight == currentMode.physicalHeight
                }
                val maxRefreshRate = (matchingModes.maxOfOrNull { it.refreshRate }
                    ?: display.supportedModes.maxOfOrNull { it.refreshRate }
                    ?: 120f).coerceAtLeast(60f)

                val params = window.attributes
                params.preferredRefreshRate = maxRefreshRate
                window.attributes = params
                maxRefreshRateConfigured = true
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                val display = windowManager.defaultDisplay
                val maxRefreshRate = (display.supportedModes.maxOfOrNull { it.refreshRate } ?: 120f).coerceAtLeast(60f)
                val params = window.attributes
                params.preferredRefreshRate = maxRefreshRate
                window.attributes = params
                maxRefreshRateConfigured = true
            }
        } catch (e: Exception) {
            // Graceful fallback if OEM ROM throws display service exception
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure max refresh rate is applied
        requestMaxRefreshRate()
        // Permission state is cheap to refresh here. Avoid a full PackageManager/UsageStats/DB
        // reload on every resume; that causes visible frame drops when returning to the launcher.
        viewModel.checkUsagePermission()
    }

    // Handle back press on the launcher - don't exit, just go home
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // On the launcher home screen, back should do nothing
        // (same behavior as stock launchers)
    }
}
