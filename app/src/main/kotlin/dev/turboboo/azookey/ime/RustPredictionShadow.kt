package dev.turboboo.azookey.ime

internal fun interface PredictionShadow {
    fun observe(input: String, dictionaryPath: String)
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

internal class RustPredictionShadow(
    private val native: RustPredictionNative = AndroidRustPredictionNative,
) : PredictionShadow {
    override fun observe(
        input: String,
        dictionaryPath: String,
    ) {
        if (input.isEmpty()) {
            return
        }

        runCatching {
            native.prefixWordsJson(input, dictionaryPath)
        }
    }
}
