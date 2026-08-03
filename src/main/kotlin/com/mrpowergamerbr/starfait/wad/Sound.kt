package com.mrpowergamerbr.starfait.wad

class Sound(
    val name: StringPointer,
    val kind: Kind,
    val fileType: StringPointer,
    val fileName: StringPointer,
    val effects: Int,
    val volume: Float,
    val pan: Float,
    val preload: Boolean,
    val audoIndex: Int
) {
    enum class Kind(val value: Int) {
        STREAMED(100),
        UNCOMPRESSED(101),
        COMPRESSED(102),
        UNCOMPRESSED_ON_LOAD(103);

        companion object {
            fun fromValue(value: Int): Kind {
                return entries.firstOrNull { it.value == value } ?: error("Unknown sound kind: $value")
            }
        }
    }
}