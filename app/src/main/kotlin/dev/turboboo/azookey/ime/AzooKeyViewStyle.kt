package dev.turboboo.azookey.ime

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.widget.Button

internal object AzooKeyViewStyle {
    fun palette(context: Context): AzooKeyVisualDesign.Palette {
        val nightMode =
            context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
            AzooKeyVisualDesign.DARK
        } else {
            AzooKeyVisualDesign.LIGHT
        }
    }

    fun styleKey(
        button: Button,
        special: Boolean,
        selected: Boolean = false,
        textSizeSp: Float = if (special) 15f else 20f,
    ) {
        val palette = palette(button.context)
        val normalColor = when {
            selected -> palette.pressedKey
            special -> palette.specialKey
            else -> palette.normalKey
        }
        applyButtonBase(button)
        button.textSize = textSizeSp
        button.setTextColor(palette.text)
        button.background = stateBackground(
            normalColor = normalColor,
            pressedColor = palette.pressedKey,
            cornerRadiusPx =
                AzooKeyVisualDesign.KEY_CORNER_RADIUS_DP *
                    button.resources.displayMetrics.density,
        )
    }

    fun styleCandidate(button: Button) {
        val palette = palette(button.context)
        applyButtonBase(button)
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
        )
    }

    private fun applyButtonBase(button: Button) {
        button.isAllCaps = false
        button.stateListAnimator = null
        button.elevation = 0f
        button.minWidth = 0
        button.minimumWidth = 0
        button.minHeight = 0
        button.minimumHeight = 0
        button.includeFontPadding = false
    }

    private fun stateBackground(
        normalColor: Int,
        pressedColor: Int,
        cornerRadiusPx: Float,
    ): StateListDrawable =
        StateListDrawable().apply {
            addState(
                intArrayOf(android.R.attr.state_pressed),
                roundedDrawable(pressedColor, cornerRadiusPx),
            )
            addState(
                intArrayOf(),
                roundedDrawable(normalColor, cornerRadiusPx),
            )
        }

    private fun roundedDrawable(
        color: Int,
        cornerRadiusPx: Float,
    ): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = cornerRadiusPx
        }

    private fun dp(
        context: Context,
        value: Float,
    ): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
