package dev.turboboo.azookey.ime

import android.widget.Button
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AzooKeyViewStyleTest {
    @Test
    fun keyTypefaceUsesConfiguredFontWeight() {
        val context = RuntimeEnvironment.getApplication()
        val store = KeyboardThemeStore(context)
        val original = store.load()
        try {
            store.save(original.copy(fontWeight = 7))
            val key = Button(context)
            AzooKeyViewStyle.styleKey(key, special = false)
            assertEquals(700, key.typeface.weight)

            val candidate = Button(context)
            AzooKeyViewStyle.styleCandidate(candidate)
            assertEquals(700, candidate.typeface.weight)
        } finally {
            store.save(original)
        }
    }
}
