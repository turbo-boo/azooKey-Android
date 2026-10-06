package dev.turboboo.azookey.core

sealed interface EditorCommand {
    data class SetComposingText(val text: String) : EditorCommand
    data class CommitText(val text: String) : EditorCommand
    data class DeleteSurroundingText(val beforeCodePoints: Int, val afterCodePoints: Int) : EditorCommand
}

class ImeEngine {
    var composingText: String = ""
        private set

    fun input(text: String): List<EditorCommand> = emptyList()

    fun backspace(): List<EditorCommand> = emptyList()

    fun commit(): List<EditorCommand> = emptyList()

    fun reset() {
        composingText = ""
    }
}
