package dev.turboboo.azookey.ime

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputFieldPolicyTest {
    private fun editor(type: Int, options: Int = 0) = EditorInfo().apply {
        inputType = type
        imeOptions = options
    }

    @Test fun normalTextSupportsConversionAndLearning() {
        assertEquals(InputFieldPolicy.DEFAULT, InputFieldPolicy.from(editor(InputType.TYPE_CLASS_TEXT)))
    }

    @Test fun allPasswordVariantsUseDirectInputWithoutLearning() {
        val variants = listOf(
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD,
        )
        variants.forEach { type ->
            val policy = InputFieldPolicy.from(editor(type))
            assertTrue(policy.directInput)
            assertFalse(policy.allowSuggestions)
            assertFalse(policy.allowLearning)
        }
    }

    @Test fun noPersonalizedLearningStillAllowsSuggestions() {
        val policy = InputFieldPolicy.from(
            editor(InputType.TYPE_CLASS_TEXT, EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING),
        )
        assertTrue(policy.allowSuggestions)
        assertFalse(policy.allowLearning)
        assertFalse(policy.directInput)
    }

    @Test fun noSuggestionsDisablesCandidatesAndLearning() {
        val policy = InputFieldPolicy.from(
            editor(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS),
        )
        assertFalse(policy.allowSuggestions)
        assertFalse(policy.allowLearning)
    }

    @Test fun numberInputIsNotMistakenForPassword() {
        assertEquals(InputFieldPolicy.DEFAULT, InputFieldPolicy.from(editor(InputType.TYPE_CLASS_NUMBER)))
    }
}
