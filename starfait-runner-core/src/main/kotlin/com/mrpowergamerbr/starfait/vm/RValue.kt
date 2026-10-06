package com.mrpowergamerbr.starfait.vm

import com.mrpowergamerbr.starfait.MathUtils

sealed class RValue {
    abstract fun toStringTyped(): kotlin.String

    // You may be thinking: "Couldn't we add a quick and easy (this as? Int32)?.value for things that we are SURE won't be a different type?"
    // Well, I thought the same thing, but it seems that a lot of places that you THINK would use only int32 (like instanceIds on the VM) use sometimes int32 and sometimes reals
    // So better make everything go through the converter functions
    open fun convertToInt32(): Int = error("Type $this can't be converted to int32!")
    open fun convertToInt64(): Long = error("Type $this can't be converted to int64!")
    open fun convertToReal(): Double = error("Type $this can't be converted to real!")
    open fun convertToGMLString(): kotlin.String = error("Type $this can't be converted to string!")
    open fun convertToBoolean(): kotlin.Boolean = error("Type $this can't be converted to boolean!")

    data object Undefined : RValue() {
        override fun toStringTyped() = "undefined"

        override fun convertToGMLString() = "undefined"
        override fun convertToInt32() = 0
        override fun convertToReal() = 0.0
    }

    data class String(val value: kotlin.String) : RValue() {
        override fun toStringTyped() = "string(\"$value\")"

        override fun convertToGMLString() = value
        override fun convertToReal() = MathUtils.strtod(value)
    }

    data class Int32(val value: Int) : RValue() {
        override fun toStringTyped() = "int32($value)"

        override fun convertToGMLString() = value.toString()
        override fun convertToInt32() = value
        override fun convertToInt64() = value.toLong()
        override fun convertToReal() = value.toDouble()
        override fun convertToBoolean() = MathUtils.convertInt32ToBoolean(value)
    }

    data class Int64(val value: Long) : RValue() {
        override fun toStringTyped() = "int64($value)"

        override fun convertToGMLString() = value.toString()
        override fun convertToInt32() = value.toInt()
        override fun convertToInt64() = value
        override fun convertToReal() = value.toDouble()
    }

    data class Real(val value: Double) : RValue() {
        override fun toStringTyped() = "real($value)"

        override fun convertToGMLString() = MathUtils.formatReal(value)
        override fun convertToReal() = value
        override fun convertToInt32() = value.toInt()
        override fun convertToBoolean() = value > 0.5 // GameMaker-HTML5's yyGetBool returns true if the value is > 0.5
    }

    data class Boolean(val value: kotlin.Boolean) : RValue() {
        override fun toStringTyped() = "boolean($value)"

        override fun convertToGMLString() = if (value) "1" else "0"
        override fun convertToBoolean() = value
        override fun convertToReal() = if (value) 1.0 else 0.0
    }

    data class Array(val value: GMLArray) : RValue() {
        override fun toStringTyped() = "array($value)"

        override fun convertToGMLString() = TODO()
    }
}