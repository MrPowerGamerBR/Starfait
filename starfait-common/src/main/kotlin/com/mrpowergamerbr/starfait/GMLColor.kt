package com.mrpowergamerbr.starfait

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

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

        // Based on GameMaker-HTML5's "make_color_hsv" implementation
        // The only twist is that the native code clamps the value using bitwise operations BEFORE converting
        // For reference (based on the Android runner):
        // F_MakeColorHSV: It converts from Reals to Int using YYGetInt32, then calls Color_HSV
        // Color_HSV: Clamps the input values from 0 to 255 by doing bitwise operations
        // Color_HSVToColor: Essentially just calls Color_HSVToRGB
        // Color_HSVToRGB: The REAL implementation that converts the values to HSV (which is the same as the one in GameMaker-HTML5)
        fun fromHSV(hue: Int, saturation: Int, value: Int): GMLColor {
            // Color_HSV
            // The original code seems to pack it into a single value, but we don't need to do that
            val hueClamped = hue and 0xFF
            val saturationClamped = saturation and 0xFF
            val valueClamped = value and 0xFF

            // Color_HSVToRGB
            // This part of the code is *similar* to how GameMaker-HTML5 looks like
            var rr: Double
            var gg: Double
            var bb: Double

            var hh = (hueClamped * 360.0 / 255.0)
            if (hh == 360.0) {
                hh = 0.0
            }
            val ss = saturationClamped / 255.0
            val vv = valueClamped / 255.0

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

            return fromRGB(MathUtils.delphiRound(rr), MathUtils.delphiRound(gg), MathUtils.delphiRound(bb))
        }

        /**
         * Lerps (lineraly interpolates) between two colors
         */
        fun lerp(color1: GMLColor, color2: GMLColor, amount: Double): GMLColor {
            val r = (color2.red - color1.red) * amount + color1.red
            val g = (color2.green - color1.green) * amount + color1.green
            val b = (color2.blue - color1.blue) * amount + color1.blue
            // Yes, they use DelphiRound for the lerp
            return fromRGB(MathUtils.delphiRound(r), MathUtils.delphiRound(g), MathUtils.delphiRound(b))
        }
    }

    val red: Int get() = value and 0xFF
    val green: Int get() = (value shr 8) and 0xFF
    val blue: Int get() = (value shr 16) and 0xFF

    val redF: Float get() = red / 255f
    val greenF: Float get() = green / 255f
    val blueF: Float get() = blue / 255f
}