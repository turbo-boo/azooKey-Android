package dev.turboboo.azookey.core

/*
 * Ported from azooKey's ReplaceBehaviorManager.defaultReplace behavior.
 *
 * Upstream:
 *   azooKey/azooKey
 *   AzooKeyCore/Sources/KeyboardExtensionUtils/ReplaceBehaviorManager.swift
 * Original author: Keita Miwa (ensan)
 *
 * Copyright (c) 2020-2023 Keita Miwa (ensan).
 * Licensed under the MIT License. See THIRD_PARTY_NOTICES.md.
 */
object KanaCharacterTransformer {
    private val replacements = mapOf(
        "あ" to "ぁ",
        "ぁ" to "あ",
        "い" to "ぃ",
        "ぃ" to "い",
        "え" to "ぇ",
        "ぇ" to "え",
        "お" to "ぉ",
        "ぉ" to "お",
        "や" to "ゃ",
        "ゃ" to "や",
        "ゆ" to "ゅ",
        "ゅ" to "ゆ",
        "よ" to "ょ",
        "ょ" to "よ",
        "わ" to "ゎ",
        "ゎ" to "わ",

        "か" to "が",
        "が" to "か",
        "き" to "ぎ",
        "ぎ" to "き",
        "く" to "ぐ",
        "ぐ" to "く",
        "け" to "げ",
        "げ" to "け",
        "こ" to "ご",
        "ご" to "こ",

        "さ" to "ざ",
        "ざ" to "さ",
        "し" to "じ",
        "じ" to "し",
        "す" to "ず",
        "ず" to "す",
        "せ" to "ぜ",
        "ぜ" to "せ",
        "そ" to "ぞ",
        "ぞ" to "そ",

        "た" to "だ",
        "だ" to "た",
        "ち" to "ぢ",
        "ぢ" to "ち",
        "て" to "で",
        "で" to "て",
        "と" to "ど",
        "ど" to "と",

        "つ" to "っ",
        "っ" to "づ",
        "づ" to "つ",

        "う" to "ぅ",
        "ぅ" to "ゔ",
        "ゔ" to "う",

        "は" to "ば",
        "ば" to "ぱ",
        "ぱ" to "は",
        "ひ" to "び",
        "び" to "ぴ",
        "ぴ" to "ひ",
        "ふ" to "ぶ",
        "ぶ" to "ぷ",
        "ぷ" to "ふ",
        "へ" to "べ",
        "べ" to "ぺ",
        "ぺ" to "へ",
        "ほ" to "ぼ",
        "ぼ" to "ぽ",
        "ぽ" to "ほ",
    )

    fun transform(character: String): String = replacements[character] ?: character
}
