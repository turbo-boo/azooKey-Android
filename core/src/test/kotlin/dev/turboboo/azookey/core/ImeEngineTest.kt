package dev.turboboo.azookey.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeEngineTest {
    @Test
    fun inputBuildsComposingTextAndEmitsSetComposingText() {
        val engine = ImeEngine()

        assertEquals(
            listOf(EditorCommand.SetComposingText("き")),
            engine.input("き"),
        )
        assertEquals(
            listOf(EditorCommand.SetComposingText("きょ")),
            engine.input("ょ"),
        )
        assertEquals("きょ", engine.composingText)
    }

    @Test
    fun backspaceEditsCompositionBeforeTouchingEditorHistory() {
        val engine = ImeEngine()
        engine.input("かな")

        assertEquals(
            listOf(EditorCommand.SetComposingText("か")),
            engine.backspace(),
        )
        assertEquals(
            listOf(EditorCommand.SetComposingText("")),
            engine.backspace(),
        )
        assertEquals("", engine.composingText)
    }

    @Test
    fun backspaceWithoutCompositionDeletesOneCodePointBeforeCursor() {
        val engine = ImeEngine()

        assertEquals(
            listOf(EditorCommand.DeleteSurroundingText(beforeCodePoints = 1, afterCodePoints = 0)),
            engine.backspace(),
        )
    }

    @Test
    fun commitCommitsCompositionAndClearsState() {
        val engine = ImeEngine()
        engine.input("かな")

        assertEquals(
            listOf(EditorCommand.CommitText("かな")),
            engine.commit(),
        )
        assertEquals("", engine.composingText)
        assertTrue(engine.commit().isEmpty())
    }
}
