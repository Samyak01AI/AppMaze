package com.appmaze.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appmaze.launcher.LauncherUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: LauncherUiState,
    onNavigateBack: () -> Unit,
    onScramblingEnabledChanged: (Boolean) -> Unit,
    onScramblingIntensityChanged: (String) -> Unit,
    onThemeModeChanged: (String) -> Unit,
    onGridColumnsChanged: (Int) -> Unit,
    onShowLabelsChanged: (Boolean) -> Unit,
    onInterventionEnabledChanged: (Boolean) -> Unit,
    onScramblingThresholdChanged: (Int) -> Unit,
    onScramblingFrequencyChanged: (Int) -> Unit,
    onDemoModeChanged: (Boolean) -> Unit,
    onFrictionLevelChanged: (String) -> Unit,
    onScrambleStrategyChanged: (String) -> Unit,
    onAntiRoutineChanged: (Boolean) -> Unit,
    onIconDisguiseChanged: (Boolean) -> Unit,
    onResetPositions: () -> Unit,
    onClearStatistics: () -> Unit,
    onScrambleNow: () -> Unit,
    onRequestUsagePermission: () -> Unit,
    onNavigateToManageApps: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // ─── Friction Level ──────────────────────────
            SettingsSection("Friction Level") {
                SettingsFrictionSelector(
                    selectedLevel = uiState.frictionLevel,
                    onLevelSelected = onFrictionLevelChanged
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ─── Scrambling Section ──────────────────────
            SettingsSection("Scrambling") {
                SettingsToggle(
                    title = "Enable Scrambling",
                    subtitle = "Automatically move distracting apps",
                    icon = Icons.Outlined.Shuffle,
                    checked = uiState.scramblingEnabled,
                    onCheckedChange = onScramblingEnabledChanged
                )

                if (uiState.scramblingEnabled) {
                    SettingsStrategySelector(
                        selectedStrategy = uiState.scrambleStrategy,
                        onStrategySelected = onScrambleStrategyChanged
                    )

                    SettingsIntensitySelector(
                        selectedIntensity = uiState.scramblingIntensity,
                        onIntensitySelected = onScramblingIntensityChanged
                    )

                    SettingsSlider(
                        title = "Usage Threshold",
                        subtitle = "Scramble when daily usage exceeds this",
                        value = uiState.scramblingThreshold.toFloat(),
                        valueRange = 15f..120f,
                        steps = 6,
                        valueLabel = { "${it.toInt()} min" },
                        onValueChange = { onScramblingThresholdChanged(it.toInt()) }
                    )

                    SettingsSlider(
                        title = "Scramble Frequency",
                        subtitle = "Minimum hours between scrambles",
                        value = uiState.scramblingFrequency.toFloat(),
                        valueRange = 1f..24f,
                        steps = 22,
                        valueLabel = { "${it.toInt()} hrs" },
                        onValueChange = { onScramblingFrequencyChanged(it.toInt()) }
                    )

                    SettingsToggle(
                        title = "Anti-Routine Mode",
                        subtitle = "Subtly move frequently-used apps to prevent muscle memory",
                        icon = Icons.Outlined.SwapHoriz,
                        checked = uiState.antiRoutineEnabled,
                        onCheckedChange = onAntiRoutineChanged
                    )

                    SettingsToggle(
                        title = "Icon Disguise",
                        subtitle = "Replace icons of scrambled distracting apps with boring app icons",
                        icon = Icons.Outlined.TheaterComedy,
                        checked = uiState.iconDisguiseEnabled,
                        onCheckedChange = onIconDisguiseChanged
                    )

                    SettingsToggle(
                        title = "Intervention Dialog",
                        subtitle = "Show a prompt before opening distracting apps",
                        icon = Icons.Outlined.Warning,
                        checked = uiState.interventionEnabled,
                        onCheckedChange = onInterventionEnabledChanged
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(onClick = onScrambleNow),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Scramble Now",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ─── Apps Section ────────────────────────────
            SettingsSection("Apps") {
                SettingsAction(
                    title = "Manage Distracting Apps",
                    subtitle = "${uiState.distractingApps.size} apps marked as distracting",
                    icon = Icons.Outlined.Apps,
                    onClick = onNavigateToManageApps
                )

                SettingsAction(
                    title = "Reset App Positions",
                    subtitle = "Restore alphabetical ordering",
                    icon = Icons.Outlined.RestartAlt,
                    onClick = onResetPositions
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ─── Launcher Section ────────────────────────
            SettingsSection("Launcher") {
                SettingsSlider(
                    title = "Grid Columns",
                    subtitle = "Number of columns in the app grid",
                    value = uiState.gridColumns.toFloat(),
                    valueRange = 3f..6f,
                    steps = 2,
                    valueLabel = { "${it.toInt()}" },
                    onValueChange = { onGridColumnsChanged(it.toInt()) }
                )

                SettingsToggle(
                    title = "Show App Labels",
                    subtitle = "Display app names below icons",
                    icon = Icons.Outlined.TextFields,
                    checked = uiState.showLabels,
                    onCheckedChange = onShowLabelsChanged
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ─── Appearance Section ──────────────────────
            SettingsSection("Appearance") {
                SettingsThemeSelector(
                    selectedTheme = uiState.themeMode,
                    onThemeSelected = onThemeModeChanged
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ─── Privacy Section ─────────────────────────
            SettingsSection("Privacy") {
                SettingsAction(
                    title = "Usage Access",
                    subtitle = if (uiState.hasUsagePermission) "Permission granted" else "Permission not granted",
                    icon = if (uiState.hasUsagePermission) Icons.Filled.CheckCircle else Icons.Outlined.Shield,
                    onClick = onRequestUsagePermission
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "All your data stays on this device. Shuffle does not send any information to external servers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SettingsAction(
                    title = "Clear Statistics",
                    subtitle = "Remove all usage history and scramble records",
                    icon = Icons.Outlined.DeleteOutline,
                    onClick = onClearStatistics,
                    destructive = true
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ─── Developer Section ───────────────────────
            SettingsSection("Developer") {
                SettingsToggle(
                    title = "Demo Mode",
                    subtitle = "Simulate usage data for demonstration",
                    icon = Icons.Outlined.Science,
                    checked = uiState.demoModeEnabled,
                    onCheckedChange = onDemoModeChanged
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        content()
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun SettingsAction(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (destructive)
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null,
                tint = if (destructive) MaterialTheme.colorScheme.error
                       else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge,
                    color = if (destructive) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsSlider(
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: (Float) -> String,
    onValueChange: (Float) -> Unit
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    valueLabel(sliderValue),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onValueChange(sliderValue) },
                valueRange = valueRange,
                steps = steps
            )
        }
    }
}

@Composable
private fun SettingsFrictionSelector(
    selectedLevel: String,
    onLevelSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("How much friction do you want?", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    "gentle" to "Gentle",
                    "balanced" to "Balanced",
                    "strict" to "Strict"
                ).forEach { (key, label) ->
                    val isSelected = selectedLevel == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { onLevelSelected(key) },
                        label = { Text(label) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsStrategySelector(
    selectedStrategy: String,
    onStrategySelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Scramble Strategy", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    "random" to "Random",
                    "adaptive" to "Adaptive",
                    "aggressive" to "Aggr.",
                    "focus" to "Focus"
                ).forEach { (key, label) ->
                    val isSelected = selectedStrategy == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { onStrategySelected(key) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsIntensitySelector(
    selectedIntensity: String,
    onIntensitySelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Intensity", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("gentle" to "Gentle", "moderate" to "Moderate", "strong" to "Strong").forEach { (key, label) ->
                    val isSelected = selectedIntensity == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { onIntensitySelected(key) },
                        label = { Text(label) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsThemeSelector(
    selectedTheme: String,
    onThemeSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Theme", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    "system" to "System",
                    "light" to "Light",
                    "dark" to "Dark"
                ).forEach { (key, label) ->
                    val isSelected = selectedTheme == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { onThemeSelected(key) },
                        label = { Text(label) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }
        }
    }
}
