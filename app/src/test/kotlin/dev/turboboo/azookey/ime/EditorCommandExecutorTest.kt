package dev.turboboo.azookey.ime

import dev.turboboo.azookey.core.EditorCommand
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorCommandExecutorTest {
    @Test
    fun setComposingTextIsForwarded() {
        val connection = FakeEditorConnection()
        val executor = EditorCommandExecutor()

        executor.execute(
            listOf(EditorCommand.SetComposingText("かな")),
            connection,
        )

        assertEquals(listOf("compose:かな"), connection.calls)
    }

    @Test
    fun commitTextIsForwarded() {
        val connection = FakeEditorConnection()
        val executor = EditorCommandExecutor()

        executor.execute(
            listOf(EditorCommand.CommitText("かな")),
            connection,
        )

        assertEquals(listOf("commit:かな"), connection.calls)
    }

    @Test
    fun deletionUsesCodePointsFirst() {
        val connection = FakeEditorConnection(codePointDeleteResult = true)
        val executor = EditorCommandExecutor()

        executor.execute(
            listOf(EditorCommand.DeleteSurroundingText(1, 0)),
            connection,
        )

        assertEquals(listOf("deleteCodePoints:1:0"), connection.calls)
    }

    @Test
    fun deletionFallsBackToUtf16WhenEditorRejectsCodePointDeletion() {
        val connection = FakeEditorConnection(codePointDeleteResult = false)
        val executor = EditorCommandExecutor()

        executor.execute(
            listOf(EditorCommand.DeleteSurroundingText(1, 0)),
            connection,
        )

        assertEquals(
            listOf(
                "deleteCodePoints:1:0",
                "deleteUtf16:1:0",
            ),
            connection.calls,
        )
    }

    @Test
    fun commandsPreserveOrder() {
        val connection = FakeEditorConnection()
        val executor = EditorCommandExecutor()

        executor.execute(
            listOf(
                EditorCommand.SetComposingText("あ"),
                EditorCommand.SetComposingText("あい"),
                EditorCommand.CommitText("あい"),
            ),
            connection,
        )

        assertEquals(
            listOf(
                "compose:あ",
                "compose:あい",
                "commit:あい",
            ),
            connection.calls,
        )
    }

    private class FakeEditorConnection(
        private val codePointDeleteResult: Boolean = true,
    ) : EditorConnection {
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
            return codePointDeleteResult
        }

        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
            calls += "deleteUtf16:$beforeLength:$afterLength"
            return true
        }
    }
}
