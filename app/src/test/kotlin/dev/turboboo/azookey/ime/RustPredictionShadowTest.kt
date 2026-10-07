package dev.turboboo.azookey.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class RustPredictionShadowTest {
    @Test
    fun observeForwardsInputAndDictionaryPath() {
        val calls = mutableListOf<Pair<String, String>>()
        val shadow = RustPredictionShadow(
            native = RustPredictionNative { input, dictionaryPath ->
                calls += input to dictionaryPath
                """["明後日"]"""
            },
        )

        shadow.observe(
            input = "あさって",
            dictionaryPath = "/tmp/dictionary",
            swiftPredictions = listOf("明後日"),
        )

        assertEquals(
            listOf("あさって" to "/tmp/dictionary"),
            calls,
        )
    }

    @Test
    fun matchingPredictionsDoNotReportMismatch() {
        val mismatches = mutableListOf<PredictionParityMismatch>()
        val shadow = RustPredictionShadow(
            native = RustPredictionNative { _, _ -> """["明後日","あさって"]""" },
            onMismatch = mismatches::add,
        )

        shadow.observe(
            input = "あさって",
            dictionaryPath = "/tmp/dictionary",
            swiftPredictions = listOf("明後日", "あさって"),
        )

        assertEquals(emptyList<PredictionParityMismatch>(), mismatches)
    }

    @Test
    fun differingPredictionsReportBothSides() {
        val mismatches = mutableListOf<PredictionParityMismatch>()
        val shadow = RustPredictionShadow(
            native = RustPredictionNative { _, _ -> """["明後日","明々後日"]""" },
            onMismatch = mismatches::add,
        )

        shadow.observe(
            input = "あさって",
            dictionaryPath = "/tmp/dictionary",
            swiftPredictions = listOf("明後日", "あさって"),
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
            native = RustPredictionNative { _, _ ->
                callCount += 1
                "[]"
            },
        )

        shadow.observe(
            input = "",
            dictionaryPath = "/tmp/dictionary",
            swiftPredictions = emptyList(),
        )

        assertEquals(0, callCount)
    }

    @Test
    fun nativeFailureDoesNotEscapeShadowBoundary() {
        val shadow = RustPredictionShadow(
            native = RustPredictionNative { _, _ ->
                error("native failure")
            },
        )

        shadow.observe(
            input = "あさって",
            dictionaryPath = "/tmp/dictionary",
            swiftPredictions = listOf("明後日"),
        )
    }
}
