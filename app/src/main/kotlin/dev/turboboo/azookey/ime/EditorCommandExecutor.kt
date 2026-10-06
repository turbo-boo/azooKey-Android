package dev.turboboo.azookey.ime

import dev.turboboo.azookey.core.EditorCommand

interface EditorConnection {
    fun setComposingText(text: String): Boolean
    fun commitText(text: String): Boolean
    fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean
    fun getTextBeforeCursor(maxChars: Int): CharSequence? = null
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
                            beforeLength = utf16LengthBeforeCursor(
                                connection = connection,
                                codePointCount = command.beforeCodePoints,
                            ),
                            afterLength = command.afterCodePoints,
                        )
                    }
                }
            }
        }
    }

    private fun utf16LengthBeforeCursor(
        connection: EditorConnection,
        codePointCount: Int,
    ): Int {
        if (codePointCount <= 0) {
            return 0
        }

        val maxUtf16Chars = codePointCount * Character.MAX_VALUE.code.coerceAtMost(2)
        val text = connection.getTextBeforeCursor(maxUtf16Chars)?.toString()
            ?: return codePointCount

        var start = text.length
        var remaining = codePointCount
        while (remaining > 0 && start > 0) {
            start = text.offsetByCodePoints(start, -1)
            remaining--
        }
        return text.length - start
    }
}
