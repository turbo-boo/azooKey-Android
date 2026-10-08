package dev.turboboo.azookey.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class KeyboardHeightStoreTest {
    @Test fun heightScalePersistsAndClampsToSupportedRange() {
        val context = RuntimeEnvironment.getApplication()
        val store = KeyboardHeightStore(context)
        store.setPercent(125)
        assertEquals(125, KeyboardHeightStore(context).percent())
        assertEquals(1.25f, store.scale(), 0.0001f)
        store.setPercent(KeyboardHeightStore.DEFAULT_PERCENT)
    }

    @Test fun invalidKeyboardHeightIsRejected() {
        val store = KeyboardHeightStore(RuntimeEnvironment.getApplication())
        assertThrows(IllegalArgumentException::class.java) { store.setPercent(69) }
        assertThrows(IllegalArgumentException::class.java) { store.setPercent(141) }
    }
}
