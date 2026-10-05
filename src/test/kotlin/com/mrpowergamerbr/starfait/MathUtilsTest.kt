package com.mrpowergamerbr.starfait

import kotlin.test.Test
import kotlin.test.assertEquals

class MathUtilsTest {
    @Test
    fun `test delphi rounding`() {
        assertEquals(2, MathUtils.delphiRound(2.5))
        assertEquals(4, MathUtils.delphiRound(3.5))
        assertEquals(24, MathUtils.delphiRound(23.5))
        assertEquals(24, MathUtils.delphiRound(24.5))
        assertEquals(-24, MathUtils.delphiRound(-23.5))
        assertEquals(-24, MathUtils.delphiRound(-24.5))
    }

    @Test
    fun `test strtod parsing`() {
        assertEquals(10.0, MathUtils.strtod("10.0"))
        assertEquals(10.5, MathUtils.strtod("10.5"))
        assertEquals(10.0, MathUtils.strtod("10,0"))
        assertEquals(10.0, MathUtils.strtod("10,5"))
        assertEquals(0.0, MathUtils.strtod(""))
        assertEquals(0.0, MathUtils.strtod("          "))
        assertEquals(4002.8922, MathUtils.strtod("  4002.8922"))
        assertEquals(4002.8922, MathUtils.strtod("4002.8922  "))
        assertEquals(0.0, MathUtils.strtod("Loritta is so cute!"))
        assertEquals(0.0, MathUtils.strtod("Loritta 4562.4562"))
        assertEquals(4562.4562, MathUtils.strtod("4562.4562 Loritta"))
        assertEquals(0.5, MathUtils.strtod(".5"))
        assertEquals(0.0, MathUtils.strtod("..5"))
        assertEquals(0.0, MathUtils.strtod("a9"))
        assertEquals(9.0, MathUtils.strtod("9a"))
        assertEquals(9.0, MathUtils.strtod("9a.5"))
        assertEquals(45.0, MathUtils.strtod("45 62"))
        assertEquals(0.0, MathUtils.strtod(".   5"))
        assertEquals(0.005, MathUtils.strtod(".005"))
    }
}
