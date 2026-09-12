package com.appmaze.focus

import com.appmaze.data.FocusSessionEntity
import com.appmaze.data.FocusSessionDao
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents the current state of Focus Mode.
 */
sealed class FocusState {
    data object Inactive : FocusState()

    data class Active(
        val totalMinutes: Int,
        val remainingSeconds: Long,
        val startTime: Long
    ) : FocusState() {
        val progress: Float
            get() {
                val totalSeconds = totalMinutes * 60L
                return if (totalSeconds > 0) {
                    1f - (remainingSeconds.toFloat() / totalSeconds)
                } else 0f
            }

        val remainingMinutes: Int get() = (remainingSeconds / 60).toInt()
        val remainingSecondsInMinute: Int get() = (remainingSeconds % 60).toInt()
    }

    data class Completed(
        val durationMinutes: Int
    ) : FocusState()
}

/**
 * Manages Focus Mode sessions using coroutine-based countdown.
 * Persists completed sessions to the database for statistics.
 */
class FocusManager(
    private val focusSessionDao: FocusSessionDao,
    private val scope: CoroutineScope
) {

    private val _state = MutableStateFlow<FocusState>(FocusState.Inactive)
    val state: StateFlow<FocusState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var sessionStartTime: Long = 0L

    /**
     * Starts a new focus session with the given duration.
     */
    fun startSession(durationMinutes: Int) {
        cancelSession() // Cancel any existing session

        sessionStartTime = System.currentTimeMillis()
        val totalSeconds = durationMinutes * 60L

        timerJob = scope.launch {
            var remaining = totalSeconds
            _state.value = FocusState.Active(
                totalMinutes = durationMinutes,
                remainingSeconds = remaining,
                startTime = sessionStartTime
            )

            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining--
                _state.value = FocusState.Active(
                    totalMinutes = durationMinutes,
                    remainingSeconds = remaining,
                    startTime = sessionStartTime
                )
            }

            if (isActive) {
                // Session completed successfully
                val endTime = System.currentTimeMillis()
                focusSessionDao.insert(
                    FocusSessionEntity(
                        startTime = sessionStartTime,
                        endTime = endTime,
                        durationMinutes = durationMinutes,
                        completed = true
                    )
                )
                _state.value = FocusState.Completed(durationMinutes)
            }
        }
    }

    /**
     * Ends the current session early (marks as incomplete).
     */
    fun endSession() {
        val currentState = _state.value
        timerJob?.cancel()
        timerJob = null

        if (currentState is FocusState.Active) {
            scope.launch {
                val endTime = System.currentTimeMillis()
                val elapsedMinutes = ((endTime - sessionStartTime) / 60_000).toInt()
                focusSessionDao.insert(
                    FocusSessionEntity(
                        startTime = sessionStartTime,
                        endTime = endTime,
                        durationMinutes = elapsedMinutes,
                        completed = false
                    )
                )
            }
        }

        _state.value = FocusState.Inactive
    }

    /**
     * Cancels without logging.
     */
    fun cancelSession() {
        timerJob?.cancel()
        timerJob = null
        _state.value = FocusState.Inactive
    }

    /**
     * Dismisses the Completed state and returns to Inactive.
     */
    fun dismissCompletion() {
        _state.value = FocusState.Inactive
    }

    /**
     * Returns the current focus streak — consecutive completed sessions.
     */
    suspend fun getFocusStreak(): Int {
        val sessions = focusSessionDao.getAllSessionsDesc()
        var streak = 0
        for (session in sessions) {
            if (session.completed) {
                streak++
            } else {
                break
            }
        }
        return streak
    }

    /**
     * Returns total focused minutes today.
     */
    suspend fun getTodayFocusMinutes(): Long {
        val todayStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        return focusSessionDao.totalFocusMinutesSince(todayStart) ?: 0L
    }

    val isActive: Boolean
        get() = _state.value is FocusState.Active
}
