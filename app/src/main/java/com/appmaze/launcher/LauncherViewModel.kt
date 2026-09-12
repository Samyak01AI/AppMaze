package com.appmaze.launcher

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.appmaze.ShuffleApplication
import com.appmaze.apps.AppInfo
import com.appmaze.apps.IconCache
import com.appmaze.apps.InstalledAppsManager
import com.appmaze.data.*
import com.appmaze.focus.FocusManager
import com.appmaze.focus.FocusState
import com.appmaze.scrambling.IconDisguiseEngine
import com.appmaze.scrambling.PatternDetector
import com.appmaze.scrambling.ScrambleStrategy
import com.appmaze.scrambling.ScramblingEngine
import com.appmaze.usage.DistractionLevel
import com.appmaze.usage.UsageAnalyzer
import com.appmaze.usage.UsageStatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Represents an app in the launcher grid with its display info and position.
 */
@Immutable
data class LauncherAppItem(
    val packageName: String,
    val appName: String,
    val icon: ImageBitmap?,
    val position: Int,
    val isDistracting: Boolean = false,
    val distractionScore: Int = 0,
    val distractionLevel: DistractionLevel = DistractionLevel.LOW,
    val dailyUsageMinutes: Long = 0,
    val launchCountToday: Int = 0,
    val isEssential: Boolean = false,
    val disguisePackageName: String? = null,
    val disguiseIcon: ImageBitmap? = null
)

/**
 * UI state for the launcher screen.
 */
