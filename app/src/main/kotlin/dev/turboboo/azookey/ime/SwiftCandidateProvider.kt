package dev.turboboo.azookey.ime

import android.content.Context
import android.content.res.AssetManager
import dev.turboboo.azookey.converter.AzooKeyAndroidJNI
import java.io.File
import org.json.JSONArray

private const val DICTIONARY_ASSET_ROOT = "azookey_dictionary"
private const val DICTIONARY_REVISION = "4d418525b090cf49c219819d05a7e3cc2a4346eb"

class SwiftCandidateProvider(
    context: Context,
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
        val candidates = parseCandidateJson(
            AzooKeyAndroidJNI.candidatesJSON(
                input,
                dictionaryPath,
            ),
        )
        predictionShadow?.observe(input, dictionaryPath)
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
        val array = JSONArray(json)
        buildList {
            val seen = mutableSetOf<String>()
            for (index in 0 until array.length()) {
                val candidate = array.optString(index, "")
                if (candidate.isNotBlank() && seen.add(candidate)) {
                    add(candidate)
                }
            }
        }
    }.getOrDefault(emptyList())
