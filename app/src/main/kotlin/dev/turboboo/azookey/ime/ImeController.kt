package dev.turboboo.azookey.ime

import dev.turboboo.azookey.core.ImeEngine

class ImeController(
    private val engine: ImeEngine = ImeEngine(),
    private val executor: EditorCommandExecutor = EditorCommandExecutor(),
) {
    val composingText: String
        get() = engine.composingText

    fun input(text: String, connection: EditorConnection) {
        executor.execute(engine.input(text), connection)
    }

    fun backspace(connection: EditorConnection) {
        executor.execute(engine.backspace(), connection)
    }

    fun changeCharacterType(connection: EditorConnection) {
        executor.execute(engine.changeLastCharacterType(), connection)
    }

    fun commit(connection: EditorConnection) {
        executor.execute(engine.commit(), connection)
    }

    fun selectCandidate(candidate: String, connection: EditorConnection) {
        executor.execute(engine.commitCandidate(candidate), connection)
    }

    fun space(connection: EditorConnection) {
        executor.execute(engine.commit(), connection)
        connection.commitText(" ")
    }

    fun reset() {
        engine.reset()
    }
}
