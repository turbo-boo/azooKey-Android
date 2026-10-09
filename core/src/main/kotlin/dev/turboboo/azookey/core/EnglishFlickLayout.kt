package dev.turboboo.azookey.core

/*
 * English flick assignments are ported from azooKey.
 *
 * Upstream:
 *   azooKey/azooKey
 *   AzooKeyCore/Sources/KeyboardViews/Custard/FlickEnglishCustard.swift
 * Original implementation author: Keita Miwa (ensan)
 * License: MIT
 */
object EnglishFlickLayout {
    private val lowercaseKeys = listOf(
        FlickKey(center = "@", left = "#", top = "/", right = "&", bottom = "_", label = "@#/&_"),
        FlickKey(center = "g", left = "h", top = "i", label = "GHI"),
        FlickKey(center = "p", left = "q", top = "r", right = "s", label = "PQRS"),
        FlickKey(center = "a", left = "b", top = "c", label = "ABC"),
        FlickKey(center = "j", left = "k", top = "l", label = "JKL"),
        FlickKey(center = "t", left = "u", top = "v", label = "TUV"),
        FlickKey(center = "'", left = "\"", top = "(", right = ")", label = "'\"()"),
        FlickKey(center = "d", left = "e", top = "f", label = "DEF"),
        FlickKey(center = "m", left = "n", top = "o", label = "MNO"),
        FlickKey(center = "w", left = "x", top = "y", right = "z", label = "WXYZ"),
        FlickKey(center = ".", left = ",", top = "?", right = "!", label = ".,?!"),
    )

    private val letterLabels = setOf(
        "ABC",
        "DEF",
        "GHI",
        "JKL",
        "MNO",
        "PQRS",
        "TUV",
        "WXYZ",
    )

    private val byLabel = lowercaseKeys.associateBy(FlickKey::label)

    fun key(label: String, uppercase: Boolean): FlickKey? {
        val key = byLabel[label] ?: return null
        if (!uppercase || label !in letterLabels) {
            return key
        }

        return key.copy(
            center = key.center.uppercase(),
            left = key.left?.uppercase(),
            top = key.top?.uppercase(),
            right = key.right?.uppercase(),
            bottom = key.bottom?.uppercase(),
        )
    }
}
