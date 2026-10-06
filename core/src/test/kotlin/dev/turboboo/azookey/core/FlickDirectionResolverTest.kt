package dev.turboboo.azookey.core

import org.junit.Assert.assertEquals
import org.junit.Test

class FlickDirectionResolverTest {
    private val resolver = FlickDirectionResolver(thresholdPx = 16f)
    private val origin = PointF2(100f, 100f)

    @Test
    fun movementInsideThresholdIsCenter() {
        assertEquals(
            FlickDirection.CENTER,
            resolver.resolve(origin, PointF2(108f, 108f)),
        )
    }

    @Test
    fun dominantHorizontalMovementResolvesLeftAndRight() {
        assertEquals(
            FlickDirection.LEFT,
            resolver.resolve(origin, PointF2(60f, 106f)),
        )
        assertEquals(
            FlickDirection.RIGHT,
            resolver.resolve(origin, PointF2(140f, 94f)),
        )
    }

    @Test
    fun dominantVerticalMovementResolvesTopAndBottom() {
        assertEquals(
            FlickDirection.TOP,
            resolver.resolve(origin, PointF2(106f, 60f)),
        )
        assertEquals(
            FlickDirection.BOTTOM,
            resolver.resolve(origin, PointF2(94f, 140f)),
        )
    }
}
