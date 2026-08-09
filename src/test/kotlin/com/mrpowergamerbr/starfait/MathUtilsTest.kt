package com.mrpowergamerbr.starfait

import kotlin.test.Test

class MathUtilsTest {
    @Test
    fun `test delphi rounding`() {
        assert(MathUtils.delphiRound(2.5) == 2)
        assert(MathUtils.delphiRound(3.5) == 4)
        assert(MathUtils.delphiRound(23.5) == 24)
        assert(MathUtils.delphiRound(24.5) == 24)
        assert(MathUtils.delphiRound(-23.5) == -24)
        assert(MathUtils.delphiRound(-24.5) == -24)
    }
}