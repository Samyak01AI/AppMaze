package com.appmaze.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "appmaze_settings")

/**
 * Manages app settings using DataStore Preferences.
 */
class SettingsRepository(private val context: Context) {

    // ─── Keys ────────────────────────────────────────
    private object Keys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val SCRAMBLING_ENABLED = booleanPreferencesKey("scrambling_enabled")
        val SCRAMBLING_INTENSITY = stringPreferencesKey("scrambling_intensity")
        val SCRAMBLING_THRESHOLD_MINUTES = intPreferencesKey("scrambling_threshold_minutes")
        val SCRAMBLING_FREQUENCY_HOURS = intPreferencesKey("scrambling_frequency_hours")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val SHOW_APP_LABELS = booleanPreferencesKey("show_app_labels")
        val THEME_MODE = stringPreferencesKey("theme_mode") // "light", "dark", "system"
        val INTERVENTION_ENABLED = booleanPreferencesKey("intervention_enabled")
        val DEMO_MODE_ENABLED = booleanPreferencesKey("demo_mode_enabled")
        // New settings
        val FRICTION_LEVEL = stringPreferencesKey("friction_level") // "gentle", "balanced", "strict"
        val SCRAMBLE_STRATEGY = stringPreferencesKey("scramble_strategy") // "random", "adaptive", "aggressive", "focus"
        val FOCUS_MODE_DURATION_MINUTES = intPreferencesKey("focus_mode_duration_minutes")
        val ANTI_ROUTINE_ENABLED = booleanPreferencesKey("anti_routine_enabled")
        val ICON_DISGUISE_ENABLED = booleanPreferencesKey("icon_disguise_enabled")
    }

    // ─── Flows ───────────────────────────────────────

    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETE] ?: false
    }

    val scramblingEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SCRAMBLING_ENABLED] ?: true
    }

    val scramblingIntensity: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.SCRAMBLING_INTENSITY] ?: "moderate"
    }

    val scramblingThresholdMinutes: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.SCRAMBLING_THRESHOLD_MINUTES] ?: 45
    }

    val scramblingFrequencyHours: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.SCRAMBLING_FREQUENCY_HOURS] ?: 6
    }

    val gridColumns: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.GRID_COLUMNS] ?: 4
    }

    val showAppLabels: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SHOW_APP_LABELS] ?: true
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE] ?: "system"
    }

    val interventionEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.INTERVENTION_ENABLED] ?: true
    }

    val demoModeEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DEMO_MODE_ENABLED] ?: false
    }

    val frictionLevel: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.FRICTION_LEVEL] ?: "balanced"
    }

    val scrambleStrategy: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.SCRAMBLE_STRATEGY] ?: "adaptive"
    }

    val focusModeDurationMinutes: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.FOCUS_MODE_DURATION_MINUTES] ?: 25
    }

    val antiRoutineEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ANTI_ROUTINE_ENABLED] ?: true
    }

    val iconDisguiseEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ICON_DISGUISE_ENABLED] ?: true
    }

    // ─── Setters ─────────────────────────────────────

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setScramblingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SCRAMBLING_ENABLED] = enabled }
    }

    suspend fun setScramblingIntensity(intensity: String) {
        context.dataStore.edit { it[Keys.SCRAMBLING_INTENSITY] = intensity }
    }

    suspend fun setScramblingThresholdMinutes(minutes: Int) {
        context.dataStore.edit {
            it[Keys.SCRAMBLING_THRESHOLD_MINUTES] = minutes.coerceAtLeast(1)
        }
    }

    suspend fun setScramblingFrequencyHours(hours: Int) {
        context.dataStore.edit { it[Keys.SCRAMBLING_FREQUENCY_HOURS] = hours }
    }

    suspend fun setGridColumns(columns: Int) {
        context.dataStore.edit { it[Keys.GRID_COLUMNS] = columns }
    }

    suspend fun setShowAppLabels(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_APP_LABELS] = show }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setInterventionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.INTERVENTION_ENABLED] = enabled }
    }

    suspend fun setDemoModeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DEMO_MODE_ENABLED] = enabled }
    }

    suspend fun setFrictionLevel(level: String) {
        context.dataStore.edit { it[Keys.FRICTION_LEVEL] = level }
    }

    suspend fun setScrambleStrategy(strategy: String) {
        context.dataStore.edit { it[Keys.SCRAMBLE_STRATEGY] = strategy }
    }

    suspend fun setFocusModeDurationMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.FOCUS_MODE_DURATION_MINUTES] = minutes.coerceIn(5, 120) }
    }

    suspend fun setAntiRoutineEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ANTI_ROUTINE_ENABLED] = enabled }
    }

    suspend fun setIconDisguiseEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ICON_DISGUISE_ENABLED] = enabled }
    }

    suspend fun clearAllData() {
        context.dataStore.edit { it.clear() }
    }
}
