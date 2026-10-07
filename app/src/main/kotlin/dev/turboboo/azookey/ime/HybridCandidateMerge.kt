package dev.turboboo.azookey.ime

internal data class ScoredCandidate(
    val text: String,
    val value: Float,
    val exactRuby: Boolean = false,
)

internal fun mergeHybridCandidates(
    swiftCandidates: List<ScoredCandidate>,
    rustPredictions: List<ScoredCandidate>,
    limit: Int = 10,
): List<String> {
    if (limit <= 0) {
        return emptyList()
    }

    val uniqueTop = linkedMapOf<String, ScoredCandidate>()
    (swiftCandidates.take(5) + rustPredictions).forEach { candidate ->
        if (candidate.text.isBlank()) {
            return@forEach
        }

        val current = uniqueTop[candidate.text]
        if (current == null || current.value < candidate.value) {
            uniqueTop[candidate.text] = candidate
        }
    }

    val mixedTop = uniqueTop.values
        .sortedByDescending(ScoredCandidate::value)
        .take(5)
        .toMutableList()

    if (mixedTop.take(3).none(ScoredCandidate::exactRuby)) {
        val exactCandidate = swiftCandidates.firstOrNull(ScoredCandidate::exactRuby)
        if (exactCandidate != null) {
            mixedTop.removeAll { it.text == exactCandidate.text }
            mixedTop.add(
                index = minOf(2, mixedTop.size),
                element = exactCandidate,
            )
            while (mixedTop.size > 5) {
                mixedTop.removeAt(mixedTop.lastIndex)
            }
        }
    }

    val result = mutableListOf<String>()
    val seen = mutableSetOf<String>()

    fun append(text: String) {
        if (text.isNotBlank() && seen.add(text) && result.size < limit) {
            result += text
        }
    }

    mixedTop.forEach { append(it.text) }
    swiftCandidates.drop(5).forEach { append(it.text) }

    return result
}
