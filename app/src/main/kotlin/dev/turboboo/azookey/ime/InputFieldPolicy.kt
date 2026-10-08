package dev.turboboo.azookey.ime

import android.text.InputType
import android.view.inputmethod.EditorInfo

/**
 * The host editor controls whether suggestions and learning are appropriate.
 * Password fields use direct input to avoid invoking the native dictionary.
 */
internal data class InputFieldPolicy(
    val directInput: Boolean,
    val allowSuggestions: Boolean,
    val allowLearning: Boolean,
    val preferredMode: KeyboardInputMode? = null,
) {
    companion object {
        val DEFAULT = InputFieldPolicy(
            directInput = false,
            allowSuggestions = true,
            allowLearning = true,
        )

        fun from(editor: EditorInfo?): InputFieldPolicy {
            if (editor == null) return DEFAULT

            val type = editor.inputType
            val klass = type and InputType.TYPE_MASK_CLASS
            val variation = type and InputType.TYPE_MASK_VARIATION
            val password =
                (klass == InputType.TYPE_CLASS_TEXT &&
                    variation in setOf(
                        InputType.TYPE_TEXT_VARIATION_PASSWORD,
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                        InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
                    )) ||
                    (klass == InputType.TYPE_CLASS_NUMBER &&
                        variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD)
            if (password) {
                return InputFieldPolicy(
                    directInput = true,
                    allowSuggestions = false,
                    allowLearning = false,
                    preferredMode = KeyboardInputMode.LATIN,
                )
            }

            // Numeric, telephone and date editors should not run the Japanese
            // dictionary or learn their literal contents.
            if (
                klass == InputType.TYPE_CLASS_NUMBER ||
                klass == InputType.TYPE_CLASS_PHONE ||
                klass == InputType.TYPE_CLASS_DATETIME
            ) {
                return InputFieldPolicy(
                    directInput = true,
                    allowSuggestions = false,
                    allowLearning = false,
                    preferredMode = KeyboardInputMode.NUMBER_SYMBOLS,
                )
            }

            val latinTextField = klass == InputType.TYPE_CLASS_TEXT &&
                variation in setOf(
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                    InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
                    InputType.TYPE_TEXT_VARIATION_URI,
                )
            if (latinTextField) {
                return InputFieldPolicy(
                    directInput = true,
                    allowSuggestions = false,
                    allowLearning = false,
                    preferredMode = KeyboardInputMode.LATIN,
                )
            }

            val noSuggestions =
                klass == InputType.TYPE_CLASS_TEXT &&
                    type and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS != 0
            val noLearning =
                editor.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0

            return InputFieldPolicy(
                directInput = false,
                allowSuggestions = !noSuggestions,
                allowLearning = !noSuggestions && !noLearning,
            )
        }
    }
}
