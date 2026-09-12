package com.appmaze.usage

/**
 * Human-readable distraction level derived from the numerical score.
 */
enum class DistractionLevel(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    EXTREME("Extreme");

    companion object {
        fun fromScore(score: Int): DistractionLevel = when {
            score >= 80 -> EXTREME
            score >= 55 -> HIGH
            score >= 30 -> MEDIUM
            else -> LOW
        }
    }
}
