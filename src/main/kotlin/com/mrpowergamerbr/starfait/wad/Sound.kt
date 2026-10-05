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
        COMPRESSED_STREAMED(100),
        UNCOMPRESSED_NOT_STREAMED(101),
        COMPRESSED_NOT_STREAMED(102),
        COMPRESSED_UNCOMPRESS_ON_LOAD_NOT_STREAMED(103);

        companion object {
            fun fromValue(value: Int): Kind {
                return entries.firstOrNull { it.value == value } ?: error("Unknown sound kind: $value")
            }
        }
    }
}