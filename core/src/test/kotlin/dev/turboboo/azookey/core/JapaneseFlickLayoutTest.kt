package dev.turboboo.azookey.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class JapaneseFlickLayoutTest {
    @Test
    fun aRowMatchesAzooKeyLayout() {
        val key = JapaneseFlickLayout.key("あ")
        assertNotNull(key)
        key!!
        assertEquals("あ", key.output(FlickDirection.CENTER))
        assertEquals("い", key.output(FlickDirection.LEFT))
        assertEquals("う", key.output(FlickDirection.TOP))
        assertEquals("え", key.output(FlickDirection.RIGHT))
        assertEquals("お", key.output(FlickDirection.BOTTOM))
    }

    @Test
    fun yaRowKeepsJapaneseBracketsOnHorizontalFlicks() {
        val key = JapaneseFlickLayout.key("や")!!
        assertEquals("「", key.output(FlickDirection.LEFT))
        assertEquals("ゆ", key.output(FlickDirection.TOP))
        assertEquals("」", key.output(FlickDirection.RIGHT))
        assertEquals("よ", key.output(FlickDirection.BOTTOM))
    }

    @Test
    fun waRowHasNoBottomVariation() {
        val key = JapaneseFlickLayout.key("わ")!!
        assertEquals("を", key.output(FlickDirection.LEFT))
        assertEquals("ん", key.output(FlickDirection.TOP))
        assertEquals("ー", key.output(FlickDirection.RIGHT))
        assertNull(key.output(FlickDirection.BOTTOM))
    }
}
