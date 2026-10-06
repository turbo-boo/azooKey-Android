package dev.turboboo.azookey.core

data class FlickKey(
    val center: String,
    val left: String? = null,
    val top: String? = null,
    val right: String? = null,
    val bottom: String? = null,
) {
    fun output(direction: FlickDirection): String? = when (direction) {
        FlickDirection.CENTER -> center
        FlickDirection.LEFT -> left
        FlickDirection.TOP -> top
        FlickDirection.RIGHT -> right
        FlickDirection.BOTTOM -> bottom
    }
}

/*
 * Kana/flick assignments are ported from azooKey's built-in Japanese flick Custard.
 *
 * Upstream:
 *   azooKey/azooKey
 *   AzooKeyCore/Sources/KeyboardViews/Custard/FlickJapaneseCustard.swift
 * Upstream implementation author: Miwa (ensan-hcl)
 * Source commit introducing the current Custard implementation:
 *   a3b1a3a3b52d91bdae2846461e778797d3ea5016
 *
 * Copyright (c) 2020-2023 Keita Miwa (ensan).
 * Licensed under the MIT License. See THIRD_PARTY_NOTICES.md.
 */
object JapaneseFlickLayout {
    val kanaKeys: List<FlickKey> = listOf(
        FlickKey(center = "あ", left = "い", top = "う", right = "え", bottom = "お"),
        FlickKey(center = "か", left = "き", top = "く", right = "け", bottom = "こ"),
        FlickKey(center = "さ", left = "し", top = "す", right = "せ", bottom = "そ"),
        FlickKey(center = "た", left = "ち", top = "つ", right = "て", bottom = "と"),
        FlickKey(center = "な", left = "に", top = "ぬ", right = "ね", bottom = "の"),
        FlickKey(center = "は", left = "ひ", top = "ふ", right = "へ", bottom = "ほ"),
        FlickKey(center = "ま", left = "み", top = "む", right = "め", bottom = "も"),
        FlickKey(center = "や", left = "「", top = "ゆ", right = "」", bottom = "よ"),
        FlickKey(center = "ら", left = "り", top = "る", right = "れ", bottom = "ろ"),
        FlickKey(center = "わ", left = "を", top = "ん", right = "ー"),
    )

    private val keysByCenter = kanaKeys.associateBy(FlickKey::center)

    fun key(center: String): FlickKey? = keysByCenter[center]
}
