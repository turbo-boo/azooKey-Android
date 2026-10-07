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
        )

        assertEquals(
            listOf("あさって" to "/tmp/dictionary"),
            calls,
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
        )
    }
}
