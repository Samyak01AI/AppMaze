package com.appmaze.scrambling

import com.appmaze.data.AppPositionEntity
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Scrambling strategies that control how aggressively apps are repositioned.
 */
enum class ScrambleStrategy(val label: String) {
    RANDOM("Random"),
    ADAPTIVE("Adaptive"),
    AGGRESSIVE("Aggressive"),
    FOCUS("Focus Mode");

    companion object {
        fun fromString(value: String): ScrambleStrategy =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: ADAPTIVE
    }
}

class ScramblingEngine {

    data class ScrambleConfig(
        val intensity: String,
        val gridColumns: Int,
        val recentPositionsByPackage: Map<String, List<Int>> = emptyMap(),
        val protectedPackages: Set<String> = emptySet(),
        val strategy: ScrambleStrategy = ScrambleStrategy.ADAPTIVE,
        val focusModeActive: Boolean = false
    )

    fun scramble(
        allPositions: List<AppPositionEntity>,
        distractingPackages: Set<String>,
        config: ScrambleConfig
    ): List<AppPositionEntity> {

        if (allPositions.isEmpty() || distractingPackages.isEmpty()) {
            return allPositions
        }

        return when (config.strategy) {
            ScrambleStrategy.RANDOM -> scrambleRandom(allPositions, distractingPackages, config)
            ScrambleStrategy.ADAPTIVE -> scrambleAdaptive(allPositions, distractingPackages, config)
            ScrambleStrategy.AGGRESSIVE -> scrambleAggressive(allPositions, distractingPackages, config)
            ScrambleStrategy.FOCUS -> scrambleFocus(allPositions, distractingPackages, config)
        }
    }

    /**
     * RANDOM: Shuffles distracting apps to random positions.
     */
    private fun scrambleRandom(
        allPositions: List<AppPositionEntity>,
        distractingPackages: Set<String>,
        config: ScrambleConfig
    ): List<AppPositionEntity> {
        val result = allPositions.sortedBy { it.position }.toMutableList()

        val candidates = result
            .filter { it.packageName in distractingPackages && it.packageName !in config.protectedPackages }
            .map { it.packageName }

        val scrambleLimit = when (config.intensity.lowercase()) {
            "gentle" -> 1
            "moderate" -> minOf(2, candidates.size)
            "strong" -> candidates.size
            else -> minOf(1, candidates.size)
        }

        for (packageName in candidates.take(scrambleLimit)) {
            val app = result.firstOrNull { it.packageName == packageName } ?: continue
            val currentPosition = app.position

            // Pick a random non-protected position that isn't the current one
            val availablePositions = result
                .filter { it.packageName !in config.protectedPackages && it.position != currentPosition }
                .map { it.position }
                .filter { it !in (config.recentPositionsByPackage[packageName].orEmpty()) }

            val newPosition = availablePositions.randomOrNull() ?: continue
            swapPositions(result, packageName, currentPosition, newPosition)
        }

        return result.sortedBy { it.position }
    }

