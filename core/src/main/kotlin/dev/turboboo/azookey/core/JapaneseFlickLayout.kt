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

object JapaneseFlickLayout {
    val kanaKeys: List<FlickKey> = emptyList()

    fun key(center: String): FlickKey? = kanaKeys.firstOrNull { it.center == center }
}
