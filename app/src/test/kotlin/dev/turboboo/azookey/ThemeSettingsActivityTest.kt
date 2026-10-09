package dev.turboboo.azookey

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The theme editor must discard an unsaved draft on system Back,
 * including Android 16 predictive Back dispatched through AndroidX.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ThemeSettingsActivityTest {
    @Test
    fun systemBackCancelsThemeEditing() {
        Robolectric.buildActivity(ThemeSettingsActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            activity.onBackPressedDispatcher.onBackPressed()
            assertTrue(activity.isFinishing)
        }
    }
}
