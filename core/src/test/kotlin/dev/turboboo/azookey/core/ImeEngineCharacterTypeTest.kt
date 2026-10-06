package dev.turboboo.azookey.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ImeEngineCharacterTypeTest {
    @Test
    fun changeCharacterTypeReplacesOnlyLastComposingCharacter() {
        val engine = ImeEngine()
        engine.input("かか")

        assertEquals(
            listOf(EditorCommand.SetComposingText("かが")),
            engine.changeLastCharacterType(),
        )
        assertEquals("かが", engine.composingText)
    }

    @Test
    fun changeCharacterTypeWithoutCompositionDoesNothing() {
        val engine = ImeEngine()

        assertEquals(emptyList<EditorCommand>(), engine.changeLastCharacterType())
    }
}
