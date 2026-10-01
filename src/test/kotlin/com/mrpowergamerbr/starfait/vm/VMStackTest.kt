package com.mrpowergamerbr.starfait.vm

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VMStackTest {
    @Test
    fun `test push`() {
        val stack = VMStack()
        stack.pushUndefinedVariable()
        require(stack.top == 1)
    }
}