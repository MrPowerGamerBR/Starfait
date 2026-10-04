package com.mrpowergamerbr.starfait.vm

import com.mrpowergamerbr.starfait.MathUtils

sealed class StackValue {
    abstract val dataType: InstructionDataType
    abstract fun toStringTyped(): kotlin.String

    open fun convertToGMLString(): kotlin.String = error("Type $this can't be converted to string!")
    open fun convertToReal(): kotlin.Double = error("Type $this can't be converted to real!")
    open fun convertToInt32(): Int = error("Type $this can't be converted to int32!")
    open fun convertToInt64(): Long = error("Type $this can't be converted to int64!")
    open fun convertToBoolean(): kotlin.Boolean = error("Type $this can't be converted to boolean!")
    abstract fun convertToVariable(): RValue

    fun arithmeticType() = when (this) {
        is Boolean -> ArithmeticType.REAL
        is Double -> ArithmeticType.REAL
        is Int32 -> ArithmeticType.INT32
        is Int64 -> ArithmeticType.INT64
        is String -> ArithmeticType.STRING
        is Variable -> when (val value = this.value) {
            is RValue.Int32 -> ArithmeticType.INT32
            is RValue.Int64 -> ArithmeticType.INT64
            is RValue.Real -> ArithmeticType.REAL
            is RValue.String -> ArithmeticType.STRING
            else -> error("Type $value does not support arithmetic!")
        }
    }

    fun cmpType() = when (this) {
        is Boolean -> CmpType.REAL
        is Double -> CmpType.REAL
        is Int32 -> CmpType.INT32
        is Int64 -> CmpType.INT64
        is String -> CmpType.STRING
        is Variable -> when (val value = this.value) {
            is RValue.Int32 -> CmpType.INT32
            is RValue.Int64 -> CmpType.INT64
            is RValue.Real -> CmpType.REAL
            is RValue.String -> CmpType.STRING
            is RValue.Undefined -> CmpType.INT32
            is RValue.Boolean -> CmpType.INT32
            else -> error("Type $value does not support cmp!")
        }
    }

    class Int32(val value: Int) : StackValue() {
        override val dataType = InstructionDataType.INT32
        override fun toStringTyped() = "${dataType.name.lowercase()}($value)"

        override fun convertToVariable() = RValue.Int32(value)
        override fun convertToReal() = value.toDouble()
        override fun convertToInt32() = value
        override fun convertToInt64() = value.toLong()
        override fun convertToBoolean() = MathUtils.convertInt32ToBoolean(value)
    }

    class Int64(val value: Long) : StackValue() {
        override val dataType = InstructionDataType.INT64
        override fun toStringTyped() = "${dataType.name.lowercase()}($value)"

        override fun convertToVariable() = RValue.Int64(value)
        override fun convertToInt32() = value.toInt()
        override fun convertToInt64() = value
    }

    class Double(val value: kotlin.Double) : StackValue() {
        override val dataType = InstructionDataType.DOUBLE
        override fun toStringTyped() = "${dataType.name.lowercase()}($value)"

        override fun convertToVariable() = RValue.Real(value)
        override fun convertToReal() = value
    }

    class Variable(val value: RValue) : StackValue() {
        override val dataType = InstructionDataType.VARIABLE
        override fun toStringTyped() = "${dataType.name.lowercase()}(${value.toStringTyped()})"

        override fun convertToVariable() = value
        override fun convertToGMLString() = value.convertToGMLString()
        override fun convertToReal() = value.convertToReal()
        override fun convertToInt32() = value.convertToInt32()
        override fun convertToInt64() = value.convertToInt64()
        override fun convertToBoolean() = value.convertToBoolean()
    }

    class String(val value: kotlin.String) : StackValue() {
        override val dataType = InstructionDataType.STRING
        override fun toStringTyped() = "${dataType.name.lowercase()}(\"$value\")"

        override fun convertToVariable() = RValue.String(value)
        override fun convertToGMLString() = value
    }

    class Boolean(val value: kotlin.Boolean) : StackValue() {
        override val dataType = InstructionDataType.BOOLEAN
        override fun toStringTyped() = "${dataType.name.lowercase()}($value)"

        override fun convertToVariable() = RValue.Boolean(value)
        override fun convertToBoolean() = value
        override fun convertToReal() = if (value) 1.0 else 0.0
    }
}