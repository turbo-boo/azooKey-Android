package dev.turboboo.azookey.ime

import dev.turboboo.azookey.core.ImeEngine

class ImeController(
    private val engine: ImeEngine = ImeEngine(),
    private val executor: EditorCommandExecutor = EditorCommandExecutor(),
) {
    fun input(text: String, connection: EditorConnection) = Unit

    fun backspace(connection: EditorConnection) = Unit

    fun commit(connection: EditorConnection) = Unit

    fun space(connection: EditorConnection) = Unit

    fun reset() {
        engine.reset()
    }
}
