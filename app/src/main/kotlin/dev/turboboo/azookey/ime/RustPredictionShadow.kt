package dev.turboboo.azookey.ime

import android.util.Log

internal fun interface PredictionShadow {
    fun observe(
        input: String,
        dictionaryPath: String,
        swiftPredictions: List<String>,
    )
}

internal fun interface RustPredictionNative {
    fun prefixWordsJson(input: String, dictionaryPath: String): String
}

internal object AndroidRustPredictionNative : RustPredictionNative {
    override fun prefixWordsJson(
        input: String,
        dictionaryPath: String,
    ): String = RustPredictionBridge.prefixWordsJson(input, dictionaryPath)
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
    ) {
        if (input.isEmpty()) {
            return
        }

        runCatching {
            parseCandidateJson(
                native.prefixWordsJson(input, dictionaryPath),
            ).take(swiftPredictions.size)
        }.onSuccess { rustPredictions ->
            if (rustPredictions != swiftPredictions) {
                onMismatch(
                    PredictionParityMismatch(
                        swiftPredictions = swiftPredictions,
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
