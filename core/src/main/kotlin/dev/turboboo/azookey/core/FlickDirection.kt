package dev.turboboo.azookey.core

data class PointF2(
    val x: Float,
    val y: Float,
)

enum class FlickDirection {
    CENTER,
    LEFT,
    TOP,
    RIGHT,
    BOTTOM,
}

class FlickDirectionResolver(
    private val thresholdPx: Float,
) {
    init {
        require(thresholdPx >= 0f)
    }

    fun resolve(start: PointF2, end: PointF2): FlickDirection = FlickDirection.CENTER
}
