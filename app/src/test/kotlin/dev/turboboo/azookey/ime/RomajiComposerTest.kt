package dev.turboboo.azookey.ime

import org.junit.Assert.*
import org.junit.Test

class RomajiComposerTest {
    private fun convert(input: String): String {
        val c = RomajiComposer()
        return buildString {
            input.forEach { append(c.accept(it.toString())) }
            append(c.flush())
        }
    }
    @Test fun basicSyllablesAndContractions() {
        assertEquals("かな", convert("kana"))
        assertEquals("きょう", convert("kyou"))
        assertEquals("しんぶん", convert("shinbun"))
        assertEquals("ちょっと", convert("chotto"))
        assertEquals("っか", convert("kka"))
    }
    @Test fun ambiguousNAndSmallKana() {
        assertEquals("んな", convert("nna"))
        assertEquals("にゃ", convert("nya"))
        assertEquals("ん", convert("nn"))
        assertEquals("んあ", convert("n'a"))
        assertEquals("ふぁ", convert("fa"))
        assertEquals("っ", convert("xtsu"))
    }
    @Test fun deletingUnfinishedPrefix() {
        val c = RomajiComposer()
        assertEquals("", c.accept("k"))
        assertTrue(c.hasPending)
        assertTrue(c.backspace())
        assertFalse(c.backspace())
        assertEquals("あ", c.accept("a"))
    }
}
