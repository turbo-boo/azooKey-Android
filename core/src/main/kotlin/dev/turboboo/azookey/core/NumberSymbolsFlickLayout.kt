package dev.turboboo.azookey.core

/*
 * Number/symbol flick assignments are ported from azooKey.
 *
 * Upstream:
 *   azooKey/azooKey
 *   AzooKeyCore/Sources/KeyboardViews/Custard/FlickNumberSymbolsCustard.swift
 * Original implementation author: Keita Miwa (ensan)
 * License: MIT
 */
object NumberSymbolsFlickLayout {
    val keys: List<FlickKey> = listOf(
        FlickKey(center = "1", left = "☆", top = "♪", right = "→", label = "1☆♪→"),
        FlickKey(center = "4", left = "○", top = "＊", right = "・", label = "4○＊・"),
        FlickKey(center = "7", left = "「", top = "」", right = ":", label = "7「」:"),
        FlickKey(center = "(", left = ")", top = "[", right = "]", label = "()[]"),
        FlickKey(center = "2", left = "¥", top = "$", right = "€", label = "2¥$€"),
        FlickKey(center = "5", left = "+", top = "×", right = "÷", label = "5+×÷"),
        FlickKey(center = "8", left = "〒", top = "々", right = "〆", label = "8〒々〆"),
        FlickKey(center = "0", left = "〜", top = "…", label = "0〜…"),
        FlickKey(center = "3", left = "%", top = "°", right = "#", label = "3%°#"),
        FlickKey(center = "6", left = "<", top = "=", right = ">", label = "6<=>"),
        FlickKey(center = "9", left = "^", top = "|", right = "\\", label = "9^|\\"),
        FlickKey(center = ".", left = ",", top = "-", right = "/", label = ".,-/"),
    )

    private val byLabel = keys.associateBy(FlickKey::label)

    fun key(label: String): FlickKey? = byLabel[label]
}
