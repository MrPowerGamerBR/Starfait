package com.mrpowergamerbr.starfait

import java.util.Locale

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
}