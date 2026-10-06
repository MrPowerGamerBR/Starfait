package com.mrpowergamerbr.starfait.runner

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BoundingBoxTest {
    @Test
    fun `test bounding box`() {
        val boundingBox = BoundingBox.fromWorldCoordinates(0f, 0f, 32f, 32f)
        assertEquals(0.0f, boundingBox.distanceTowardsPoint(2f, 2f)) // Inside the BoundingBox
        assertEquals(45.254833f, boundingBox.distanceTowardsPoint(64f, 64f))

        assertEquals(18f, boundingBox.distanceTowardsPoint(16f, 50f))  // directly below
        assertEquals(18f, boundingBox.distanceTowardsPoint(16f, -18f)) // directly above
        assertEquals(18f, boundingBox.distanceTowardsPoint(50f, 16f))  // directly right
        assertEquals(18f, boundingBox.distanceTowardsPoint(-18f, 16f)) // directly left

        // diagonals
        assertEquals(4.2426405f, boundingBox.distanceTowardsPoint(-3f, -3f)) // top left
        assertEquals(4.2426405f, boundingBox.distanceTowardsPoint(35f, -3f)) // top right
        assertEquals(4.2426405f, boundingBox.distanceTowardsPoint(35f, 35f)) // bottom right
        assertEquals(4.2426405f, boundingBox.distanceTowardsPoint(-3f, 35f)) // bottom left
    }
}