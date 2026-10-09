package dev.turboboo.azookey.userdictionary

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal data class UserDictionaryEntry(
    val reading: String,
    val word: String,
)

internal class UserDictionaryStore(
    context: Context,
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun entries(): List<UserDictionaryEntry> =
        decode(preferences.getString(KEY_ENTRIES, "[]") ?: "[]")

    fun json(): String =
        encode(entries())

    fun add(
        reading: String,
        word: String,
    ): Boolean {
        val entry = UserDictionaryEntry(
            reading = normalizeReading(reading.trim()),
            word = word.trim(),
        )
        if (entry.reading.isEmpty() || entry.word.isEmpty()) {
            return false
        }

        val current = entries().toMutableList()
        if (entry in current) {
            return false
        }
        current += entry
        persist(current)
        return true
    }

    fun remove(entry: UserDictionaryEntry): Boolean {
        val normalized = UserDictionaryEntry(
            reading = normalizeReading(entry.reading.trim()),
            word = entry.word.trim(),
        )
        val current = entries().toMutableList()
        val removed = current.remove(normalized)
        if (removed) {
            persist(current)
        }
        return removed
    }

    private fun persist(entries: List<UserDictionaryEntry>) {
        preferences.edit()
            .putString(KEY_ENTRIES, encode(entries))
            .apply()
    }

    private fun encode(entries: List<UserDictionaryEntry>): String =
        JSONArray().apply {
            entries.forEach { entry ->
                put(
                    JSONObject()
                        .put("reading", entry.reading)
                        .put("word", entry.word),
                )
            }
        }.toString()

    private fun decode(json: String): List<UserDictionaryEntry> =
        runCatching {
            val array = JSONArray(json)
            buildList {
                val seen = mutableSetOf<UserDictionaryEntry>()
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val entry = UserDictionaryEntry(
                        reading = normalizeReading(item.optString("reading", "").trim()),
                        word = item.optString("word", "").trim(),
                    )
                    if (
                        entry.reading.isNotEmpty() &&
                        entry.word.isNotEmpty() &&
                        seen.add(entry)
                    ) {
                        add(entry)
                    }
                }
            }
        }.getOrDefault(emptyList())

    companion object {
        internal const val PREFERENCES_NAME = "user_dictionary"
        private const val KEY_ENTRIES = "entries"

        internal fun normalizeReading(reading: String): String =
            buildString(reading.length) {
                reading.forEach { character ->
                    if (character in '\u3041'..'\u3096') {
                        append((character.code + 0x60).toChar())
                    } else {
                        append(character)
                    }
                }
            }
    }
}
