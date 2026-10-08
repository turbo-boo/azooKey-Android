package dev.turboboo.azookey.ime

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeTextInputRouterTest {
    private class RecordingConnection : EditorConnection {
        val calls = mutableListOf<String>()
        override fun setComposingText(text: String): Boolean {
            calls += "compose:$text"
            return true
        }
        override fun commitText(text: String): Boolean {
            calls += "commit:$text"
            return true
        }
        override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean = true
        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean = true
    }

    @Test fun hiraganaComposesAndRequestsCandidates() {
        val controller = ImeController()
        val connection = RecordingConnection()
        val router = ImeTextInputRouter(controller)

        assertTrue(router.input("か", KeyboardInputMode.HIRAGANA, InputFieldPolicy.DEFAULT, connection))
        assertEquals("か", controller.composingText)
        assertEquals(listOf("compose:か"), connection.calls)
    }

    @Test fun latinAndSymbolsAreLiteralAndDoNotInvokeConversion() {
        for ((mode, value) in listOf(
            KeyboardInputMode.LATIN to "abc",
            KeyboardInputMode.NUMBER_SYMBOLS to "１２☆",
        )) {
            val controller = ImeController()
            val connection = RecordingConnection()
            val router = ImeTextInputRouter(controller)

            assertFalse(router.input(value, mode, InputFieldPolicy.DEFAULT, connection))
            assertEquals("", controller.composingText)
            assertEquals(listOf("commit:$value"), connection.calls)
        }
    }

    @Test fun inputModeChangeDoesNotAppendLatinToUnfinishedKana() {
        val controller = ImeController()
        val connection = RecordingConnection()
        val router = ImeTextInputRouter(controller)
        router.input("かな", KeyboardInputMode.HIRAGANA, InputFieldPolicy.DEFAULT, connection)

        assertFalse(router.input("x", KeyboardInputMode.LATIN, InputFieldPolicy.DEFAULT, connection))

        assertEquals("", controller.composingText)
        assertEquals(listOf("compose:かな", "commit:かな", "commit:x"), connection.calls)
    }

    @Test fun passwordBypassesConversionEvenInHiraganaMode() {
        val policy = InputFieldPolicy.from(EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        })
        val controller = ImeController()
        val connection = RecordingConnection()

        assertFalse(ImeTextInputRouter(controller).input("あ", KeyboardInputMode.HIRAGANA, policy, connection))
        assertEquals("", controller.composingText)
        assertEquals(listOf("commit:あ"), connection.calls)
    }

    @Test fun noSuggestionFieldCanComposeWithoutPublishingCandidates() {
        val policy = InputFieldPolicy.from(EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        })
        val controller = ImeController()
        val connection = RecordingConnection()

        assertFalse(ImeTextInputRouter(controller).input("か", KeyboardInputMode.HIRAGANA, policy, connection))
        assertEquals("か", controller.composingText)
        assertEquals(listOf("compose:か"), connection.calls)
    }

    @Test fun emptyTextIsIgnored() {
        val connection = RecordingConnection()
        val router = ImeTextInputRouter(ImeController())
        assertFalse(router.input("", KeyboardInputMode.LATIN, InputFieldPolicy.DEFAULT, connection))
        assertEquals(emptyList<String>(), connection.calls)
    }
}
