package com.mrpowergamerbr.starfait.wad

import kotlin.math.ceil

/**
 * Helper for reading GameMaker's collision masks
 *
 * The collision mask format is an MSB (most significant bit), each "row" of the image is the width of the sprite, rounded to the `ceil` 8
 *
 * Each pixel of the mask is "packed" in a bit, so the first bit contains the first (0 until 8) pixels of the mask on the X axis
 *
 * Example: If you have an 8x1 sprite, alternating "opaque" and "transparent" pixels, the mask will look like this: `10101010`
 */
@JvmInline
value class CollisionMask(val data: ByteArray) {
    /**
     * Checks if the pixel at [x], [y] on the mask is "solid" (bit is set to 1)
     *
     * If the coordinates are out of bounds, it will return false
     *
     * @param maskWidth the width of the sprite that holds this mask
     * @param maskHeight the height of the sprite that holds this mask
     * @param x the x coordinate that will be checked
     * @param y the y coordinate that will be checked
     * @return true if the pixel is solid, false otherwise
     */
    fun isPixelSolid(maskWidth: Int, maskHeight: Int, x: Int, y: Int): Boolean {
        if (x !in 0 until maskWidth || y !in 0 until maskHeight)
            return false

        val maskWidthRoundedToNearest8 = (ceil(maskWidth / 8.0) * 8).toInt()

        val packedPixel = data[(x + (y * maskWidthRoundedToNearest8)) / 8]
        // We do 7 here because we want 0..7
        val bitToCheck = 7 - (x % 8)
        val isSet = packedPixel.toInt() and (1 shl bitToCheck) != 0
        return isSet
    }
}