package dev.turboboo.azookey.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class HybridCandidateMergeTest {
    @Test
    fun rustPredictionsMixIntoSwiftTopFiveByScore() {
        val result = mergeHybridCandidates(
            swiftCandidates = listOf(
                ScoredCandidate("変換1", 10f),
                ScoredCandidate("変換2", 8f, exactRuby = true),
                ScoredCandidate("変換3", 6f),
                ScoredCandidate("変換4", 4f),
                ScoredCandidate("変換5", 2f),
                ScoredCandidate("文節候補", 100f),
            ),
            rustPredictions = listOf(
                ScoredCandidate("予測1", 9f),
                ScoredCandidate("予測2", 7f),
                ScoredCandidate("予測3", 1f),
            ),
        )

        assertEquals(
            listOf("変換1", "予測1", "変換2", "予測2", "変換3", "文節候補"),
            result,
        )
    }

    @Test
    fun exactRubyCandidateIsKeptWithinTopThree() {
        val result = mergeHybridCandidates(
            swiftCandidates = listOf(
                ScoredCandidate("変換1", 10f),
                ScoredCandidate("変換2", 9f),
                ScoredCandidate("かな", 1f, exactRuby = true),
                ScoredCandidate("変換4", 0f),
            ),
            rustPredictions = listOf(
                ScoredCandidate("予測1", 8f),
                ScoredCandidate("予測2", 7f),
                ScoredCandidate("予測3", 6f),
            ),
        )

        assertEquals("かな", result[2])
    }

    @Test
    fun duplicateTextKeepsHigherScoredCandidate() {
        val result = mergeHybridCandidates(
            swiftCandidates = listOf(
                ScoredCandidate("同じ", 3f),
                ScoredCandidate("別", 2f, exactRuby = true),
            ),
            rustPredictions = listOf(
                ScoredCandidate("同じ", 5f),
            ),
        )

        assertEquals(listOf("同じ", "別"), result)
    }

    @Test
    fun resultIsUniqueAndLimited() {
        val swift = (1..10).map {
            ScoredCandidate("変換$it", (20 - it).toFloat(), exactRuby = it == 2)
        }
        val rust = listOf(
            ScoredCandidate("予測1", 30f),
            ScoredCandidate("予測2", 29f),
            ScoredCandidate("予測3", 28f),
        )

        val result = mergeHybridCandidates(swift, rust, limit = 10)

        assertEquals(result.distinct(), result)
        assertEquals(10, result.size)
    }
}
