package dev.turboboo.azookey.ime

import android.view.inputmethod.EditorInfo

/*
 * Labels mirror azooKey's Design.Language.getEnterKeyText.
 *
 * Upstream:
 *   azooKey/azooKey@b50db4aec1069a8d2341f70415da3bdc61f1fce6
 *   AzooKeyCore/Sources/KeyboardViews/Design.swift
 * Original implementation author: Keita Miwa (ensan)
 *
 * Licensed under the MIT License. See THIRD_PARTY_NOTICES.md.
 */
internal object EnterKeyPresentation {
    fun label(
        imeOptions: Int,
        hasComposition: Boolean,
    ): String {
        if (hasComposition) {
            return "確定"
        }

        return when (imeOptions and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_GO -> "開く"
            EditorInfo.IME_ACTION_SEARCH -> "検索"
            EditorInfo.IME_ACTION_SEND -> "送信"
            EditorInfo.IME_ACTION_NEXT -> "次へ"
            EditorInfo.IME_ACTION_DONE -> "完了"
            else -> "改行"
        }
    }
}
