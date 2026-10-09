package dev.turboboo.azookey.ime

import android.util.Log

internal fun interface PredictionShadow {
    fun observe(
        input: String,
        dictionaryPath: String,
        swiftPredictions: List<String>,
        predictionPathJson: String,
    )
}

internal fun interface RustPredictionNative {
    fun prefixWordsJson(
        input: String,
        dictionaryPath: String,
        predictionPathJson: String,
    ): String
}

internal object AndroidRustPredictionNative : RustPredictionNative {
    override fun prefixWordsJson(
        input: String,
        dictionaryPath: String,
        predictionPathJson: String,
    ): String =
        if (predictionPathJson != "[]") {
            RustPredictionBridge.prefixWordsFromPathJson(
                predictionPathJson,
                dictionaryPath,
                SHADOW_N_BEST,
            )
        } else {
            RustPredictionBridge.prefixWordsJson(input, dictionaryPath)
        }
}

internal data class PredictionParityMismatch(
    val swiftPredictions: List<String>,
    val rustPredictions: List<String>,
)

internal class RustPredictionShadow(
    private val native: RustPredictionNative = AndroidRustPredictionNative,
    private val onMismatch: (PredictionParityMismatch) -> Unit = {
        Log.w(TAG, "Rust prediction shadow mismatch")
    },
) : PredictionShadow {
    override fun observe(
        input: String,
        dictionaryPath: String,
        swiftPredictions: List<String>,
        predictionPathJson: String,
    ) {
        if (input.isEmpty()) {
            return
        }

        val expected = swiftPredictions.take(SHADOW_N_BEST)
        runCatching {
            parseCandidateJson(
                native.prefixWordsJson(
                    input,
                    dictionaryPath,
                    predictionPathJson,
                ),
            ).take(SHADOW_N_BEST)
        }.onSuccess { rustPredictions ->
            if (rustPredictions != expected) {
                onMismatch(
                    PredictionParityMismatch(
                        swiftPredictions = expected,
                        rustPredictions = rustPredictions,
                    ),
                )
            }
        }
    }

    private companion object {
        const val TAG = "AzooKeyRustPrediction"
    }
}

private const val SHADOW_N_BEST = 3
