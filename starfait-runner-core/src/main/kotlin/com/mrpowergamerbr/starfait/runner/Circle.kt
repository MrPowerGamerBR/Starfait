package com.mrpowergamerbr.starfait.runner

/**
 * A circle
 */
class Circle(val x: Float, val y: Float, val radius: Float) {
    fun getBoundingBox(): BoundingBox {
        // Compute the bounding box of the circle
        val circleX1 = x - radius
        val circleX2 = x + radius
        val circleY1 = y - radius
        val circleY2 = y + radius

        return BoundingBox.fromLeftRightTopBottom(circleX1, circleX2, circleY1, circleY2)
    }

    fun contains(pointX: Float, pointY: Float): Boolean {
        val dx = pointX - x
        val dy = pointY - y
        return radius * radius >= dx * dx + dy * dy
    }
}