package com.mrpowergamerbr.starfait

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

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

        // From GameMaker-HTML5's "make_color_hsv" implementation
        fun fromHSV(hue: Float, saturation: Float, value: Float): GMLColor {
            var rr: Double
            var gg: Double
            var bb: Double

            var hh = hue * 360.0 / 255.0
            if (hh == 360.0) hh = 0.0
            val ss = saturation / 255.0
            val vv = value / 255.0

            if (ss == 0.0) {
                rr = vv
                gg = vv
                bb = vv
            } else {
                hh = hh / 60.0
                val i = floor(hh).toInt()
                val f = hh - i
                val p = vv * (1.0 - ss)
                val q = vv * (1.0 - (ss * f))
                val t = vv * (1.0 - (ss * (1.0 - f)))
                when (i) {
                    0 -> {
                        rr = vv
                        gg = t
                        bb = p
                    }

                    1 -> {
                        rr = q
                        gg = vv
                        bb = p
                    }

                    2 -> {
                        rr = p
                        gg = vv
                        bb = t
                    }

                    3 -> {
                        rr = p
                        gg = q
                        bb = vv
                    }

                    4 -> {
                        rr = t
                        gg = p
                        bb = vv
                    }

                    else -> {
                        rr = vv
                        gg = p
                        bb = q
                    }
                }
            }

            rr = max(0.0, min(255.0, floor((rr * 255.0) + 0.5)))
            gg = max(0.0, min(255.0, floor((gg * 255.0) + 0.5)))
            bb = max(0.0, min(255.0, floor((bb * 255.0) + 0.5)))

            return fromRGB(rr.toInt(), gg.toInt(), bb.toInt())
        }
    }

    val red: Int get() = value and 0xFF
    val green: Int get() = (value shr 8) and 0xFF
    val blue: Int get() = (value shr 16) and 0xFF

    val redF: Float get() = red / 255f
    val greenF: Float get() = green / 255f
    val blueF: Float get() = blue / 255f
}