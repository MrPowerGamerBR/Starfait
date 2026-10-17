package com.mrpowergamerbr.starfait.wad

import kotlin.math.ceil
import kotlin.math.floor

class Sprite(
    val name: String,
    val width: Int,
    val height: Int,
    val bboxLeft: Int,
    val bboxRight: Int,
    val bboxBottom: Int,
    val bboxTop: Int,
    val transparent: Boolean,
    val smooth: Boolean,
    val preload: Boolean,
    val bboxMode: BoundingBoxMode,
    val precise: Boolean,
    val originX: Int,
    val originY: Int,
    val type: Type,
    val frames: List<TexturePageEntryPointer>,
    val collisionMasks: List<CollisionMask>
) {
    // Unlike GameMaker-HTML5, where only precise sprites get a mask built, the WAD stores masks for every sprite
    // So "has a mask" is decided by the precise flag
    val hasCollisionMask get() = precise && collisionMasks.isNotEmpty()

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

    enum class BoundingBoxMode(val value: Int) {
        AUTOMATIC(0),
        FULL_IMAGE(1),
        MANUAL(2);

        companion object {
            fun fromValue(value: Int): BoundingBoxMode {
                return entries.firstOrNull { it.value == value } ?: error("Unknown bounding box mode: $value")
            }
        }
    }
}
