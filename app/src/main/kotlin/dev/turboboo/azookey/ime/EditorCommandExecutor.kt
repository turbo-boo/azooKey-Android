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
        commands.forEach { command ->
            when (command) {
                is EditorCommand.SetComposingText -> {
                    connection.setComposingText(command.text)
                }

                is EditorCommand.CommitText -> {
                    connection.commitText(command.text)
                }

                is EditorCommand.DeleteSurroundingText -> {
                    val deletedByCodePoint = connection.deleteSurroundingTextInCodePoints(
                        beforeLength = command.beforeCodePoints,
                        afterLength = command.afterCodePoints,
                    )
                    if (!deletedByCodePoint) {
                        connection.deleteSurroundingText(
                            beforeLength = command.beforeCodePoints,
                            afterLength = command.afterCodePoints,
                        )
                    }
                }
            }
        }
    }
}
