package dev.turboboo.azookey.ime

internal data class ScoredCandidate(
    val text: String,
    val value: Float,
    val ruby: String = "",
    val exactRuby: Boolean = false,
)

internal class StablePredictionCache {
    private data class Entry(
        val reading: String,
        val candidates: List<ScoredCandidate>,
    )

    private var entry: Entry? = null

    fun compatible(reading: String): List<ScoredCandidate> {
        val cached = entry ?: return emptyList()
        if (!reading.startsWith(cached.reading)) {
            clear()
            return emptyList()
        }

        val compatible = cached.candidates.filter { candidate ->
            candidate.ruby.isNotEmpty() &&
                candidate.ruby != reading &&
                candidate.ruby.startsWith(reading)
        }
        if (compatible.isEmpty()) {
            clear()
        }
        return compatible
    }

    fun update(
        reading: String,
        candidates: List<ScoredCandidate>,
    ) {
        entry = if (candidates.isEmpty()) {
            null
        } else {
            Entry(
                reading = reading,
                candidates = candidates,
            )
        }
    }

    fun clear() {
        entry = null
    }
}

internal fun mergeStablePredictions(
    stablePredictions: List<ScoredCandidate>,
    freshPredictions: List<ScoredCandidate>,
    limit: Int,
): List<ScoredCandidate> {
    if (limit <= 0) {
        return emptyList()
    }

    val stable = uniqueScoredCandidates(stablePredictions)
    if (stable.size >= limit) {
        return stable.take(limit)
    }

    val stableTexts = stable.mapTo(mutableSetOf(), ScoredCandidate::text)
    val fresh = uniqueScoredCandidates(freshPredictions)
        .filterNot { it.text in stableTexts }
        .sortedByDescending(ScoredCandidate::value)
        .take(limit - stable.size)

    return stable + fresh
}

internal fun mergeHybridCandidates(
    swiftCandidates: List<ScoredCandidate>,
    rustPredictions: List<ScoredCandidate>,
    stablePredictions: List<ScoredCandidate> = emptyList(),
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

    val scoreMergedTop = uniqueTop.values
        .sortedByDescending(ScoredCandidate::value)

    val stable = uniqueScoredCandidates(stablePredictions)
    val stableTexts = stable.mapTo(mutableSetOf(), ScoredCandidate::text)
    val mixedTop = (
        stable +
            scoreMergedTop.filterNot { it.text in stableTexts }
        )
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

internal fun dictionaryReading(input: String): String = buildString(input.length) {
    input.forEach { character ->
        if (character in '\u3041'..'\u3096') {
            append((character.code + 0x60).toChar())
        } else {
            append(character)
        }
    }
}

private fun uniqueScoredCandidates(
    candidates: List<ScoredCandidate>,
): List<ScoredCandidate> {
    val result = mutableListOf<ScoredCandidate>()
    val indices = mutableMapOf<String, Int>()

    candidates.forEach { candidate ->
        if (candidate.text.isBlank()) {
            return@forEach
        }

        val index = indices[candidate.text]
        if (index == null) {
            indices[candidate.text] = result.size
            result += candidate
        } else if (result[index].value < candidate.value) {
            result[index] = candidate
        }
    }

    return result
}
