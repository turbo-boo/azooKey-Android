package dev.turboboo.azookey.ime

import android.content.Context

/**
 * User-adjustable flick threshold; default matches the original Android port.
 * Upstream: azooKey/azooKey MainApp/Features/Settings/Keyboard/Interaction/FlickSensitivitySettingView.swift
 * The preference storage and threshold conversion are Android-specific.
 */
internal class FlickSensitivityStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "flick_sensitivity",
        Context.MODE_PRIVATE,
    )

    fun percent(): Int = preferences.getInt("percent", DEFAULT_PERCENT).coerceIn(MIN_PERCENT, MAX_PERCENT)

    fun setPercent(percent: Int) {
        require(percent in MIN_PERCENT..MAX_PERCENT)
        preferences.edit().putInt("percent", percent).apply()
    }

    fun thresholdPx(density: Float): Float {
        require(density > 0f)
        return DEFAULT_THRESHOLD_DP * density * DEFAULT_PERCENT / percent()
    }

    companion object {
        const val MIN_PERCENT = 50
        const val MAX_PERCENT = 200
        const val DEFAULT_PERCENT = 100
        private const val DEFAULT_THRESHOLD_DP = 25f
    }
}