    /**
     * ADAPTIVE: Uses distraction scores to determine scramble distance and frequency.
     * Higher distraction → more movement. This is the refined version of the original algorithm.
     */
    private fun scrambleAdaptive(
        allPositions: List<AppPositionEntity>,
        distractingPackages: Set<String>,
        config: ScrambleConfig
    ): List<AppPositionEntity> {

        val result = allPositions.sortedBy { it.position }.toMutableList()

        val candidatePackages = result
            .filter {
                it.packageName in distractingPackages &&
                        it.packageName !in config.protectedPackages
            }
            .sortedByDescending { it.distractionScore }
            .map { it.packageName }

        val scrambleLimit = when (config.intensity.lowercase()) {
            "gentle" -> 1
            "moderate" -> minOf(2, candidatePackages.size)
            "strong" -> candidatePackages.size
            "adaptive" -> {
                when {
                    result.any { it.packageName in candidatePackages && it.distractionScore >= 85 } -> candidatePackages.size
                    result.any { it.packageName in candidatePackages && it.distractionScore >= 65 } -> minOf(3, candidatePackages.size)
                    else -> minOf(1, candidatePackages.size)
                }
            }
            else -> minOf(1, candidatePackages.size)
        }

        for (packageName in candidatePackages.take(scrambleLimit)) {

            val app = result.firstOrNull { it.packageName == packageName } ?: continue
            val currentPosition = app.position

            val targetDistance = when (config.intensity.lowercase()) {
                "gentle" -> config.gridColumns
                "moderate" -> config.gridColumns * 2
                "strong" -> config.gridColumns * 3
                "adaptive" -> {
                    when {
                        app.distractionScore >= 90 -> config.gridColumns * 3
                        app.distractionScore >= 70 -> config.gridColumns * 2
                        else -> config.gridColumns
                    }
                }
                else -> config.gridColumns
            }

            val recentPositions =
                config.recentPositionsByPackage[app.packageName].orEmpty()

            val bestPosition = result
                .asSequence()
                .filter { slot -> slot.position != currentPosition }
                .filter { slot -> slot.position !in recentPositions }
                .filter { slot -> slot.packageName !in config.protectedPackages }
                .map { slot -> slot.position }
                .maxByOrNull { candidate ->
                    scoreCandidate(
                        currentPosition = currentPosition,
                        candidatePosition = candidate,
                        targetDistance = targetDistance,
                        gridColumns = config.gridColumns,
                        recentPositions = recentPositions
                    )
                }

            if (bestPosition != null) {
                swapPositions(result, packageName, currentPosition, bestPosition)
            }
        }

        return result.sortedBy { it.position }
    }

    /**
     * AGGRESSIVE: Moves ALL distracting apps every time, maximizing unpredictability.
     */
    private fun scrambleAggressive(
        allPositions: List<AppPositionEntity>,
        distractingPackages: Set<String>,
        config: ScrambleConfig
    ): List<AppPositionEntity> {
        val result = allPositions.sortedBy { it.position }.toMutableList()

        val candidates = result
            .filter { it.packageName in distractingPackages && it.packageName !in config.protectedPackages }
            .sortedByDescending { it.distractionScore }

        for (app in candidates) {
            val currentPosition = app.position
            val recentPositions = config.recentPositionsByPackage[app.packageName].orEmpty()

            // Move as far as possible
            val targetDistance = config.gridColumns * 3

            val bestPosition = result
                .asSequence()
                .filter { it.position != currentPosition }
                .filter { it.position !in recentPositions }
                .filter { it.packageName !in config.protectedPackages }
                .map { it.position }
                .maxByOrNull { candidate ->
                    scoreCandidate(currentPosition, candidate, targetDistance, config.gridColumns, recentPositions)
                }

            if (bestPosition != null) {
                swapPositions(result, app.packageName, currentPosition, bestPosition)
            }
        }

        return result.sortedBy { it.position }
    }

    /**
     * FOCUS: Only moves high-distraction apps (score >= 60). Everything else stays stable.
     * Ideal during Focus Mode sessions.
     */
    private fun scrambleFocus(
        allPositions: List<AppPositionEntity>,
        distractingPackages: Set<String>,
        config: ScrambleConfig
    ): List<AppPositionEntity> {
        val result = allPositions.sortedBy { it.position }.toMutableList()

        // Only target high-distraction apps
        val candidates = result
            .filter {
                it.packageName in distractingPackages &&
                        it.packageName !in config.protectedPackages &&
                        it.distractionScore >= 60
            }
            .sortedByDescending { it.distractionScore }

        for (app in candidates) {
            val currentPosition = app.position
            val recentPositions = config.recentPositionsByPackage[app.packageName].orEmpty()

            // Push far
            val targetDistance = config.gridColumns * 4

            val bestPosition = result
                .asSequence()
                .filter { it.position != currentPosition }
                .filter { it.position !in recentPositions }
                .filter { it.packageName !in config.protectedPackages }
                .map { it.position }
                .maxByOrNull { candidate ->
                    scoreCandidate(currentPosition, candidate, targetDistance, config.gridColumns, recentPositions)
                }

            if (bestPosition != null) {
                swapPositions(result, app.packageName, currentPosition, bestPosition)
            }
        }

        return result.sortedBy { it.position }
    }

