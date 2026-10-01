package com.mrpowergamerbr.starfait.vm

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VMStackTest {
    @Test
    fun `test push`() {
        val stack = VMStack()
        stack.push(RValue.Undefined)
        require(stack.top == 1)
    }

    @Test
    fun `test empty peek exception`() {
        val stack = VMStack()
        val ex = assertFailsWith<IllegalArgumentException> {
            stack.peek()
        }
        assertEquals("You can't peek an empty stack!", ex.message)
    }

    @Test
    fun `test peek out of bounds exception`() {
        val stack = VMStack()
        val ex = assertFailsWith<IllegalArgumentException> {
            stack.peekAt(Int.MAX_VALUE)
        }
        assertContains(ex.message!!, "Trying to peek out of bounds!")
    }
}