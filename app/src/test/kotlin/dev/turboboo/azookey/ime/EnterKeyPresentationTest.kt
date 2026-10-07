package dev.turboboo.azookey.ime

import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class EnterKeyPresentationTest {
    @Test
    fun composingTextUsesCompleteLabel() {
        assertEquals(
            "確定",
            EnterKeyPresentation.label(
                imeOptions = EditorInfo.IME_ACTION_SEARCH,
                hasComposition = true,
            ),
        )
    }

    @Test
    fun editorActionsUseAzooKeyLabels() {
        assertEquals("改行", label(EditorInfo.IME_ACTION_NONE))
        assertEquals("開く", label(EditorInfo.IME_ACTION_GO))
        assertEquals("検索", label(EditorInfo.IME_ACTION_SEARCH))
        assertEquals("送信", label(EditorInfo.IME_ACTION_SEND))
        assertEquals("次へ", label(EditorInfo.IME_ACTION_NEXT))
        assertEquals("完了", label(EditorInfo.IME_ACTION_DONE))
        assertEquals("改行", label(EditorInfo.IME_ACTION_UNSPECIFIED))
    }

    @Test
    fun optionFlagsDoNotChangeActionLabel() {
        assertEquals(
            "検索",
            label(
                EditorInfo.IME_ACTION_SEARCH or
                    EditorInfo.IME_FLAG_NO_EXTRACT_UI,
            ),
        )
    }

    private fun label(options: Int): String =
        EnterKeyPresentation.label(
            imeOptions = options,
            hasComposition = false,
        )
}
