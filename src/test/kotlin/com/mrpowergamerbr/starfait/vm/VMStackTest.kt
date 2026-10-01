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

    @Test
    fun `test push with type`() {
        val stack = VMStack()
        stack.pushVariable(RValue.String("Loritta is so cute!!!"))
        require(stack.top == 1)
        val unwrapped = stack.popVariableAndUnwrap()
        require(unwrapped.convertToGMLString() == "Loritta is so cute!!!")
    }

    @Test
    fun `test boolean-like pop`() {
        val stack = VMStack()
        stack.pushBoolean(true)
        require(stack.popBooleanLikeAndUnwrap())
        stack.pushInt32(1)
        require(stack.popBooleanLikeAndUnwrap())
    }

    @Test
    fun `test pop by instruction type`() {
        val stack = VMStack()
        fun pushAndPop(type: InstructionDataType, popCall: () -> (Unit)) {
            popCall()
            stack.popByInstructionType(type)
        }

        pushAndPop(InstructionDataType.INT32) { stack.pushInt32(1234) }
        pushAndPop(InstructionDataType.INT64) { stack.pushInt64(30032017) }
        pushAndPop(InstructionDataType.STRING) { stack.pushString("Loritta is so cute!!!") }
        pushAndPop(InstructionDataType.BOOLEAN) { stack.pushBoolean(true) }
        pushAndPop(InstructionDataType.DOUBLE) { stack.pushDouble(1234.5678) }
        pushAndPop(InstructionDataType.VARIABLE) { stack.pushVariable(RValue.String("ayaya")) }
        require(stack.top == 0)
    }
}