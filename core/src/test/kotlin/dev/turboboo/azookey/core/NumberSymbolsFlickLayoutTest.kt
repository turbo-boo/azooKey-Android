package dev.turboboo.azookey.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumberSymbolsFlickLayoutTest {
    @Test
    fun numberAndSymbolGroupsMatchAzooKey() {
        assertEquals(
            FlickKey(
                center = "1",
                left = "☆",
                top = "♪",
                right = "→",
                label = "1☆♪→",
            ),
            NumberSymbolsFlickLayout.key("1☆♪→"),
        )
        assertEquals(
            FlickKey(
                center = "5",
                left = "+",
                top = "×",
                right = "÷",
                label = "5+×÷",
            ),
            NumberSymbolsFlickLayout.key("5+×÷"),
        )
        assertEquals(
            FlickKey(
                center = "9",
                left = "^",
                top = "|",
                right = "\\",
                label = "9^|\\",
            ),
            NumberSymbolsFlickLayout.key("9^|\\"),
        )
    }

    @Test
    fun bottomRowGroupsMatchAzooKey() {
        assertEquals(
            FlickKey(
                center = "0",
                left = "〜",
                top = "…",
                label = "0〜…",
            ),
            NumberSymbolsFlickLayout.key("0〜…"),
        )
        assertEquals(
            FlickKey(
                center = ".",
                left = ",",
                top = "-",
                right = "/",
                label = ".,-/",
            ),
            NumberSymbolsFlickLayout.key(".,-/"),
        )
        assertNull(NumberSymbolsFlickLayout.key("missing"))
    }
}
