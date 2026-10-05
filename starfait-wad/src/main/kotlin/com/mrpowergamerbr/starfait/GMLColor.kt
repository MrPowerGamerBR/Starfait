package com.mrpowergamerbr.starfait

/**
 * A color in the BGR format
 */
@JvmInline
value class GMLColor(val value: Int) {
    companion object {
        fun fromRGB(red: Int, green: Int, blue: Int): GMLColor {
            var value = 0
            value = ((blue and 0xFF) shl 16) or value
            value = ((green and 0xFF) shl 8) or value
            value = ((red and 0xFF)) or value

            return GMLColor(value)
        }
    }

    val red: Int get() = value and 0xFF
    val green: Int get() = (value shr 8) and 0xFF
    val blue: Int get() = (value shr 16) and 0xFF

    val redF: Float get() = red / 255f
    val greenF: Float get() = green / 255f
    val blueF: Float get() = blue / 255f
}