package dev.turboboo.azookey.core

import kotlin.math.abs

/*
 * Direction classification is adapted from azooKey.
 *
 * Original implementation:
 *   azooKey/azooKey
 *   AzooKeyCore/Sources/KeyboardViews/extension CGPoint.swift
 * Original author: Keita Miwa (ensan)
 * License: MIT
 *
 * Android-specific threshold handling and data types are original to this port.
 */

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
        require(thresholdPx > 0f) { "thresholdPx must be positive" }
    }

    fun resolve(start: PointF2, end: PointF2): FlickDirection {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val distanceSquared = dx * dx + dy * dy

        if (distanceSquared < thresholdPx * thresholdPx) {
            return FlickDirection.CENTER
        }

        if (dx > 0f && abs(dy) < dx) {
            return FlickDirection.RIGHT
        }
        if (dx < 0f && abs(dy) < -dx) {
            return FlickDirection.LEFT
        }
        if (dy > 0f && abs(dx) < dy) {
            return FlickDirection.BOTTOM
        }

        // Match azooKey's direction behavior for the remaining case,
        // including exact diagonals.
        return FlickDirection.TOP
    }
}
