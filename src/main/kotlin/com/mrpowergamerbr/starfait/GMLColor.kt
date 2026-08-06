package com.mrpowergamerbr.starfait

/**
 * A color in the BGR format
 */
@JvmInline
value class GMLColor(val value: Int) {
    val red: Int get() = value and 0xFF
    val green: Int get() = (value shr 8) and 0xFF
    val blue: Int get() = (value shr 16) and 0xFF
}