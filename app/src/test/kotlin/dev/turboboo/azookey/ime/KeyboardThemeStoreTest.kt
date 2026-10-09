package dev.turboboo.azookey.ime

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class KeyboardThemeStoreTest {
    private lateinit var store: KeyboardThemeStore

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences(KeyboardThemeStore.PREFERENCES_NAME, 0)
            .edit()
            .clear()
            .commit()
        store = KeyboardThemeStore(context)
    }

    @Test
    fun customThemeRoundTripsAllExposedUpstreamFields() {
        val theme = KeyboardTheme(
            backgroundColor = 0xFF112233.toInt(),
            textColor = 0xFF223344.toInt(),
            resultTextColor = 0xFF334455.toInt(),
            resultBackgroundColor = 0xFF445566.toInt(),
            borderColor = 0xFF556677.toInt(),
            borderWidthDp = 2.5f,
            normalKeyColor = 0xFF667788.toInt(),
            specialKeyColor = 0xFF778899.toInt(),
            pressedKeyColor = 0xFF8899AA.toInt(),
            fontWeight = 7,
            backgroundImageUri = "content://theme/background",
        )

        store.save(theme)

        assertEquals(theme, KeyboardThemeStore(RuntimeEnvironment.getApplication()).load())
    }

    @Test
    fun resetRestoresBuiltInThemeAndClearsBackgroundImage() {
        store.save(
            store.load().copy(
                backgroundColor = Color.RED,
                backgroundImageUri = "content://theme/background",
            ),
        )

        store.reset()
        val reset = store.load()

        assertEquals(AzooKeyVisualDesign.LIGHT.background, reset.backgroundColor)
        assertNull(reset.backgroundImageUri)
    }

    @Test
    fun pressedColorUsesUpstreamBrightnessRule() {
        assertEquals(
            0xFFE6E6E6.toInt(),
            KeyboardTheme.derivePressedColor(Color.WHITE),
        )
        assertEquals(
            0xFF1A1A1A.toInt(),
            KeyboardTheme.derivePressedColor(Color.BLACK),
        )
    }

    @Test
    fun viewStyleReadsSavedTheme() {
        val context = RuntimeEnvironment.getApplication()
        store.save(
            store.load().copy(
                backgroundColor = 0xFF102030.toInt(),
                normalKeyColor = 0xFF405060.toInt(),
                specialKeyColor = 0xFF708090.toInt(),
            ),
        )

        val palette = AzooKeyViewStyle.palette(context)

        assertEquals(0xFF102030.toInt(), palette.background)
        assertEquals(0xFF405060.toInt(), palette.normalKey)
        assertEquals(0xFF708090.toInt(), palette.specialKey)
    }
}
