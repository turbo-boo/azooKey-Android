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
    fun stableCacheKeepsOnlyCompatibleDirectInputPredictions() {
        val cache = StablePredictionCache()
        cache.update(
            reading = "ア",
            candidates = listOf(
                ScoredCandidate("朝", 5f, ruby = "アサ"),
                ScoredCandidate("明日", 4f, ruby = "アシタ"),
                ScoredCandidate("明後日", 3f, ruby = "アサッテ"),
            ),
        )

        assertEquals(
            listOf(ScoredCandidate("明後日", 3f, ruby = "アサッテ")),
            cache.compatible(reading = "アサ"),
        )
    }

    @Test
    fun stableCacheInvalidatesWhenInputNoLongerExtendsPreviousInput() {
        val cache = StablePredictionCache()
        cache.update(
            reading = "アサ",
            candidates = listOf(
                ScoredCandidate("明後日", 3f, ruby = "アサッテ"),
            ),
        )

        assertEquals(emptyList<ScoredCandidate>(), cache.compatible(reading = "イ"))
        assertEquals(emptyList<ScoredCandidate>(), cache.compatible(reading = "アサッ"))
    }

    @Test
    fun stablePredictionsRemainAheadOfFreshPredictions() {
        val stable = listOf(
            ScoredCandidate("固定1", 1f, ruby = "アサッテ"),
            ScoredCandidate("固定2", 0f, ruby = "アサヒ"),
        )
        val fresh = listOf(
            ScoredCandidate("固定1", 100f, ruby = "アサッテ"),
            ScoredCandidate("新規1", 50f, ruby = "アサマ"),
            ScoredCandidate("新規2", 40f, ruby = "アサリ"),
        )

        assertEquals(
            listOf(
                ScoredCandidate("固定1", 1f, ruby = "アサッテ"),
                ScoredCandidate("固定2", 0f, ruby = "アサヒ"),
                ScoredCandidate("新規1", 50f, ruby = "アサマ"),
            ),
            mergeStablePredictions(stable, fresh, limit = 3),
        )
    }

    @Test
    fun stablePredictionsStayAheadOfScoreMergedTopFive() {
        val stable = listOf(
            ScoredCandidate("固定予測", -100f, ruby = "アサッテ"),
        )
        val predictions = stable + ScoredCandidate("新規予測", 8f, ruby = "アサヒ")

        val result = mergeHybridCandidates(
            swiftCandidates = listOf(
                ScoredCandidate("変換1", 10f, exactRuby = true),
                ScoredCandidate("変換2", 9f),
                ScoredCandidate("変換3", 7f),
                ScoredCandidate("変換4", 6f),
                ScoredCandidate("変換5", 5f),
            ),
            rustPredictions = predictions,
            stablePredictions = stable,
        )

        assertEquals(
            listOf("固定予測", "変換1", "変換2", "新規予測", "変換3"),
            result,
        )
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
