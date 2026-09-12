package com.appmaze.scrambling

/**
 * Detects sequential app launch patterns to inform intelligent scrambling.
 * Uses simple transition counting (lightweight Markov chain).
 */
class PatternDetector {

    /**
     * Analyzes launch events to find frequent app sequences.
     * @param launchSequence List of package names in chronological order
     * @return Map of packageName to list of apps frequently launched after it
     */
    fun detectTransitions(launchSequence: List<String>): Map<String, List<String>> {
        if (launchSequence.size < 2) return emptyMap()

        // Count transitions: from -> (to -> count)
        val transitions = mutableMapOf<String, MutableMap<String, Int>>()

        for (i in 0 until launchSequence.size - 1) {
            val from = launchSequence[i]
            val to = launchSequence[i + 1]
            if (from != to) { // Skip self-transitions
                transitions.getOrPut(from) { mutableMapOf() }
                    .merge(to, 1) { old, new -> old + new }
            }
        }

        // Return top successors for each app (those with >= 3 occurrences)
        return transitions.mapValues { (_, successors) ->
            successors.entries
                .filter { it.value >= 3 }
                .sortedByDescending { it.value }
                .take(3)
                .map { it.key }
        }.filter { it.value.isNotEmpty() }
    }

    /**
     * Returns packages that should receive extra scrambling based on predictable patterns.
     * If the user predictably goes A→B→C, B and C should be scrambled more.
     */
    fun getPredictableFollowers(
        launchSequence: List<String>,
        recentlyLaunched: String?
    ): List<String> {
        if (recentlyLaunched == null) return emptyList()
        val transitions = detectTransitions(launchSequence)
        return transitions[recentlyLaunched].orEmpty()
    }

    /**
     * Returns the most frequently launched apps from the sequence.
     */
    fun getFrequentApps(launchSequence: List<String>, topN: Int = 5): List<String> {
        return launchSequence
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(topN)
            .map { it.key }
    }
}
