package dev.turboboo.azookey.ime

/**
 * Only hiragana input is a composing kana-kanji conversion sequence.
 * Latin and number/symbol keys commit literal text; the converter never sees
 * those sequences. This is also enforced for private/direct-input fields.
 */
enum class KeyboardInputMode {
    HIRAGANA,
    LATIN,
    NUMBER_SYMBOLS,
    JAPANESE_QWERTY,
    LATIN_QWERTY,
    EMOJI,
}

internal class ImeTextInputRouter(
    private val controller: ImeController,
    private val romaji: RomajiComposer = RomajiComposer(),
) {
    val hasPendingRomaji: Boolean get() = romaji.hasPending
    fun reset() = romaji.reset()
    fun flush(connection: EditorConnection) {
        val text = romaji.flush()
        if (text.isNotEmpty()) controller.input(text, connection)
    }
    fun backspace(connection: EditorConnection) {
        if (!romaji.backspace()) controller.backspace(connection)
    }
    /**
     * @return true when kana-kanji candidates should be refreshed.
     */
    fun input(
        text: String,
        mode: KeyboardInputMode,
        policy: InputFieldPolicy,
        connection: EditorConnection,
    ): Boolean {
        if (text.isEmpty()) return false
        if (policy.directInput || mode !in setOf(
                KeyboardInputMode.HIRAGANA,
                KeyboardInputMode.JAPANESE_QWERTY,
            )
        ) {
            flush(connection)
            controller.commit(connection)
            connection.commitText(text)
            return false
        }
        if (mode == KeyboardInputMode.JAPANESE_QWERTY) {
            val converted = romaji.accept(text)
            if (converted.isNotEmpty()) controller.input(converted, connection)
        } else {
            flush(connection)
            controller.input(text, connection)
        }
        return policy.allowSuggestions && controller.composingText.isNotEmpty()
    }
}
