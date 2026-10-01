package com.mrpowergamerbr.starfait.runner

import kotlin.math.max
import kotlin.math.min

@ConsistentCopyVisibility
data class BoundingBox private constructor(
    val left: Float,
    val right: Float,
    val top: Float,
    val bottom: Float,
) {
    companion object {
        /**
         * Creates a BoundingBox and applies the correct coordinate order (right = larger X, bottom = larger Y)
         */
        fun fromWorldCoordinates(x1: Float, y1: Float, x2: Float, y2: Float) = BoundingBox(
            min(x1, x2),
            max(x1, x2),
            min(y1, y2),
            max(y1, y2)
        )
    }

    fun isCollidingWith(other: BoundingBox): Boolean {
        return left < other.right && right > other.left && top < other.bottom && bottom > other.top
    }

    fun contains(x: Float, y: Float): Boolean {
        return x >= left && x <= right && y >= top && y <= bottom
    }
}