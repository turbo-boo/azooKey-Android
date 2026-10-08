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
}

internal class ImeTextInputRouter(
    private val controller: ImeController,
) {
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
        if (policy.directInput || mode != KeyboardInputMode.HIRAGANA) {
            // Finish any composition before inserting literal text.
            controller.commit(connection)
            connection.commitText(text)
            return false
        }
        controller.input(text, connection)
        return policy.allowSuggestions
    }
}
