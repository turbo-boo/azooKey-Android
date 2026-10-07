package dev.turboboo.azookey.ime

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.widget.Button

internal object AzooKeyViewStyle {
    fun theme(context: Context): KeyboardTheme =
        KeyboardThemeStore(context).load()

    fun palette(context: Context): AzooKeyVisualDesign.Palette =
        theme(context).toPalette()

    fun styleKey(
        button: Button,
        special: Boolean,
        selected: Boolean = false,
        textSizeSp: Float = if (special) 15f else 20f,
    ) {
        val theme = theme(button.context)
        val palette = theme.toPalette()
        val normalColor = when {
            selected -> palette.pressedKey
            special -> palette.specialKey
            else -> palette.normalKey
        }
        applyButtonBase(
            button = button,
            fontWeight = theme.fontWeight,
        )
        button.textSize = textSizeSp
        button.setTextColor(palette.text)
        button.background = stateBackground(
            normalColor = normalColor,
            pressedColor = palette.pressedKey,
            cornerRadiusPx =
                AzooKeyVisualDesign.KEY_CORNER_RADIUS_DP *
                    button.resources.displayMetrics.density,
            borderColor = theme.borderColor,
            borderWidthPx =
                theme.borderWidthDp *
                    button.resources.displayMetrics.density,
        )
    }

    fun styleCandidate(button: Button) {
        val theme = theme(button.context)
        val palette = theme.toPalette()
        applyButtonBase(
            button = button,
            fontWeight = theme.fontWeight,
        )
        button.textSize = AzooKeyVisualDesign.CANDIDATE_TEXT_SIZE_SP
        button.setTextColor(palette.resultText)
        button.setPadding(
            dp(button.context, 5f),
            dp(button.context, 5f),
            dp(button.context, 5f),
            dp(button.context, 5f),
        )
        button.background = stateBackground(
            normalColor = palette.resultBackground,
            pressedColor = palette.pressedKey,
            cornerRadiusPx =
                AzooKeyVisualDesign.CANDIDATE_CORNER_RADIUS_DP *
                    button.resources.displayMetrics.density,
            borderColor = android.graphics.Color.TRANSPARENT,
            borderWidthPx = 0f,
        )
    }

    private fun applyButtonBase(
        button: Button,
        fontWeight: Int,
    ) {
        button.isAllCaps = false
        button.stateListAnimator = null
        button.elevation = 0f
        button.minWidth = 0
        button.minimumWidth = 0
        button.minHeight = 0
        button.minimumHeight = 0
        button.includeFontPadding = false
        button.typeface = Typeface.create(
            "sans-serif",
            fontWeight * 100,
            false,
        )
    }

    private fun stateBackground(
        normalColor: Int,
        pressedColor: Int,
        cornerRadiusPx: Float,
        borderColor: Int,
        borderWidthPx: Float,
    ): StateListDrawable =
        StateListDrawable().apply {
            addState(
                intArrayOf(android.R.attr.state_pressed),
                roundedDrawable(
                    color = pressedColor,
                    cornerRadiusPx = cornerRadiusPx,
                    borderColor = borderColor,
                    borderWidthPx = borderWidthPx,
                ),
            )
            addState(
                intArrayOf(),
                roundedDrawable(
                    color = normalColor,
                    cornerRadiusPx = cornerRadiusPx,
                    borderColor = borderColor,
                    borderWidthPx = borderWidthPx,
                ),
            )
        }

    private fun roundedDrawable(
        color: Int,
        cornerRadiusPx: Float,
        borderColor: Int,
        borderWidthPx: Float,
    ): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = cornerRadiusPx
            if (borderWidthPx > 0f) {
                setStroke(
                    borderWidthPx.toInt().coerceAtLeast(1),
                    borderColor,
                )
            }
        }

    private fun dp(
        context: Context,
        value: Float,
    ): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
