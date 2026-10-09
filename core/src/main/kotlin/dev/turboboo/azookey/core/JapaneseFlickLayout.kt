package dev.turboboo.azookey.core

/*
 * The Japanese flick key variations are ported from azooKey.
 *
 * Original implementation:
 *   azooKey/azooKey
 *   AzooKeyCore/Sources/KeyboardViews/Custard/FlickJapaneseCustard.swift
 * Original author: Keita Miwa (ensan)
 * License: MIT
 */

data class FlickKey(
    val center: String,
    val left: String? = null,
    val top: String? = null,
    val right: String? = null,
    val bottom: String? = null,
    val label: String = center,
) {
    fun output(direction: FlickDirection): String? = when (direction) {
        FlickDirection.CENTER -> center
        FlickDirection.LEFT -> left
        FlickDirection.TOP -> top
        FlickDirection.RIGHT -> right
        FlickDirection.BOTTOM -> bottom
    }
}

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

    private val byCenter = kanaKeys.associateBy(FlickKey::center)

    fun key(center: String): FlickKey? = byCenter[center]
}
