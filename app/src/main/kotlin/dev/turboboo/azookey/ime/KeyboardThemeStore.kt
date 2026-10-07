package dev.turboboo.azookey.ime

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/*
 * Exposed fields mirror azooKey's ThemeEditView / ThemeData.
 *
 * Upstream:
 *   azooKey/azooKey@b50db4aec1069a8d2341f70415da3bdc61f1fce6
 *   AzooKeyCore/Sources/KeyboardThemes/ThemeData.swift
 *   MainApp/Features/Theme/Editor/ThemeEditView.swift
 * Original implementation author: Keita Miwa (ensan) and azooKey contributors
 *
 * Licensed under the MIT License. See THIRD_PARTY_NOTICES.md.
 */
internal data class KeyboardTheme(
    val backgroundColor: Int,
    val textColor: Int,
    val resultTextColor: Int,
    val resultBackgroundColor: Int,
    val borderColor: Int,
    val borderWidthDp: Float,
    val normalKeyColor: Int,
    val specialKeyColor: Int,
    val pressedKeyColor: Int,
    val fontWeight: Int,
    val backgroundImageUri: String? = null,
) {
    init {
        require(borderWidthDp in 0f..10f)
        require(fontWeight in 1..9)
    }

    fun toPalette(): AzooKeyVisualDesign.Palette =
        AzooKeyVisualDesign.Palette(
            background = backgroundColor,
            normalKey = normalKeyColor,
            specialKey = specialKeyColor,
            pressedKey = pressedKeyColor,
            text = textColor,
            resultBackground = resultBackgroundColor,
            resultText = resultTextColor,
        )

    companion object {
        fun builtIn(context: Context): KeyboardTheme {
            val nightMode =
                context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val palette =
                if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
                    AzooKeyVisualDesign.DARK
                } else {
                    AzooKeyVisualDesign.LIGHT
                }

            return KeyboardTheme(
                backgroundColor = palette.background,
                textColor = palette.text,
                resultTextColor = palette.resultText,
                resultBackgroundColor = palette.resultBackground,
                borderColor = Color.TRANSPARENT,
                borderWidthDp = 1f,
                normalKeyColor = palette.normalKey,
                specialKeyColor = palette.specialKey,
                pressedKeyColor = palette.pressedKey,
                fontWeight = 4,
            )
        }

        fun derivePressedColor(normalColor: Int): Int {
            val hsv = FloatArray(3)
            Color.colorToHSV(normalColor, hsv)
            hsv[2] = if (hsv[2] >= 0.5f) {
                (hsv[2] - 0.1f).coerceAtLeast(0f)
            } else {
                (hsv[2] + 0.1f).coerceAtMost(1f)
            }

            val opacity = Color.alpha(normalColor) / 255f
            val adjustedAlpha =
                (max(0.05f, sqrt(opacity)) * 255f).roundToInt()
                    .coerceIn(0, 255)
            return Color.HSVToColor(
                adjustedAlpha,
                hsv,
            )
        }
    }
}

internal class KeyboardThemeStore(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun load(): KeyboardTheme {
        val json = preferences.getString(KEY_THEME, null)
            ?: return KeyboardTheme.builtIn(appContext)

        return runCatching {
            JSONObject(json).toTheme()
        }.getOrElse {
            KeyboardTheme.builtIn(appContext)
        }
    }

    fun save(theme: KeyboardTheme) {
        preferences.edit()
            .putString(KEY_THEME, theme.toJson().toString())
            .apply()
    }

    fun reset() {
        preferences.edit()
            .remove(KEY_THEME)
            .apply()
    }

    private fun KeyboardTheme.toJson(): JSONObject =
        JSONObject()
            .put("version", 1)
            .put("backgroundColor", backgroundColor)
            .put("textColor", textColor)
            .put("resultTextColor", resultTextColor)
            .put("resultBackgroundColor", resultBackgroundColor)
            .put("borderColor", borderColor)
            .put("borderWidthDp", borderWidthDp.toDouble())
            .put("normalKeyColor", normalKeyColor)
            .put("specialKeyColor", specialKeyColor)
            .put("pressedKeyColor", pressedKeyColor)
            .put("fontWeight", fontWeight)
            .put("backgroundImageUri", backgroundImageUri)

    private fun JSONObject.toTheme(): KeyboardTheme =
        KeyboardTheme(
            backgroundColor = getInt("backgroundColor"),
            textColor = getInt("textColor"),
            resultTextColor = getInt("resultTextColor"),
            resultBackgroundColor = getInt("resultBackgroundColor"),
            borderColor = getInt("borderColor"),
            borderWidthDp = getDouble("borderWidthDp").toFloat(),
            normalKeyColor = getInt("normalKeyColor"),
            specialKeyColor = getInt("specialKeyColor"),
            pressedKeyColor = getInt("pressedKeyColor"),
            fontWeight = getInt("fontWeight"),
            backgroundImageUri =
                if (isNull("backgroundImageUri")) {
                    null
                } else {
                    optString("backgroundImageUri").takeIf(String::isNotBlank)
                },
        )

    companion object {
        internal const val PREFERENCES_NAME = "keyboard_theme"
        private const val KEY_THEME = "theme"
    }
}
