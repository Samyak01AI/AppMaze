package com.appmaze.usage

import kotlin.math.roundToInt

class UsageAnalyzer {

    fun calculateDistractionScore(
        dailyUsageMinutes: Long,
        launchCount: Int = 0,
        thresholdMinutes: Int = 45,
        manuallyMarked: Boolean = false,
        habitScore: Int = 0
    ): Int {

        if (dailyUsageMinutes <= 0L && launchCount <= 0 && !manuallyMarked) {
            return 0
        }

        val usageRatio =
            (dailyUsageMinutes.toDouble() /
                    thresholdMinutes.coerceAtLeast(1))
                .coerceIn(0.0, 2.0)

        val usageScore = (usageRatio * 40.0).coerceIn(0.0, 40.0)

        val launchScore =
            (launchCount.coerceAtMost(20) / 20.0) * 20.0

        val habitComponent =
            (habitScore.coerceIn(0, 100) / 100.0) * 20.0

        val manualBonus =
            if (manuallyMarked) 20.0 else 0.0

        return (
                usageScore +
                        launchScore +
                        habitComponent +
                        manualBonus
                ).roundToInt().coerceIn(0, 100)
    }

    fun calculateHabitScore(
        launchCount: Int,
        dailyUsageMinutes: Long,
        repeatedTimeWindowAccesses: Int
    ): Int {

        val launchScore =
            (launchCount * 4).coerceAtMost(45)

        val usageScore =
            (dailyUsageMinutes / 2).coerceAtMost(35)

        val timingScore =
            (repeatedTimeWindowAccesses * 5).coerceAtMost(20)

        return (launchScore + usageScore + timingScore)
            .toInt()
            .coerceIn(0, 100)
    }

    fun shouldAutoClassifyAsDistracting(
        dailyUsageMinutes: Long,
        thresholdMinutes: Int,
        isEssential: Boolean
    ): Boolean {
        if (isEssential) return false

        return dailyUsageMinutes >= thresholdMinutes
    }
}