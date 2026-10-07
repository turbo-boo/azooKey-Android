package dev.turboboo.azookey.ime

/*
 * Visual sizing and classic theme values are ported from azooKey.
 *
 * Upstream:
 *   azooKey/azooKey@b50db4aec1069a8d2341f70415da3bdc61f1fce6
 *   AzooKeyCore/Sources/KeyboardViews/Design.swift
 *   AzooKeyCore/Sources/KeyboardViews/View/Components/KeyBackground.swift
 *   AzooKeyCore/Sources/KeyboardViews/View/KeyboardBar/ResultBar.swift
 *   Resources/Designs.xcassets/*.colorset/Contents.json
 * Original implementation author: Keita Miwa (ensan) and azooKey contributors
 *
 * Licensed under the MIT License. See THIRD_PARTY_NOTICES.md.
 */
internal object AzooKeyVisualDesign {
    const val KEY_CORNER_RADIUS_DP = 6f
    const val CANDIDATE_CORNER_RADIUS_DP = 5f
    const val CANDIDATE_TEXT_SIZE_SP = 18f
    const val CANDIDATE_SPACING_DP = 10f
    const val ALL_FLICK_SUGGEST_DELAY_MS = 500L
    const val KEYBOARD_VERTICAL_PADDING_DP = 12f

    data class Palette(
        val background: Int,
        val normalKey: Int,
        val specialKey: Int,
        val pressedKey: Int,
        val text: Int,
        val resultBackground: Int,
        val resultText: Int,
    )

    data class Metrics(
        val keyboardHeightPx: Float,
        val keyboardBarHeightPx: Float,
        val candidateButtonHeightPx: Float,
        val keyWidthPx: Float,
        val keyHeightPx: Float,
        val horizontalSpacingPx: Float,
        val verticalSpacingPx: Float,
        val horizontalInsetPx: Float,
    )

    val LIGHT = Palette(
        background = 0xFFD1D2D8.toInt(),
        normalKey = 0xFFFFFFFF.toInt(),
        specialKey = 0xFFC6C8CF.toInt(),
        pressedKey = 0xFFEDEEF2.toInt(),
        text = 0xFF000000.toInt(),
        resultBackground = 0xFFD1D2D8.toInt(),
        resultText = 0xFF000000.toInt(),
    )

    val DARK = Palette(
        background = 0xFF000000.toInt(),
        normalKey = 0xFF1E1E1E.toInt(),
        specialKey = 0xFF1C1C1E.toInt(),
        pressedKey = 0xFF2B2C2E.toInt(),
        text = 0xFFFFFFFF.toInt(),
        resultBackground = 0xFF000000.toInt(),
        resultText = 0xFFFFFFFF.toInt(),
    )

    fun phonePortrait(
        widthPx: Float,
        density: Float,
    ): Metrics {
        require(widthPx > 0f)
        require(density > 0f)

        val fixedPadding = KEYBOARD_VERTICAL_PADDING_DP * density
        val keyboardHeight = (51f / 74f) * widthPx + fixedPadding
        val keyboardBarHeight = (keyboardHeight - fixedPadding) * 37f / 204f
        val candidateButtonHeight = keyboardBarHeight * 0.6f

        val horizontalKeyCount = 5f
        val verticalKeyCount = 4f
        val widthCoefficient = 5f / (5.1f + horizontalKeyCount / 10f)
        val keyWidth = widthPx / horizontalKeyCount * widthCoefficient

        val spacingCoefficient =
            (5f + horizontalKeyCount) / (7.5f + horizontalKeyCount)
        val horizontalSpacing =
            (widthPx - keyWidth * horizontalKeyCount) /
                (horizontalKeyCount - 1f) *
                spacingCoefficient

        val verticalSpacing = widthPx / 50f
        val keysHeight = keyboardHeight - (keyboardBarHeight + fixedPadding)
        val keyHeight =
            (keysHeight - (verticalKeyCount - 1f) * verticalSpacing) /
                verticalKeyCount

        val keysWidth =
            keyWidth * horizontalKeyCount +
                horizontalSpacing * (horizontalKeyCount - 1f)
        val horizontalInset = (widthPx - keysWidth) / 2f

        return Metrics(
            keyboardHeightPx = keyboardHeight,
            keyboardBarHeightPx = keyboardBarHeight,
            candidateButtonHeightPx = candidateButtonHeight,
            keyWidthPx = keyWidth,
            keyHeightPx = keyHeight,
            horizontalSpacingPx = horizontalSpacing,
            verticalSpacingPx = verticalSpacing,
            horizontalInsetPx = horizontalInset,
        )
    }
}
