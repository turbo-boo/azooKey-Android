package dev.turboboo.azookey.core

import kotlin.math.abs
import kotlin.math.hypot

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

/*
 * Direction selection is ported from azooKey's CGPoint.direction(to:) behavior.
 *
 * Upstream:
 *   azooKey/azooKey
 *   AzooKeyCore/Sources/KeyboardViews/extension CGPoint.swift
 * Original author: Keita Miwa (ensan)
 * Created: 2020-04-09
 *
 * Copyright (c) 2020-2023 Keita Miwa (ensan).
 * Licensed under the MIT License. See THIRD_PARTY_NOTICES.md.
 */
class FlickDirectionResolver(
    private val thresholdPx: Float,
) {
    init {
        require(thresholdPx >= 0f)
    }

    fun resolve(start: PointF2, end: PointF2): FlickDirection {
        val dx = end.x - start.x
        val dy = end.y - start.y

        if (hypot(dx.toDouble(), dy.toDouble()) < thresholdPx) {
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
        if (dy < 0f && abs(dx) < -dy) {
            return FlickDirection.TOP
        }

        // Match azooKey's boundary fallback.
        return FlickDirection.TOP
    }
}
