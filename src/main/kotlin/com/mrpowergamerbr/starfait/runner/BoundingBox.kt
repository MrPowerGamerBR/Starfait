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

    /**
     * Liang-Barsky clip of the segment (x1,y1)-(x2,y2) against this box (edges inclusive).
     */
    fun intersectsLine(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        val dx = x2 - x1
        val dy = y2 - y1
        var t0 = 0f
        var t1 = 1f

        val p = floatArrayOf(-dx, dx, -dy, dy)
        val q = floatArrayOf(x1 - left, right - x1, y1 - top, bottom - y1)

        for (i in 0 until 4) {
            if (0f == p[i]) {
                // Parallel to this edge pair; outside means no hit
                if (0f > q[i])
                    return false
            } else {
                val t = q[i] / p[i]
                if (0f > p[i]) {
                    if (t > t1) return false
                    if (t > t0) t0 = t
                } else {
                    if (t0 > t) return false
                    if (t1 > t) t1 = t
                }
            }
        }
        return true
    }
}
