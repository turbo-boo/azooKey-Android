package dev.turboboo.azookey.ime

import dev.turboboo.azookey.core.EditorCommand

interface EditorConnection {
    fun setComposingText(text: String): Boolean
    fun commitText(text: String): Boolean
    fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean
    fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean
}

class EditorCommandExecutor {
    fun execute(
        commands: List<EditorCommand>,
        connection: EditorConnection,
    ) {
        // Implemented after the behavior tests are confirmed red.
    }
}
