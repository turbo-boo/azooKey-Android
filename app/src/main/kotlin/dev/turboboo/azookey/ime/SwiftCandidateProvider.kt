package dev.turboboo.azookey.ime

import android.content.Context
import android.content.res.AssetManager
import dev.turboboo.azookey.converter.AzooKeyAndroidJNI
import java.io.File

private const val DICTIONARY_ASSET_ROOT = "azookey_dictionary"
private const val DICTIONARY_REVISION = "4d418525b090cf49c219819d05a7e3cc2a4346eb"

internal fun interface ScoredPredictionEngine {
    fun predictions(
        pathJson: String,
        dictionaryPath: String,
    ): List<ScoredCandidate>
}

internal object AndroidRustScoredPredictionEngine : ScoredPredictionEngine {
    override fun predictions(
        pathJson: String,
        dictionaryPath: String,
    ): List<ScoredCandidate> {
        if (pathJson == "[]") {
            return emptyList()
        }
        return parseScoredPredictionJson(
            RustPredictionBridge.prefixScoredFromPathJson(
                pathJson,
                dictionaryPath,
                3,
            ),
        )
    }
}

internal class SwiftCandidateProvider(
    context: Context,
    private val predictionEngine: ScoredPredictionEngine = AndroidRustScoredPredictionEngine,
    private val predictionShadow: PredictionShadow? = null,
) : CandidateProvider {
    private val appContext = context.applicationContext
    private val dictionaryDirectory: File by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        installDictionary()
    }

    override fun candidates(input: String): List<String> {
        if (input.isEmpty()) {
            return emptyList()
        }

        val dictionaryPath = dictionaryDirectory.absolutePath
        val bridge = parseConversionBridgeJson(
            AzooKeyAndroidJNI.conversionBridgeJSON(
                input,
                dictionaryPath,
            ),
        )

        val rustPredictions = runCatching {
            predictionEngine.predictions(
                pathJson = bridge.pathJson,
                dictionaryPath = dictionaryPath,
            )
        }.getOrDefault(emptyList())

        val candidates = if (bridge.candidates.isNotEmpty()) {
            mergeHybridCandidates(
                swiftCandidates = bridge.candidates,
                rustPredictions = rustPredictions,
            )
        } else {
            // Fail safe for a bridge-format or native-loading regression.
            parseCandidateJson(
                AzooKeyAndroidJNI.candidatesJSON(
                    input,
                    dictionaryPath,
                ),
            )
        }

        predictionShadow?.let { shadow ->
            val diagnostics = parsePredictionShadowJson(
                AzooKeyAndroidJNI.predictionShadowJSON(
                    input,
                    dictionaryPath,
                ),
            )
            shadow.observe(
                input = input,
                dictionaryPath = dictionaryPath,
                swiftPredictions = diagnostics.predictions,
                predictionPathJson = diagnostics.pathJson,
            )
        }
        return candidates
    }

    private fun installDictionary(): File = synchronized(dictionaryInstallLock) {
        val root = File(
            appContext.noBackupFilesDir,
            "azookey-converter/dictionary-$DICTIONARY_REVISION",
        )
        val marker = File(root, ".complete")

        if (marker.isFile && marker.readText() == DICTIONARY_REVISION) {
            return@synchronized root
        }

        root.deleteRecursively()
        check(root.mkdirs()) {
            "Failed to create converter dictionary directory: $root"
        }

        try {
            copyAssetTree(
                assets = appContext.assets,
                assetPath = DICTIONARY_ASSET_ROOT,
                destination = root,
            )
            marker.writeText(DICTIONARY_REVISION)
        } catch (throwable: Throwable) {
            root.deleteRecursively()
            throw throwable
        }

        root
    }

    private fun copyAssetTree(
        assets: AssetManager,
        assetPath: String,
        destination: File,
    ) {
        val children = assets.list(assetPath) ?: emptyArray()

        if (children.isEmpty()) {
            destination.parentFile?.mkdirs()
            assets.open(assetPath, AssetManager.ACCESS_STREAMING).use { input ->
                destination.outputStream().buffered().use { output ->
                    input.copyTo(output)
                }
            }
            return
        }

        check(destination.mkdirs() || destination.isDirectory) {
            "Failed to create converter asset directory: $destination"
        }

        children.forEach { child ->
            copyAssetTree(
                assets = assets,
                assetPath = "$assetPath/$child",
                destination = File(destination, child),
            )
        }
    }

    private companion object {
        val dictionaryInstallLock = Any()
    }
}

