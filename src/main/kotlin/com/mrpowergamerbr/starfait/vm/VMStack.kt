package com.mrpowergamerbr.starfait.vm

class VMStack {
    companion object {
        private val UNDEFINED = StackValue.Variable(RValue.Undefined)

        private val TRUE = StackValue.Boolean(true)
        private val FALSE = StackValue.Boolean(false)
    }

    // In the original YoYo Runner, the runner stores the values on the stack in a byte array directly
    // That's why the opcodes in the VM *need* to know which type kind they want to pop from the array, because the VM maps types -> bytes
    // We store all the values as tagged values, so we need to store which stack type was the RValue originally pushed as
    //
    // This is the same layout that Butterscotch uses, for better or for worse
    // We *could* mimic the original runner a bit more, by storing everything into a flat array + sidecar array for references
    // However, that does impact debuggability (you need to have ANOTHER sidecar array to store which types of values are stored for debugging)
    //
    // So it is better to do it like this, the only caveat is that some technically-valid-bytecode-ops-but-not-really are not going to work
    // For example: The original runner DOES allow you to DUP a float type while the top of the stack is a VARIABLE
    // However in Starfait it does NOT work... but then again, in the original runner it would've just copied garbage
    // So *technically* being like this is better for "correctness"
    val values = Array<StackValue>(1024) { UNDEFINED }
    var top = 0
        private set

    fun pushInt32(value: Int) = push(StackValue.Int32(value))
    fun pushInt64(value: Long) = push(StackValue.Int64(value))
    fun pushDouble(value: Double) = push(StackValue.Double(value))
    fun pushUndefinedVariable() = push(UNDEFINED)
    fun pushVariable(value: RValue) = if (value is RValue.Undefined) pushUndefinedVariable() else push(StackValue.Variable(value))
    fun pushString(value: String) = push(StackValue.String(value))
    fun pushBoolean(value: Boolean) = push(if (value) TRUE else FALSE)

    fun push(value: StackValue) {
        require(values.size != this.top) { "VM stack overflow!" }
        values[this.top++] = value
    }

    fun popInt32() = popWithType<StackValue.Int32>()
    fun popInt64() = popWithType<StackValue.Int64>()
    fun popDouble() = popWithType<StackValue.Double>()
    fun popVariable() = popWithType<StackValue.Variable>()
    fun popString() = popWithType<StackValue.String>()
    fun popBoolean() = popWithType<StackValue.Boolean>()

    fun popInt32AndUnwrap() = popWithType<StackValue.Int32>().value
    fun popInt64AndUnwrap() = popWithType<StackValue.Int64>().value
    fun popDoubleAndUnwrap() = popWithType<StackValue.Double>().value
    fun popVariableAndUnwrap() = popWithType<StackValue.Variable>().value
    fun popStringAndUnwrap() = popWithType<StackValue.String>().value
    fun popBooleanAndUnwrap() = popWithType<StackValue.Boolean>().value

    /**
     * Pops a value, checks if its stack size is 4 and, if it is, converts to a boolean
     */
    fun popBooleanLikeAndUnwrap(): Boolean {
        // Used for branching
        val pop = pop()
        check(pop.dataType.stackSize == 4.toByte())
        return pop.convertToBoolean()
    }

    // This looks similar to the "pop" below, but the advantage of this is that we catch bugs early if the data type is wrong
    fun popByInstructionType(dataType: InstructionDataType): StackValue {
        return when (dataType) {
            InstructionDataType.DOUBLE -> popDouble()
            InstructionDataType.FLOAT -> TODO()
            InstructionDataType.INT32 -> popInt32()
            InstructionDataType.INT64 -> popInt64()
            InstructionDataType.BOOLEAN -> popBoolean()
            InstructionDataType.VARIABLE -> popVariable()
            InstructionDataType.STRING -> popString()
            InstructionDataType.INT16 -> TODO()
        }
    }

    private inline fun <reified T> popWithType(): T {
        val value = pop()
        return value as? T ?: error("Incorrect value on the pop! I wanted ${T::class} but the type was $value")
    }

    // This should NEVER EVER EVER be used outside of the VMStack, because if we are using, we aren't respecting the original runner behavior!
    private fun pop(): StackValue {
        require(top != 0) { "VM stack underflow!" }
        val position = --top
        val value = values[position]
        return value
    }
}