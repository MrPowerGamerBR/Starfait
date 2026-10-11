package com.mrpowergamerbr.starfait.runner

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

@ConsistentCopyVisibility
data class BoundingBox private constructor(
    val left: Float,
    val right: Float,
    val top: Float,
    val bottom: Float,
) {
    companion object {
        /**
         * Creates a [BoundingBox] and applies the correct coordinate order (right = larger X, bottom = larger Y)
         */
        fun fromWorldCoordinates(x1: Float, y1: Float, x2: Float, y2: Float) = BoundingBox(
            min(x1, x2),
            max(x1, x2),
            min(y1, y2),
            max(y1, y2)
        )

        /**
         * Creates a [BoundingBox] without any coordinate order checks
         */
        fun fromLeftRightTopBottom(left: Float, right: Float, top: Float, bottom: Float) = BoundingBox(left, right, top, bottom)
    }

    fun isCollidingWith(other: BoundingBox, collisionCompatibilityMode: Boolean): Boolean {
        // This is from GameMaker-HTML5's "Collision_Instance"
        // Why that is for "To avoid floating point inaccuracies"? I have no idea
        var col_delta = 0.0 //To avoid floating point inaccuracies
        if (collisionCompatibilityMode) {
            col_delta = 1.0 //or in compat mode to go back to how it was
        }

        val bbox1 = this
        val bbox2 = other

        if (bbox1.left          >= (bbox2.right + col_delta))   return false
        if ((bbox1.right + col_delta)   <= bbox2.left)          return false
        if (bbox1.top           >= (bbox2.bottom + col_delta))  return false
        if ((bbox1.bottom + col_delta)  <= bbox2.top)           return false
        return true
    }

    fun isCollidingWithCircle(circle: Circle): Boolean {
        // Closest point on the box to the circle's center
        val closestX = max(this.left, min(circle.x, this.right))
        val closestY = max(this.top, min(circle.y, this.bottom))
        return circle.contains(closestX, closestY)
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

    fun distanceTowardsBoundingBox(otherBBox: BoundingBox) = sqrt(distanceTowardsBoundingBoxSquared(otherBBox))

    fun distanceTowardsBoundingBoxSquared(otherBBox: BoundingBox): Double {
        val selfBBox = this

        var xd = 0.0
        var yd = 0.0
        if (otherBBox.left > selfBBox.right) xd = (otherBBox.left - selfBBox.right).toDouble()
        if (selfBBox.left > otherBBox.right) xd = (otherBBox.right - selfBBox.left).toDouble()
        if (otherBBox.top > selfBBox.bottom) yd = (otherBBox.top - selfBBox.bottom).toDouble()
        if (selfBBox.top > otherBBox.bottom) yd = (otherBBox.bottom - selfBBox.top).toDouble()
        val d = xd * xd + yd * yd

        return d
    }

    fun distanceTowardsPoint(targetX: Float, targetY: Float) = sqrt(distanceTowardsPointSquared(targetX, targetY))

    fun distanceTowardsPointSquared(targetX: Float, targetY: Float): Float {
        var distanceX = 0.0f
        var distanceY = 0.0f

        // The reason we do it like this is because, if the point is INSIDE the bounding box, it MUST be 0.0
        // Essentially you need to see that the "gap" in the BoundingBox falls through
        // And we can't use "contains" because we NEED that fallthrough to happen
        // If we just use "else" (example: on the first statement -> else), it won't work for values like...
        // bbox(32, 32) -> distanceTowardsPoint(24, 50)
        // Technically not contained, but the targetX is inside the bounding box
        // But because we were using else, the distanceX would NOT be 0.0f
        if (this.left > targetX) distanceX = targetX - this.left
        if (targetX > this.right) distanceX = targetX - this.right
        if (this.top > targetY) distanceY = targetY - this.top
        if (targetY > this.bottom) distanceY = targetY - this.bottom

        return distanceX.pow(2) + distanceY.pow(2)
    }
}