internal fun parseCandidateJson(json: String): List<String> =
    runCatching {
        JsonStringArrayParser(json).parse()
            .filter(String::isNotBlank)
            .distinct()
    }.getOrDefault(emptyList())

private class JsonStringArrayParser(
    private val source: String,
) {
    private var index = 0

    fun parse(): List<String> {
        skipWhitespace()
        expect('[')
        skipWhitespace()
        if (peek() == ']') {
            index += 1
            finish()
            return emptyList()
        }

        val result = mutableListOf<String>()
        while (true) {
            skipWhitespace()
            result += parseString()
            skipWhitespace()

            when (peek()) {
                ',' -> index += 1
                ']' -> {
                    index += 1
                    finish()
                    return result
                }
                else -> error("Expected ',' or ']' in JSON string array")
            }
        }
    }

    private fun parseString(): String {
        expect('"')
        val result = StringBuilder()
        while (index < source.length) {
            val character = source[index++]
            when (character) {
                '"' -> return result.toString()
                '\\' -> result.append(parseEscape())
                else -> {
                    require(character >= ' ') {
                        "Unescaped control character in JSON string"
                    }
                    result.append(character)
                }
            }
        }
        error("Unterminated JSON string")
    }

    private fun parseEscape(): Char {
        require(index < source.length) {
            "Unterminated JSON escape"
        }
        return when (val escaped = source[index++]) {
            '"' -> '"'
            '\\' -> '\\'
            '/' -> '/'
            'b' -> '\b'
            'f' -> '\u000C'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'u' -> parseUnicodeEscape()
            else -> error("Unsupported JSON escape: \\$escaped")
        }
    }

    private fun parseUnicodeEscape(): Char {
        require(index + 4 <= source.length) {
            "Truncated JSON unicode escape"
        }
        val value = source.substring(index, index + 4).toInt(16)
        index += 4
        return value.toChar()
    }

    private fun expect(expected: Char) {
        require(peek() == expected) {
            "Expected '$expected' in JSON string array"
        }
        index += 1
    }

    private fun peek(): Char? = source.getOrNull(index)

    private fun skipWhitespace() {
        while (source.getOrNull(index)?.isWhitespace() == true) {
            index += 1
        }
    }

    private fun finish() {
        skipWhitespace()
        require(index == source.length) {
            "Trailing data after JSON string array"
        }
    }
}


internal data class ConversionBridgeData(
    val candidates: List<ScoredCandidate>,
    val pathJson: String,
)

internal fun parseConversionBridgeJson(json: String): ConversionBridgeData =
    runCatching {
        val root = org.json.JSONObject(json)
        val candidatesJson = root.optJSONArray("candidates")
        val candidates = buildList {
            if (candidatesJson != null) {
                for (index in 0 until candidatesJson.length()) {
                    val item = candidatesJson.optJSONObject(index) ?: continue
                    val text = item.optString("text", "")
                    if (text.isBlank()) {
                        continue
                    }
                    add(
                        ScoredCandidate(
                            text = text,
                            value = item.optDouble("value", Double.NEGATIVE_INFINITY).toFloat(),
                            exactRuby = item.optBoolean("exactRuby", false),
                        ),
                    )
                }
            }
        }

        ConversionBridgeData(
            candidates = candidates,
            pathJson = root.optJSONArray("path")?.toString() ?: "[]",
        )
    }.getOrDefault(
        ConversionBridgeData(
            candidates = emptyList(),
            pathJson = "[]",
        ),
    )

internal fun parseScoredPredictionJson(json: String): List<ScoredCandidate> =
    runCatching {
        val array = org.json.JSONArray(json)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val text = item.optString("word", "")
                if (text.isBlank()) {
                    continue
                }
                add(
                    ScoredCandidate(
                        text = text,
                        value = item.optDouble("score", Double.NEGATIVE_INFINITY).toFloat(),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

internal data class PredictionShadowDiagnostics(
    val predictions: List<String>,
    val pathJson: String,
)

internal fun parsePredictionShadowJson(json: String): PredictionShadowDiagnostics =
    runCatching {
        val root = org.json.JSONObject(json)
        PredictionShadowDiagnostics(
            predictions = parseCandidateJson(
                root.optJSONArray("predictions")?.toString() ?: "[]",
            ),
            pathJson = root.optJSONArray("path")?.toString() ?: "[]",
        )
    }.getOrDefault(
        PredictionShadowDiagnostics(
            predictions = emptyList(),
            pathJson = "[]",
        ),
    )
