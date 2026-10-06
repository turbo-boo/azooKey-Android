package dev.turboboo.azookey.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JapaneseSymbolsFlickLayoutTest {
    @Test
    fun defaultSymbolsMatchAzooKey() {
        assertEquals("、", JapaneseSymbolsFlickLayout.key.output(FlickDirection.CENTER))
        assertEquals("。", JapaneseSymbolsFlickLayout.key.output(FlickDirection.LEFT))
        assertEquals("？", JapaneseSymbolsFlickLayout.key.output(FlickDirection.TOP))
        assertEquals("！", JapaneseSymbolsFlickLayout.key.output(FlickDirection.RIGHT))
        assertNull(JapaneseSymbolsFlickLayout.key.output(FlickDirection.BOTTOM))
    }
}
