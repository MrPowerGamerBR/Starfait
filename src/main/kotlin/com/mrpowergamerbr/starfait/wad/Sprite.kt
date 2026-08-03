package com.mrpowergamerbr.starfait.wad

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
