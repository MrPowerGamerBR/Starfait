package com.mrpowergamerbr.starfait

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

object GMLColorTest {
    @Test
    fun `test fromRGB`() {
        assertEquals("00ff0000", GMLColor.fromRGB(0, 0, 255).value.toHexString())
        assertEquals("0000ff00", GMLColor.fromRGB(0, 255, 0).value.toHexString())
        assertEquals("000000ff", GMLColor.fromRGB(255, 0, 0).value.toHexString())
        assertEquals(GMLColor.fromRGB(41, 166, 254).value, 16688681)
    }

    @Test
    fun `test lerp`() {
        assertEquals(8344340, GMLColor.lerp(GMLColor.fromRGB(41, 166, 254), GMLColor.fromRGB(0, 0, 0), 0.5).value)
        assertEquals(11695133, GMLColor.lerp(GMLColor.fromRGB(41, 166, 254), GMLColor.fromRGB(0, 0, 0), 0.3).value)
        assertEquals(4993548, GMLColor.lerp(GMLColor.fromRGB(41, 166, 254), GMLColor.fromRGB(0, 0, 0), 0.7).value)
        assertEquals(154327, GMLColor.lerp(GMLColor.fromRGB(41, 166, 254), GMLColor.fromRGB(0, 0, 0), 2.0).value)
    }
}