@Immutable
data class LauncherUiState(
    val apps: List<LauncherAppItem> = emptyList(),
    val searchQuery: String = "",
    val filteredApps: List<LauncherAppItem> = emptyList(),
    val isSearchActive: Boolean = false,
    val totalScreenTimeMinutes: Long = 0,
    val gridColumns: Int = 4,
    val showLabels: Boolean = true,
    val themeMode: String = "system",
    val isLoading: Boolean = true,
    val onboardingComplete: Boolean = false,
    val hasUsagePermission: Boolean = false,
    val scramblingEnabled: Boolean = true,
    val scramblingIntensity: String = "moderate",
    val interventionEnabled: Boolean = true,
    val demoModeEnabled: Boolean = false,
    val showInterventionFor: LauncherAppItem? = null,
    val interventionLaunchCount: Int = 0,
    // Dashboard data
    val topApps: List<Pair<String, Long>> = emptyList(),
    val distractingApps: List<LauncherAppItem> = emptyList(),
    val scrambleCountToday: Int = 0,
    // Demo mode
    val demoUsageOverrides: Map<String, Long> = emptyMap(),
    // New features
    val frictionLevel: String = "balanced",
    val scrambleStrategy: String = "adaptive",
    val antiRoutineEnabled: Boolean = true,
    val scramblingThreshold: Int = 45,
    val scramblingFrequency: Int = 6,
    // Icon Disguise
    val iconDisguiseEnabled: Boolean = true,
    val activeDisguises: Map<String, String> = emptyMap(),
    // Focus Mode
    val focusState: FocusState = FocusState.Inactive,
    val focusModeDuration: Int = 25,
    val todayFocusMinutes: Long = 0,
    val focusStreak: Int = 0,
    // Statistics
    val interventionCountToday: Int = 0,
    val breakCountToday: Int = 0,
    val mostDistractingApp: LauncherAppItem? = null
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ShuffleApplication
    private val installedAppsManager = InstalledAppsManager(application)
    val iconCache = IconCache(application)
    private val usageStatsRepository = UsageStatsRepository(application)
    private val usageAnalyzer = UsageAnalyzer()
    private val scramblingEngine = ScramblingEngine()
    private val iconDisguiseEngine = IconDisguiseEngine()
    private val patternDetector = PatternDetector()
    private val positionDao = app.database.appPositionDao()
    private val dailyUsageDao = app.database.dailyUsageDao()
    private val scrambleEventDao = app.database.scrambleEventDao()
    private val focusSessionDao = app.database.focusSessionDao()
    private val interventionEventDao = app.database.interventionEventDao()
    private val appLaunchEventDao = app.database.appLaunchEventDao()
    private val settingsRepository = app.settingsRepository

    val focusManager = FocusManager(focusSessionDao, viewModelScope)

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    private var lastFullRefreshTimestamp = 0L

    init {
        // Observe settings changes
        viewModelScope.launch {
            combine(
                settingsRepository.onboardingComplete,
                settingsRepository.gridColumns,
                settingsRepository.showAppLabels,
                settingsRepository.themeMode,
                settingsRepository.scramblingEnabled
            ) { onboarding, cols, labels, theme, scrambling ->
                _uiState.update {
                    it.copy(
                        onboardingComplete = onboarding,
                        gridColumns = cols,
                        showLabels = labels,
                        themeMode = theme,
                        scramblingEnabled = scrambling
                    )
                }
            }.collect()
        }

        viewModelScope.launch {
            combine(
                settingsRepository.scramblingIntensity,
                settingsRepository.interventionEnabled,
                settingsRepository.demoModeEnabled,
                settingsRepository.frictionLevel,
                settingsRepository.scrambleStrategy
            ) { intensity, intervention, demo, friction, strategy ->
                _uiState.update {
                    it.copy(
                        scramblingIntensity = intensity,
                        interventionEnabled = intervention,
                        demoModeEnabled = demo,
                        frictionLevel = friction,
                        scrambleStrategy = strategy
                    )
                }
            }.collect()
        }

        viewModelScope.launch {
            combine(
                settingsRepository.scramblingThresholdMinutes,
                settingsRepository.scramblingFrequencyHours,
                settingsRepository.antiRoutineEnabled,
                settingsRepository.focusModeDurationMinutes,
                settingsRepository.iconDisguiseEnabled
            ) { threshold, frequency, antiRoutine, focusDuration, disguiseEnabled ->
                _uiState.update {
                    it.copy(
                        scramblingThreshold = threshold,
                        scramblingFrequency = frequency,
                        antiRoutineEnabled = antiRoutine,
                        focusModeDuration = focusDuration,
                        iconDisguiseEnabled = disguiseEnabled
                    )
                }
                // Clear disguises when feature is toggled off
                if (!disguiseEnabled) {
                    _uiState.update { it.copy(activeDisguises = emptyMap()) }
                }
            }.collect()
        }

        // Observe focus state
        viewModelScope.launch {
            focusManager.state.collect { state ->
                _uiState.update { it.copy(focusState = state) }
            }
        }

        loadApps()
    }

    /**
     * Loads installed apps, merges with stored positions, and updates usage data.
     * Debounced to avoid expensive full refreshes within 10 seconds.
     */
    fun loadApps(performAutomaticScramble: Boolean = true) {
        val now = System.currentTimeMillis()
        if (now - lastFullRefreshTimestamp < 10_000 && !_uiState.value.isLoading) {
            return // Debounce
        }
        lastFullRefreshTimestamp = now

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val hasPermission = usageStatsRepository.hasUsagePermission()
            _uiState.update { it.copy(hasUsagePermission = hasPermission) }

            // Get installed apps (no icons — those come from IconCache)
            val installed = installedAppsManager.getInstalledApps(forceRefresh = false)
            val installedPackages = installed.map { it.packageName }

            // Clean up uninstalled apps from DB
            withContext(Dispatchers.IO) {
                positionDao.removeUninstalledApps(installedPackages)
            }

            // Pre-warm icon cache in background
            viewModelScope.launch(Dispatchers.IO) {
                iconCache.preWarm(installedPackages)
            }

            // Get stored positions
            val storedPositions = withContext(Dispatchers.IO) { positionDao.getAllPositionsList() }
            val storedMap = storedPositions.associateBy { it.packageName }

            // Get usage data
            val todayUsage = if (hasPermission) usageStatsRepository.getTodayUsage() else emptyMap()
            val totalScreenTime = todayUsage.values.sum()

            // Get today's start timestamp
            val todayStart = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis

            // Get launch counts for today
            val launchEvents = withContext(Dispatchers.IO) {
                appLaunchEventDao.getEventsSince(todayStart)
            }
            val launchCounts = launchEvents.groupBy { it.packageName }.mapValues { it.value.size }

            // Merge installed apps with stored positions
            val appItems = mutableListOf<LauncherAppItem>()
            var nextPosition = (storedPositions.maxOfOrNull { it.position } ?: -1) + 1

            // Load icons from cache
            val iconMap = mutableMapOf<String, ImageBitmap?>()
            withContext(Dispatchers.IO) {
                for (appInfo in installed) {
                    iconMap[appInfo.packageName] = iconCache.getIcon(appInfo.packageName)
                }
            }

            val newEntities = mutableListOf<AppPositionEntity>()

            for (appInfo in installed) {
                val stored = storedMap[appInfo.packageName]
                val usage = _uiState.value.demoUsageOverrides[appInfo.packageName]
                    ?: todayUsage[appInfo.packageName]
                    ?: 0L
                val launches = launchCounts[appInfo.packageName] ?: 0
                val score = usageAnalyzer.calculateDistractionScore(usage, launches)
                val level = DistractionLevel.fromScore(score)

                val position = stored?.position ?: nextPosition++
                val isDistracting = stored?.isDistracting ?: false

                // Collect new entities for batch insert
                if (stored == null) {
                    newEntities.add(
                        AppPositionEntity(
                            packageName = appInfo.packageName,
                            position = position
                        )
                    )
                }

                appItems.add(
                    LauncherAppItem(
                        packageName = appInfo.packageName,
                        appName = appInfo.appName,
                        icon = iconMap[appInfo.packageName],
                        position = position,
                        isDistracting = isDistracting,
                        distractionScore = score,
                        distractionLevel = level,
                        dailyUsageMinutes = usage,
                        launchCountToday = launches,
                        isEssential = appInfo.isEssential
                    )
                )
            }

            // Batch insert new entities
            if (newEntities.isNotEmpty()) {
                withContext(Dispatchers.IO) { positionDao.upsertAll(newEntities) }
            }

            val sorted = appItems.sortedBy { it.position }

            // Apply icon disguises if enabled
            val disguises = _uiState.value.activeDisguises
            val disguisedSorted = if (_uiState.value.iconDisguiseEnabled && disguises.isNotEmpty()) {
                // Pre-load disguise donor icons
                val disguiseIconMap = mutableMapOf<String, ImageBitmap?>()
                withContext(Dispatchers.IO) {
                    for ((_, donorPkg) in disguises) {
                        if (donorPkg !in disguiseIconMap) {
                            disguiseIconMap[donorPkg] = iconCache.getIcon(donorPkg)
                        }
                    }
                }
                sorted.map { item ->
                    val donorPkg = disguises[item.packageName]
                    if (donorPkg != null) {
                        item.copy(
                            disguisePackageName = donorPkg,
                            disguiseIcon = disguiseIconMap[donorPkg]
                        )
                    } else item
                }
            } else sorted

            // Get top apps and dashboard data
            val topApps = todayUsage.entries
                .sortedByDescending { it.value }
                .take(5)
                .map { entry ->
                    val name = installed.find { it.packageName == entry.key }?.appName ?: entry.key
                    name to entry.value
                }

            val distracting = sorted.filter { it.isDistracting }

            // Get scramble count today
            val scrambleCount = withContext(Dispatchers.IO) {
                scrambleEventDao.countEventsSince(todayStart)
            }

            // Get stats
            val interventionCount = withContext(Dispatchers.IO) {
                interventionEventDao.countSince(todayStart)
            }
            val breakCount = withContext(Dispatchers.IO) {
                interventionEventDao.countBreaksSince(todayStart)
            }
            val focusMinutes = focusManager.getTodayFocusMinutes()
            val focusStreak = focusManager.getFocusStreak()

            // Most distracting app
            val mostDistracting = sorted
                .filter { it.isDistracting }
                .maxByOrNull { it.distractionScore }

            _uiState.update {
                it.copy(
                    apps = disguisedSorted,
                    filteredApps = if (it.isSearchActive) filterApps(disguisedSorted, it.searchQuery) else disguisedSorted,
                    totalScreenTimeMinutes = totalScreenTime,
                    topApps = topApps,
                    distractingApps = distracting,
                    scrambleCountToday = scrambleCount,
                    interventionCountToday = interventionCount,
                    breakCountToday = breakCount,
                    todayFocusMinutes = focusMinutes,
                    focusStreak = focusStreak,
                    mostDistractingApp = mostDistracting,
                    isLoading = false
                )
            }

            // Auto-classify distracting apps if scrambling enabled and usage permission is granted
            if (hasPermission && _uiState.value.scramblingEnabled) {
                autoClassifyDistractingApps(sorted, todayUsage)
                if (performAutomaticScramble) {
                    scrambleNow(force = false)
                }
            }
        }
    }

    /**
     * Auto-classifies apps as distracting based on usage threshold.
     */
    private suspend fun autoClassifyDistractingApps(
        apps: List<LauncherAppItem>,
        usageMap: Map<String, Long>
    ) {
        val threshold = _uiState.value.scramblingThreshold
        val entitiesToUpdate = mutableListOf<Pair<String, Boolean>>()

        for (app in apps) {
            val usage = usageMap[app.packageName] ?: 0L
            val shouldBeDistracting = usageAnalyzer.shouldAutoClassifyAsDistracting(
                dailyUsageMinutes = usage,
                thresholdMinutes = threshold,
                isEssential = app.isEssential
            )
            val entity = withContext(Dispatchers.IO) { positionDao.getPosition(app.packageName) } ?: continue
            if (shouldBeDistracting && !entity.isDistracting) {
                entitiesToUpdate.add(app.packageName to true)
            }
        }

        // Batch update
        if (entitiesToUpdate.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                for ((pkg, distracting) in entitiesToUpdate) {
                    positionDao.setDistracting(pkg, distracting, false)
                }
            }
        }
    }

    // ─── Search ──────────────────────────────────────────

    fun onSearchQueryChanged(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                filteredApps = filterApps(it.apps, query)
            )
        }
    }

    fun setSearchActive(active: Boolean) {
        _uiState.update {
            it.copy(
                isSearchActive = active,
                searchQuery = if (!active) "" else it.searchQuery,
                filteredApps = if (!active) it.apps else it.filteredApps
            )
        }
    }

    private fun filterApps(apps: List<LauncherAppItem>, query: String): List<LauncherAppItem> {
        if (query.isBlank()) return apps
        val lowerQuery = query.lowercase()
        return apps.filter {
            it.appName.lowercase().contains(lowerQuery) ||
                    it.packageName.lowercase().contains(lowerQuery)
        }
    }

    // ─── App Launching ───────────────────────────────────

    fun launchApp(packageName: String) {
        val appItem = _uiState.value.apps.find { it.packageName == packageName }

        // Log launch event
        viewModelScope.launch(Dispatchers.IO) {
            appLaunchEventDao.insert(
                AppLaunchEventEntity(
                    timestamp = System.currentTimeMillis(),
                    packageName = packageName
                )
            )
        }

        // Determine intervention threshold based on friction level
        val usageThreshold = when (_uiState.value.frictionLevel) {
            "gentle" -> 60L
            "strict" -> 15L
            else -> 30L // balanced
        }

        // Check for rapid launches
        if (_uiState.value.interventionEnabled && appItem != null && appItem.isDistracting) {
            viewModelScope.launch {
                val tenMinutesAgo = System.currentTimeMillis() - 10 * 60 * 1000
                val rapidLaunches = withContext(Dispatchers.IO) {
                    appLaunchEventDao.countLaunchesSince(packageName, tenMinutesAgo)
                }

                if (appItem.dailyUsageMinutes >= usageThreshold || rapidLaunches >= 3) {
                    _uiState.update {
                        it.copy(
                            showInterventionFor = appItem,
                            interventionLaunchCount = rapidLaunches
                        )
                    }
                    return@launch
                }

                performLaunch(packageName)
            }
            return
        }

        performLaunch(packageName)
    }

    fun confirmLaunch(packageName: String) {
        // Log intervention choice
        viewModelScope.launch(Dispatchers.IO) {
            interventionEventDao.insert(
                InterventionEventEntity(
                    timestamp = System.currentTimeMillis(),
                    packageName = packageName,
                    userChoice = "continue"
                )
            )
        }
        _uiState.update { it.copy(showInterventionFor = null, interventionLaunchCount = 0) }
        performLaunch(packageName)
    }

    fun dismissIntervention() {
        val pkg = _uiState.value.showInterventionFor?.packageName
        if (pkg != null) {
            viewModelScope.launch(Dispatchers.IO) {
                interventionEventDao.insert(
                    InterventionEventEntity(
                        timestamp = System.currentTimeMillis(),
                        packageName = pkg,
                        userChoice = "break"
                    )
                )
            }
        }
        _uiState.update { it.copy(showInterventionFor = null, interventionLaunchCount = 0) }
    }

    private fun performLaunch(packageName: String) {
        val intent = installedAppsManager.getLaunchIntent(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                getApplication<Application>().startActivity(intent)
            } catch (e: Exception) {
                // App might have been uninstalled
                installedAppsManager.invalidateCache()
                lastFullRefreshTimestamp = 0 // Allow immediate refresh
                loadApps()
            }
        }
    }

    // ─── Scrambling ──────────────────────────────────────

    fun scrambleNow(force: Boolean = false) {
        viewModelScope.launch {

            if (!_uiState.value.scramblingEnabled) return@launch

            val positions = withContext(Dispatchers.IO) { positionDao.getAllPositionsList() }

            if (positions.isEmpty()) return@launch

            val frequencyHours = _uiState.value.scramblingFrequency

            val historySince =
                System.currentTimeMillis() - (7L * 24L * 60L * 60L * 1000L)

            val history = withContext(Dispatchers.IO) {
                app.database.scramblePositionHistoryDao()
                    .getRecentHistory(historySince)
            }

            val recentPositionsByPackage =
                history.groupBy { it.packageName }
                    .mapValues { (_, values) ->
                        values
                            .sortedByDescending { it.timestamp }
                            .map { it.position }
                            .distinct()
                    }

            val distractingPackages = positions
                .filter { it.isDistracting }
                .filter {
                    force ||
                            scramblingEngine.isScrambleDue(
                                it.lastScrambledTimestamp,
                                frequencyHours
                            )
                }
                .map { it.packageName }
                .toSet()

            if (distractingPackages.isEmpty()) return@launch

            val strategy = ScrambleStrategy.fromString(_uiState.value.scrambleStrategy)

            val newPositions = scramblingEngine.scramble(
                allPositions = positions,
                distractingPackages = distractingPackages,
                config = ScramblingEngine.ScrambleConfig(
                    intensity = _uiState.value.scramblingIntensity,
                    gridColumns = _uiState.value.gridColumns,
                    recentPositionsByPackage = recentPositionsByPackage,
                    protectedPackages = AppInfo.ESSENTIAL_PACKAGES,
                    strategy = strategy,
                    focusModeActive = focusManager.isActive
                )
            )

            val changes =
                scramblingEngine.getScrambleSummary(
                    positions,
                    newPositions
                )

            if (changes.isEmpty()) return@launch

            withContext(Dispatchers.IO) {
                positionDao.upsertAll(newPositions)

                val now = System.currentTimeMillis()

                changes.forEach { change ->
                    scrambleEventDao.insert(
                        ScrambleEventEntity(
                            timestamp = now,
                            packageName = change.packageName,
                            fromPosition = change.fromPosition,
                            toPosition = change.toPosition
                        )
                    )

                    app.database.scramblePositionHistoryDao().insert(
                        ScramblePositionHistoryEntity(
                            packageName = change.packageName,
                            position = change.toPosition,
                            timestamp = now
                        )
                    )
                }
            }

            // Compute icon disguises for scrambled apps
            if (_uiState.value.iconDisguiseEnabled) {
                val scrambledPackages = changes.map { it.packageName }.toSet()
                val currentApps = _uiState.value.apps
                val usageMap = currentApps.associate { it.packageName to it.dailyUsageMinutes }
                val newDisguises = iconDisguiseEngine.computeDisguises(
                    scrambledPackages = scrambledPackages,
                    allApps = currentApps,
                    usageMap = usageMap,
                    currentDisguises = _uiState.value.activeDisguises
                )
                _uiState.update { it.copy(activeDisguises = newDisguises) }
            }

            lastFullRefreshTimestamp = 0 // Allow immediate refresh
            loadApps(performAutomaticScramble = false)
        }
    }

    /**
     * Performs anti-routine scramble — subtly moves one frequent app.
     */
    fun performAntiRoutineScramble() {
        if (!_uiState.value.antiRoutineEnabled) return

        viewModelScope.launch {
            val positions = withContext(Dispatchers.IO) { positionDao.getAllPositionsList() }
            if (positions.isEmpty()) return@launch

            // Get recent launch sequence for pattern detection
            val dayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000
            val launchSequence = withContext(Dispatchers.IO) {
                appLaunchEventDao.getPackageSequenceSince(dayAgo)
            }
            val frequentApps = patternDetector.getFrequentApps(launchSequence)

            if (frequentApps.isEmpty()) return@launch

            val historySince = System.currentTimeMillis() - (7L * 24L * 60L * 60L * 1000L)
            val history = withContext(Dispatchers.IO) {
                app.database.scramblePositionHistoryDao().getRecentHistory(historySince)
            }
            val recentPositionsByPackage = history.groupBy { it.packageName }
                .mapValues { (_, values) ->
                    values.sortedByDescending { it.timestamp }.map { it.position }.distinct()
                }

            val newPositions = scramblingEngine.antiRoutineScramble(
                allPositions = positions,
                frequentPackages = frequentApps,
                config = ScramblingEngine.ScrambleConfig(
                    intensity = _uiState.value.scramblingIntensity,
                    gridColumns = _uiState.value.gridColumns,
                    recentPositionsByPackage = recentPositionsByPackage,
                    protectedPackages = AppInfo.ESSENTIAL_PACKAGES
                )
            )

            val changes = scramblingEngine.getScrambleSummary(positions, newPositions)
            if (changes.isEmpty()) return@launch

            withContext(Dispatchers.IO) {
                positionDao.upsertAll(newPositions)
                val now = System.currentTimeMillis()
                changes.forEach { change ->
                    scrambleEventDao.insert(
                        ScrambleEventEntity(
                            timestamp = now,
                            packageName = change.packageName,
                            fromPosition = change.fromPosition,
                            toPosition = change.toPosition
                        )
                    )
                }
            }

            lastFullRefreshTimestamp = 0
            loadApps(performAutomaticScramble = false)
        }
    }

    // ─── Distraction Management ──────────────────────────

    fun toggleDistracting(packageName: String) {
        viewModelScope.launch {
            val entity = withContext(Dispatchers.IO) { positionDao.getPosition(packageName) } ?: return@launch
            withContext(Dispatchers.IO) {
                positionDao.setDistracting(packageName, !entity.isDistracting, true)
            }
            // Update in-place instead of full reload
            _uiState.update { state ->
                val updatedApps = state.apps.map { app ->
                    if (app.packageName == packageName) {
                        app.copy(isDistracting = !app.isDistracting)
                    } else app
                }
                state.copy(
                    apps = updatedApps,
                    filteredApps = if (state.isSearchActive) filterApps(updatedApps, state.searchQuery) else updatedApps,
                    distractingApps = updatedApps.filter { it.isDistracting }
                )
            }
        }
    }

    fun setDistracting(packageName: String, isDistracting: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                positionDao.setDistracting(packageName, isDistracting, true)
            }
            _uiState.update { state ->
                val updatedApps = state.apps.map { app ->
                    if (app.packageName == packageName) {
                        app.copy(isDistracting = isDistracting)
                    } else app
                }
                state.copy(
                    apps = updatedApps,
                    filteredApps = if (state.isSearchActive) filterApps(updatedApps, state.searchQuery) else updatedApps,
                    distractingApps = updatedApps.filter { it.isDistracting }
                )
            }
        }
    }

    // ─── Focus Mode ──────────────────────────────────────

    fun startFocusSession(durationMinutes: Int? = null) {
        val duration = durationMinutes ?: _uiState.value.focusModeDuration
        focusManager.startSession(duration)

        // Trigger a focus-mode scramble
        if (_uiState.value.scramblingEnabled) {
            viewModelScope.launch {
                // Temporarily use focus strategy for this scramble
                val savedStrategy = _uiState.value.scrambleStrategy
                _uiState.update { it.copy(scrambleStrategy = "focus") }
                scrambleNow(force = true)
                _uiState.update { it.copy(scrambleStrategy = savedStrategy) }
            }
        }
    }

    fun endFocusSession() {
        focusManager.endSession()
        refreshStats()
    }

    fun dismissFocusCompletion() {
        focusManager.dismissCompletion()
        refreshStats()
    }

    private fun refreshStats() {
        viewModelScope.launch {
            val todayStart = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            val focusMinutes = focusManager.getTodayFocusMinutes()
            val focusStreak = focusManager.getFocusStreak()
            val interventionCount = withContext(Dispatchers.IO) {
                interventionEventDao.countSince(todayStart)
            }
            val breakCount = withContext(Dispatchers.IO) {
                interventionEventDao.countBreaksSince(todayStart)
            }
            _uiState.update {
                it.copy(
                    todayFocusMinutes = focusMinutes,
                    focusStreak = focusStreak,
                    interventionCountToday = interventionCount,
                    breakCountToday = breakCount
                )
            }
        }
    }

    // ─── Settings ────────────────────────────────────────

    fun setOnboardingComplete() {
        viewModelScope.launch {
            settingsRepository.setOnboardingComplete(true)
        }
    }

    fun setScramblingEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setScramblingEnabled(enabled) }
    }

    fun setScramblingIntensity(intensity: String) {
        viewModelScope.launch { settingsRepository.setScramblingIntensity(intensity) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setGridColumns(columns: Int) {
        viewModelScope.launch { settingsRepository.setGridColumns(columns) }
    }

    fun setShowLabels(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowAppLabels(show) }
    }

    fun setInterventionEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setInterventionEnabled(enabled) }
    }

    fun setScramblingThreshold(minutes: Int) {
        viewModelScope.launch { settingsRepository.setScramblingThresholdMinutes(minutes) }
    }

    fun setScramblingFrequency(hours: Int) {
        viewModelScope.launch { settingsRepository.setScramblingFrequencyHours(hours) }
    }

    fun setDemoMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDemoModeEnabled(enabled) }
    }

    fun setFrictionLevel(level: String) {
        viewModelScope.launch { settingsRepository.setFrictionLevel(level) }
    }

    fun setScrambleStrategy(strategy: String) {
        viewModelScope.launch { settingsRepository.setScrambleStrategy(strategy) }
    }

    fun setAntiRoutineEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAntiRoutineEnabled(enabled) }
    }

    fun setIconDisguiseEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setIconDisguiseEnabled(enabled)
            if (!enabled) {
                clearDisguises()
            }
        }
    }

    fun clearDisguises() {
        _uiState.update { it.copy(activeDisguises = emptyMap()) }
        lastFullRefreshTimestamp = 0
        loadApps(performAutomaticScramble = false)
    }

    fun setFocusModeDuration(minutes: Int) {
        viewModelScope.launch { settingsRepository.setFocusModeDurationMinutes(minutes) }
    }

    // ─── Demo Mode ───────────────────────────────────────

    fun demoAddUsage(packageName: String, minutes: Long) {
        val current = _uiState.value.demoUsageOverrides.toMutableMap()
        current[packageName] = (current[packageName] ?: 0L) + minutes
        _uiState.update { it.copy(demoUsageOverrides = current) }
        lastFullRefreshTimestamp = 0
        loadApps()
    }


    fun demoResetLayout() {
        viewModelScope.launch {
            val positions = withContext(Dispatchers.IO) { positionDao.getAllPositionsList() }
            val sorted = positions.sortedBy { it.packageName }
            val reindexed = sorted.mapIndexed { index, entity ->
                entity.copy(position = index, isDistracting = false, scrambleCount = 0)
            }
            withContext(Dispatchers.IO) { positionDao.upsertAll(reindexed) }
            _uiState.update { it.copy(demoUsageOverrides = emptyMap()) }
            lastFullRefreshTimestamp = 0
            loadApps()
        }
    }

    // ─── Reset ───────────────────────────────────────────

    fun resetAppPositions() {
        viewModelScope.launch {
            val positions = withContext(Dispatchers.IO) { positionDao.getAllPositionsList() }
            val alphabetical = positions.sortedBy { it.packageName }
            val reindexed = alphabetical.mapIndexed { index, entity ->
                entity.copy(position = index)
            }
            withContext(Dispatchers.IO) { positionDao.upsertAll(reindexed) }
            lastFullRefreshTimestamp = 0
            loadApps()
        }
    }

    fun clearAllStatistics() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dailyUsageDao.deleteAll()
                scrambleEventDao.deleteAll()
                app.database.scramblePositionHistoryDao().deleteAll()
                focusSessionDao.deleteAll()
                interventionEventDao.deleteAll()
                appLaunchEventDao.deleteAll()
            }
            lastFullRefreshTimestamp = 0
            loadApps()
        }
    }

    // ─── Usage Permission ────────────────────────────────

    fun checkUsagePermission() {
        _uiState.update {
            it.copy(hasUsagePermission = usageStatsRepository.hasUsagePermission())
        }
    }

    fun getUsageSettingsIntent(): Intent {
        return usageStatsRepository.getUsageSettingsIntent()
    }

    // ─── Utility ─────────────────────────────────────────

    fun formatScreenTime(minutes: Long): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    }
}
