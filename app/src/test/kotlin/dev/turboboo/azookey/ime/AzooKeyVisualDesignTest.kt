package dev.turboboo.azookey.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class AzooKeyVisualDesignTest {
    @Test
    fun phonePortraitMetricsMatchUpstreamDesignFormula() {
        val metrics = AzooKeyVisualDesign.phonePortrait(
            widthPx = 370f,
            density = 1f,
        )

        assertEquals(267f, metrics.keyboardHeightPx, 0.01f)
        assertEquals(46.25f, metrics.keyboardBarHeightPx, 0.01f)
        assertEquals(27.75f, metrics.candidateButtonHeightPx, 0.01f)
        assertEquals(66.0714f, metrics.keyWidthPx, 0.01f)
        assertEquals(7.9286f, metrics.horizontalSpacingPx, 0.01f)
        assertEquals(7.4f, metrics.verticalSpacingPx, 0.01f)
        assertEquals(46.6375f, metrics.keyHeightPx, 0.01f)
        assertEquals(3.9643f, metrics.horizontalInsetPx, 0.01f)
    }

    @Test
    fun keyAndCandidateShapeConstantsMatchUpstream() {
        assertEquals(6f, AzooKeyVisualDesign.KEY_CORNER_RADIUS_DP, 0f)
        assertEquals(5f, AzooKeyVisualDesign.CANDIDATE_CORNER_RADIUS_DP, 0f)
        assertEquals(18f, AzooKeyVisualDesign.CANDIDATE_TEXT_SIZE_SP, 0f)
        assertEquals(10f, AzooKeyVisualDesign.CANDIDATE_SPACING_DP, 0f)
        assertEquals(500L, AzooKeyVisualDesign.ALL_FLICK_SUGGEST_DELAY_MS)
    }

    @Test
    fun classicPaletteMatchesUpstreamAssets() {
        assertEquals(0xFFD1D2D8.toInt(), AzooKeyVisualDesign.LIGHT.background)
        assertEquals(0xFFFFFFFF.toInt(), AzooKeyVisualDesign.LIGHT.normalKey)
        assertEquals(0xFFC6C8CF.toInt(), AzooKeyVisualDesign.LIGHT.specialKey)
        assertEquals(0xFFEDEEF2.toInt(), AzooKeyVisualDesign.LIGHT.pressedKey)
        assertEquals(0xFF000000.toInt(), AzooKeyVisualDesign.LIGHT.text)

        assertEquals(0xFF000000.toInt(), AzooKeyVisualDesign.DARK.background)
        assertEquals(0xFF1E1E1E.toInt(), AzooKeyVisualDesign.DARK.normalKey)
        assertEquals(0xFF1C1C1E.toInt(), AzooKeyVisualDesign.DARK.specialKey)
        assertEquals(0xFF2B2C2E.toInt(), AzooKeyVisualDesign.DARK.pressedKey)
        assertEquals(0xFFFFFFFF.toInt(), AzooKeyVisualDesign.DARK.text)
    }
}
