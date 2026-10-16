package com.mrpowergamerbr.starfait

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToLong
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

        val hundredths = (abs(value) * 100).roundToLong()
        val sign = if (0 > value) "-" else ""
        return "$sign${hundredths / 100}.${(hundredths % 100).toString().padStart(2, '0')}"
    }

    /**
     * Reimplementation of `strtod` in Kotlin
     *
     * @param input the input string
     * @return the double
     */
    fun strtod(input: String): Double {
        var position = 0

        return strtodStream {
            if (position == input.length)
                return@strtodStream -1

            input[position++].code
        }
    }

    /**
     * Reimplementation of `strtod` in Kotlin
     *
     * @param input the input string
     * @return the double
     */
    fun strtodStream(input: () -> (Int)): Double {
        val whole = StringBuilder()
        val decimal = StringBuilder()

        var parsingWhole = true
        var foundSomethingInWhole = false

        while (true) {
            val byte = input.invoke()

            if (byte == -1)
                break

            val char = byte.toChar()

            if (parsingWhole) {
                // Continue until we find something
                if (char.isWhitespace()) {
                    if (foundSomethingInWhole)
                        break
                    else
                        continue
                } else if (char == '.') {
                    parsingWhole = false
                    continue
                } else if (char.isDigit()) {
                    foundSomethingInWhole = true
                    whole.append(char)
                } else {
                    // Anything else the parser just bails
                    break
                }
            } else {
                if (char.isDigit()) {
                    decimal.append(char)
                } else {
                    break
                }
            }
        }

        return try { ("$whole.$decimal").toDouble() } catch (e: NumberFormatException) { 0.0 }
    }

    // Sqr in GameMaker-HTML5
    fun squared(x: Float) = x * x
    fun squared(x: Double) = x * x

    fun clampFloat(_f: Double): Double = (_f * 1_000_000).toInt() / 1_000_000.0

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

    fun convertInt32ToBoolean(value: Int) = value > 0
}