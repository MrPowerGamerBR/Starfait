package com.mrpowergamerbr.starfait.runner

data class BoundingBox(
    val left: Float,
    val right: Float,
    val top: Float,
    val bottom: Float,
) {
    fun isCollidingWith(other: BoundingBox): Boolean {
        return left < other.right && right > other.left && top < other.bottom && bottom > other.top
    }

    fun contains(x: Float, y: Float): Boolean {
        return x >= left && x <= right && y >= top && y <= bottom
    }
}