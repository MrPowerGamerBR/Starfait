package com.mrpowergamerbr.starfait.wad

import kotlin.math.floor

class Sprite(
    val name: StringPointer,
    val width: Int,
    val height: Int,
    val bboxLeft: Int,
    val bboxRight: Int,
    val bboxBottom: Int,
    val bboxTop: Int,
    val transparent: Boolean,
    val smooth: Boolean,
    val preload: Boolean,
    val bboxMode: Int,
    val precise: Boolean,
    val originX: Int,
    val originY: Int,
    val type: Type,
    val frames: List<TexturePageEntryPointer>,
    // One collision mask per entry, each ((width + 7) / 8) * height bytes, one bit per pixel
    val collisionMasks: List<ByteArray>
) {
    val hasCollisionMask get() = precise && collisionMasks.isNotEmpty()

    // See GameMaker-HTML5's "ColMaskSet"
    // The WAD stores masks over the whole sprite, instead of only over the bounding box
    fun colMaskSet(u: Double, v: Double, imageIndex: Int, floorInput: Boolean): Boolean {
        val mask = collisionMasks[imageIndex]

        val intU: Int
        val intV: Int
        if (floorInput) {
            intU = floor(u).toInt()
            intV = floor(v).toInt()
        } else {
            intU = u.toInt()
            intV = v.toInt()
        }

        if (bboxLeft > intU || intU > bboxRight) return false
        if (bboxTop > intV || intV > bboxBottom) return false

        val maskWidth = (width + 7) shr 3
        val maskByte = mask[intV * maskWidth + (intU shr 3)].toInt()

        return (maskByte and (1 shl (7 - (intU and 7)))) != 0
    }

    enum class Type(val value: Int) {
        NORMAL(0),
        SWF(1),
        SPINE(2);

        companion object {
            fun fromValue(value: Int): Type {
                return entries.firstOrNull { it.value == value } ?: error("Unknown sprite type: $value")
            }
        }
    }
}
