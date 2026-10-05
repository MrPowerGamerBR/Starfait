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
}