    /**
     * Anti-routine: Subtly move a single frequently-used app by a small distance.
     * Only affects one app at a time to feel intentional rather than chaotic.
     */
    fun antiRoutineScramble(
        allPositions: List<AppPositionEntity>,
        frequentPackages: List<String>,
        config: ScrambleConfig
    ): List<AppPositionEntity> {
        if (allPositions.isEmpty() || frequentPackages.isEmpty()) return allPositions

        val result = allPositions.sortedBy { it.position }.toMutableList()

        // Pick one frequent app that hasn't been anti-routine scrambled recently
        val candidate = frequentPackages
            .filter { pkg -> pkg !in config.protectedPackages }
            .firstOrNull { pkg ->
                val recentPositions = config.recentPositionsByPackage[pkg].orEmpty()
                recentPositions.size < 3 // Only if it hasn't been moved too often recently
            } ?: return allPositions

        val app = result.firstOrNull { it.packageName == candidate } ?: return allPositions
        val currentPosition = app.position
        val recentPositions = config.recentPositionsByPackage[candidate].orEmpty()

        // Move just 1-3 positions away — subtle but enough to break muscle memory
        val nearbyPositions = result
            .filter { it.position != currentPosition }
            .filter { it.position !in recentPositions }
            .filter { it.packageName !in config.protectedPackages }
            .filter { gridDistance(currentPosition, it.position, config.gridColumns) <= 3.0 }
            .filter { gridDistance(currentPosition, it.position, config.gridColumns) >= 1.0 }
            .map { it.position }

        val newPosition = nearbyPositions.randomOrNull() ?: return allPositions
        swapPositions(result, candidate, currentPosition, newPosition)

        return result.sortedBy { it.position }
    }

    private fun swapPositions(
        result: MutableList<AppPositionEntity>,
        packageName: String,
        currentPosition: Int,
        newPosition: Int
    ) {
        val displacedIndex = result.indexOfFirst { it.position == newPosition }
        val currentIndex = result.indexOfFirst { it.packageName == packageName }

        if (displacedIndex >= 0 && currentIndex >= 0) {
            val displaced = result[displacedIndex]
            val current = result[currentIndex]

            result[currentIndex] = displaced.copy(position = currentPosition)
            result[displacedIndex] = current.copy(
                position = newPosition,
                lastScrambledTimestamp = System.currentTimeMillis(),
                scrambleCount = current.scrambleCount + 1
            )
        }
    }

    private fun scoreCandidate(
        currentPosition: Int,
        candidatePosition: Int,
        targetDistance: Int,
        gridColumns: Int,
        recentPositions: List<Int>
    ): Double {

        val distance = gridDistance(
            currentPosition,
            candidatePosition,
            gridColumns
        )

        val distanceScore =
            -abs(distance - targetDistance).toDouble()

        val recentPenalty =
            if (candidatePosition in recentPositions) -1000.0 else 0.0

        return distanceScore + recentPenalty
    }

    private fun gridDistance(
        from: Int,
        to: Int,
        columns: Int
    ): Double {

        val fromRow = from / columns
        val fromCol = from % columns

        val toRow = to / columns
        val toCol = to % columns

        return sqrt(
            ((toRow - fromRow) * (toRow - fromRow) +
                    (toCol - fromCol) * (toCol - fromCol)).toDouble()
        )
    }

    fun isScrambleDue(
        lastScrambleTimestamp: Long,
        frequencyHours: Int
    ): Boolean {
        if (lastScrambleTimestamp <= 0L) return true

        val elapsed = System.currentTimeMillis() - lastScrambleTimestamp
        val frequencyMs = frequencyHours.coerceAtLeast(1) * 3_600_000L

        return elapsed >= frequencyMs
    }

    fun getScrambleSummary(
        before: List<AppPositionEntity>,
        after: List<AppPositionEntity>
    ): List<PositionChange> {

        val beforeMap = before.associate { it.packageName to it.position }
        val afterMap = after.associate { it.packageName to it.position }

        return afterMap.mapNotNull { (pkg, newPos) ->
            val oldPos = beforeMap[pkg]

            if (oldPos != null && oldPos != newPos) {
                PositionChange(
                    packageName = pkg,
                    fromPosition = oldPos,
                    toPosition = newPos
                )
            } else {
                null
            }
        }
    }
}

data class PositionChange(
    val packageName: String,
    val fromPosition: Int,
    val toPosition: Int
)