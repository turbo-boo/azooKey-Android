package dev.turboboo.azookey.ime

import android.view.inputmethod.InputConnection

class AndroidEditorConnection(
    private val inputConnection: InputConnection,
) : EditorConnection {
    override fun setComposingText(text: String): Boolean =
        inputConnection.setComposingText(text, 1)

    override fun commitText(text: String): Boolean =
        inputConnection.commitText(text, 1)

    override fun deleteSurroundingTextInCodePoints(
        beforeLength: Int,
        afterLength: Int,
    ): Boolean = inputConnection.deleteSurroundingTextInCodePoints(
        beforeLength,
        afterLength,
    )

    override fun deleteSurroundingText(
        beforeLength: Int,
        afterLength: Int,
    ): Boolean = inputConnection.deleteSurroundingText(
        beforeLength,
        afterLength,
    )
}
