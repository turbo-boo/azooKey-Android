package dev.turboboo.azookey.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class ImeControllerModifierTest {
    @Test
    fun changeCharacterTypeUpdatesCurrentComposition() {
        val connection = RecordingConnection()
        val controller = ImeController()

        controller.input("か", connection)
        connection.calls.clear()
        controller.changeCharacterType(connection)

        assertEquals(listOf("compose:が"), connection.calls)
    }

    private class RecordingConnection : EditorConnection {
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
