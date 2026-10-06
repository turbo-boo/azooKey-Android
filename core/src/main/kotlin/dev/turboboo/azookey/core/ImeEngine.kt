package dev.turboboo.azookey.core

sealed interface EditorCommand {
    data class SetComposingText(val text: String) : EditorCommand
    data class CommitText(val text: String) : EditorCommand
    data class DeleteSurroundingText(
        val beforeCodePoints: Int,
        val afterCodePoints: Int,
    ) : EditorCommand
}

class ImeEngine {
    var composingText: String = ""
        private set

    fun input(text: String): List<EditorCommand> {
        if (text.isEmpty()) {
            return emptyList()
        }

        composingText += text
        return listOf(EditorCommand.SetComposingText(composingText))
    }

    fun backspace(): List<EditorCommand> {
        if (composingText.isEmpty()) {
            return listOf(
                EditorCommand.DeleteSurroundingText(
                    beforeCodePoints = 1,
                    afterCodePoints = 0,
                ),
            )
        }

        composingText = composingText.dropLastCodePoint()
        return listOf(EditorCommand.SetComposingText(composingText))
    }

    fun commit(): List<EditorCommand> {
        if (composingText.isEmpty()) {
            return emptyList()
        }

        val committed = composingText
        composingText = ""
        return listOf(EditorCommand.CommitText(committed))
    }

    fun reset() {
        composingText = ""
    }

    private fun String.dropLastCodePoint(): String {
        if (isEmpty()) {
            return this
        }

        val codePoint = codePointBefore(length)
        return dropLast(Character.charCount(codePoint))
    }
}
