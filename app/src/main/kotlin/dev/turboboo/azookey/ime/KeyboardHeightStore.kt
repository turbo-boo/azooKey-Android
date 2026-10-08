package dev.turboboo.azookey.ime

import android.content.Context

/**
 * Multiplier-based keyboard height, following the upstream setting's intent.
 * See azooKey/azooKey docs/settings.md. Android persistence is original.
 */
internal class KeyboardHeightStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "keyboard_height",
        Context.MODE_PRIVATE,
    )

    fun percent(): Int =
        preferences.getInt("percent", DEFAULT_PERCENT).coerceIn(MIN_PERCENT, MAX_PERCENT)

    fun setPercent(percent: Int) {
        require(percent in MIN_PERCENT..MAX_PERCENT)
        preferences.edit().putInt("percent", percent).apply()
    }

    fun scale(): Float = percent() / 100f

    companion object {
        const val MIN_PERCENT = 70
        const val MAX_PERCENT = 140
        const val DEFAULT_PERCENT = 100
    }
}
