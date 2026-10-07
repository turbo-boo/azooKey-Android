package dev.turboboo.azookey.ime

internal class UserDictionarySynchronizer(
    private val snapshot: () -> String,
    private val apply: (dictionaryPath: String, json: String) -> Boolean,
) {
    private var lastDictionaryPath: String? = null
    private var lastSnapshot: String? = null

    fun sync(dictionaryPath: String): Boolean {
        val currentSnapshot = snapshot()
        if (
            dictionaryPath == lastDictionaryPath &&
            currentSnapshot == lastSnapshot
        ) {
            return true
        }

        if (!apply(dictionaryPath, currentSnapshot)) {
            return false
        }

        lastDictionaryPath = dictionaryPath
        lastSnapshot = currentSnapshot
        return true
    }
}
