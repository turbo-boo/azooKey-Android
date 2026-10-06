package dev.turboboo.azookey.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class ImeControllerTest {
    @Test
    fun inputUpdatesComposition() {
        val connection = RecordingEditorConnection()
        val controller = ImeController()

        controller.input("あ", connection)
        controller.input("い", connection)

        assertEquals(
            listOf("compose:あ", "compose:あい"),
            connection.calls,
        )
    }

    @Test
    fun backspaceEditsCompositionBeforeEditorHistory() {
        val connection = RecordingEditorConnection()
        val controller = ImeController()

        controller.input("かな", connection)
        connection.calls.clear()

        controller.backspace(connection)
        controller.backspace(connection)
        controller.backspace(connection)

        assertEquals(
            listOf(
                "compose:か",
                "compose:",
                "deleteCodePoints:1:0",
            ),
            connection.calls,
        )
    }

    @Test
    fun commitFinalizesComposition() {
        val connection = RecordingEditorConnection()
        val controller = ImeController()

        controller.input("かな", connection)
        connection.calls.clear()
        controller.commit(connection)

        assertEquals(listOf("commit:かな"), connection.calls)

        connection.calls.clear()
        controller.backspace(connection)
        assertEquals(listOf("deleteCodePoints:1:0"), connection.calls)
    }

    @Test
    fun spaceCommitsCompositionBeforeInsertingSpace() {
        val connection = RecordingEditorConnection()
        val controller = ImeController()

        controller.input("かな", connection)
        connection.calls.clear()
        controller.space(connection)

        assertEquals(
            listOf(
                "commit:かな",
                "commit: ",
            ),
            connection.calls,
        )
    }

    @Test
    fun resetDropsInternalCompositionWithoutEditingHost() {
        val connection = RecordingEditorConnection()
        val controller = ImeController()

        controller.input("かな", connection)
        connection.calls.clear()
        controller.reset()
        controller.backspace(connection)

        assertEquals(listOf("deleteCodePoints:1:0"), connection.calls)
    }

    private class RecordingEditorConnection : EditorConnection {
        val calls = mutableListOf<String>()

        override fun setComposingText(text: String): Boolean {
            calls += "compose:$text"
            return true
        }

        override fun commitText(text: String): Boolean {
            calls += "commit:$text"
            return true
        }

        override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean {
            calls += "deleteCodePoints:$beforeLength:$afterLength"
            return true
        }

        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
            calls += "deleteUtf16:$beforeLength:$afterLength"
            return true
        }
    }
}
