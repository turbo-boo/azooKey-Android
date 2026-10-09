package dev.turboboo.azookey.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EnglishFlickLayoutTest {
    @Test
    fun lowercaseLetterGroupsMatchAzooKey() {
        assertEquals(
            FlickKey(
                center = "a",
                left = "b",
                top = "c",
                label = "ABC",
            ),
            EnglishFlickLayout.key("ABC", uppercase = false),
        )
        assertEquals(
            FlickKey(
                center = "p",
                left = "q",
                top = "r",
                right = "s",
                label = "PQRS",
            ),
            EnglishFlickLayout.key("PQRS", uppercase = false),
        )
        assertEquals(
            FlickKey(
                center = "w",
                left = "x",
                top = "y",
                right = "z",
                label = "WXYZ",
            ),
            EnglishFlickLayout.key("WXYZ", uppercase = false),
        )
    }

    @Test
    fun uppercaseModeChangesLetterOutputButNotLabels() {
        assertEquals(
            FlickKey(
                center = "A",
                left = "B",
                top = "C",
                label = "ABC",
            ),
            EnglishFlickLayout.key("ABC", uppercase = true),
        )
        assertEquals(
            FlickKey(
                center = "G",
                left = "H",
                top = "I",
                label = "GHI",
            ),
            EnglishFlickLayout.key("GHI", uppercase = true),
        )
    }

    @Test
    fun symbolGroupsMatchAzooKeyAndIgnoreCaseMode() {
        val at = FlickKey(
            center = "@",
            left = "#",
            top = "/",
            right = "&",
            bottom = "_",
            label = "@#/&_",
        )
        val punctuation = FlickKey(
            center = ".",
            left = ",",
            top = "?",
            right = "!",
            label = ".,?!",
        )

        assertEquals(at, EnglishFlickLayout.key("@#/&_", uppercase = false))
        assertEquals(at, EnglishFlickLayout.key("@#/&_", uppercase = true))
        assertEquals(punctuation, EnglishFlickLayout.key(".,?!", uppercase = false))
        assertNull(EnglishFlickLayout.key("missing", uppercase = false))
    }
}
