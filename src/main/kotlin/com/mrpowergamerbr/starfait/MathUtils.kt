package com.mrpowergamerbr.starfait

import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sign

object MathUtils {
    const val EPSILON = 1e-5

    /**
     * Formats the [value] to be a string, matching YoYo Runner's behavior
     */
    fun formatReal(value: Double): String {
        // TODO: The behavior of these values depend a lot on the runner version AND on the runner platform
        if (value.isNaN())
            return "NaN"
        if (value.isInfinite())
            return if (0 > value) "-inf" else "inf"

        val asLong = value.toLong()
        if (asLong.toDouble() == value)
            return asLong.toString()

        return String.format(Locale.ROOT, "%.2f", value)
    }

    // From GameMaker-HTML5, this is also called "Round" and "round" in GM-HTML5's codebase
    fun delphiRound(_a: Float) = delphiRound(_a.toDouble())
    fun delphiRound(_a: Double): Int {
        val i = (sign(_a) * floor(abs(_a))).toInt()

        if (_a < 0) {
            val f = _a - i
            if ((i and 1) == 1)
            {
                if (f <= -0.5) {
                    return i - 1
                } else {
                    return i
                }
            } else {
                if (f >= -0.5) {
                    return i
                } else {
                    return i - 1
                }
            }
        } else {
            val f = _a - i
            if ((i and 1) == 1) {
                if (f >= 0.5) {
                    return i + 1
                } else {
                    return i
                }
            } else {
                if (f <= 0.5) {
                    return i
                } else {
                    return i + 1
                }
            }
        }
    }
}