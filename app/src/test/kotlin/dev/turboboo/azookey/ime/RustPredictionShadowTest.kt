package dev.turboboo.azookey.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class RustPredictionShadowTest {
    @Test
    fun observeForwardsInputDictionaryAndPath() {
        val calls = mutableListOf<Triple<String, String, String>>()
        val shadow = RustPredictionShadow(
            native = RustPredictionNative { input, dictionaryPath, predictionPathJson ->
                calls += Triple(input, dictionaryPath, predictionPathJson)
                """["明後日"]"""
            },
        )

        shadow.observe(
            input = "あさって",
            dictionaryPath = "/tmp/dictionary",
            swiftPredictions = listOf("明後日"),
            predictionPathJson = """[{"word":"明日"}]""",
        )

        assertEquals(
            listOf(
                Triple(
                    "あさって",
                    "/tmp/dictionary",
                    """[{"word":"明日"}]""",
                ),
            ),
            calls,
        )
    }

    @Test
    fun matchingPredictionsDoNotReportMismatch() {
        val mismatches = mutableListOf<PredictionParityMismatch>()
        val shadow = RustPredictionShadow(
            native = RustPredictionNative { _, _, _ -> """["明後日","あさって"]""" },
            onMismatch = mismatches::add,
        )

        shadow.observe(
            input = "あさって",
            dictionaryPath = "/tmp/dictionary",
            swiftPredictions = listOf("明後日", "あさって"),
            predictionPathJson = "[]",
        )

        assertEquals(emptyList<PredictionParityMismatch>(), mismatches)
    }

    @Test
    fun rustCandidatesAreComparedEvenWhenSwiftListIsEmpty() {
        val mismatches = mutableListOf<PredictionParityMismatch>()
        val shadow = RustPredictionShadow(
            native = RustPredictionNative { _, _, _ -> """["明後日"]""" },
            onMismatch = mismatches::add,
        )

        shadow.observe(
            input = "あさって",
            dictionaryPath = "/tmp/dictionary",
            swiftPredictions = emptyList(),
            predictionPathJson = "[]",
        )

        assertEquals(
            listOf(
                PredictionParityMismatch(
                    swiftPredictions = emptyList(),
                    rustPredictions = listOf("明後日"),
                ),
            ),
            mismatches,
        )
    }

    @Test
    fun differingPredictionsReportBothSides() {
        val mismatches = mutableListOf<PredictionParityMismatch>()
        val shadow = RustPredictionShadow(
            native = RustPredictionNative { _, _, _ -> """["明後日","明々後日"]""" },
            onMismatch = mismatches::add,
        )

        shadow.observe(
            input = "あさって",
            dictionaryPath = "/tmp/dictionary",
            swiftPredictions = listOf("明後日", "あさって"),
            predictionPathJson = "[]",
        )

        assertEquals(
            listOf(
                PredictionParityMismatch(
                    swiftPredictions = listOf("明後日", "あさって"),
                    rustPredictions = listOf("明後日", "明々後日"),
                ),
            ),
            mismatches,
        )
    }

    @Test
    fun observeSkipsEmptyInput() {
        var callCount = 0
        val shadow = RustPredictionShadow(
            native = RustPredictionNative { _, _, _ ->
                callCount += 1
                "[]"
            },
        )

        shadow.observe(
            input = "",
            dictionaryPath = "/tmp/dictionary",
            swiftPredictions = emptyList(),
            predictionPathJson = "[]",
        )

        assertEquals(0, callCount)
    }

    @Test
    fun nativeFailureDoesNotEscapeShadowBoundary() {
        val shadow = RustPredictionShadow(
            native = RustPredictionNative { _, _, _ ->
                error("native failure")
            },
        )

        shadow.observe(
            input = "あさって",
            dictionaryPath = "/tmp/dictionary",
            swiftPredictions = listOf("明後日"),
            predictionPathJson = "[]",
        )
    }
}
