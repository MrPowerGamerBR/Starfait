package com.mrpowergamerbr.starfait.vm

import com.mrpowergamerbr.starfait.MathUtils

// Yes this looks wonky as hell, but that's because we are trying to squeeze performance from the JVM (to avoid unnecessary allocations)
class RValue {
    var type = RValueDataType.UNDEFINED
    var bits = 0L
    var ref: Any? = null

    val real: Double
        get() = when (type) {
            RValueDataType.REAL -> Double.fromBits(bits)
            RValueDataType.BOOLEAN, RValueDataType.INT32, RValueDataType.INT64 -> bits.toDouble()
            RValueDataType.UNDEFINED -> 0.0
            else -> error("Cannot read $type as a real!")
        }

    val bool: Boolean
        get() = when (type) {
            RValueDataType.BOOLEAN -> bits != 0L
            // GameMaker-HTML5's yyGetBool returns true if the value is > 0.5
            RValueDataType.REAL -> Double.fromBits(bits) > 0.5
            else -> error("Cannot read $type as a boolean!")
        }

    val int: Int
        get() = when (type) {
            RValueDataType.INT32 -> bits.toInt()
            RValueDataType.REAL -> Double.fromBits(bits).toInt()
            else -> error("Cannot read $type as an int!")
        }

    fun setUndefined() {
        this.type = RValueDataType.UNDEFINED
        this.ref = null
    }

    fun setString(value: String) {
        this.type = RValueDataType.STRING
        this.ref = value
    }

    fun setInt32(value: Int) {
        this.type = RValueDataType.INT32
        this.bits = value.toLong()
        this.ref = null
    }

    fun setInt64(value: Long) {
        this.type = RValueDataType.INT64
        this.bits = value
        this.ref = null
    }

    fun setReal(value: Double) {
        this.type = RValueDataType.REAL
        this.bits = value.toRawBits()
        this.ref = null
    }

    fun setBoolean(value: Boolean) {
        this.type = RValueDataType.BOOLEAN
        this.bits = if (value) 1 else 0
        this.ref = null
    }

    fun setArray(array: GMLArray) {
        this.type = RValueDataType.ARRAY
        this.ref = array
    }

    fun toGMLString(): String {
        return when (type) {
            RValueDataType.STRING -> ref as String
            RValueDataType.REAL -> MathUtils.formatReal(Double.fromBits(bits))
            RValueDataType.INT32 -> bits.toString()
            RValueDataType.INT64 -> bits.toString()
            RValueDataType.BOOLEAN -> if (bits != 0L) "1" else "0"
            RValueDataType.UNDEFINED -> "undefined"
            else -> error("Cannot convert $type to a string!")
        }
    }

    fun toStringTyped(): String {
        return when (type) {
            RValueDataType.STRING -> "\"$ref\""
            RValueDataType.INT32 -> "int32($bits)"
            RValueDataType.INT64 -> "int64($bits)"
            RValueDataType.BOOLEAN -> "boolean($bits)"
            RValueDataType.UNDEFINED -> "undefined"
            RValueDataType.REAL -> "real($real)"
            RValueDataType.ARRAY -> "array($ref)"
            else -> error("Unknown type $type!")
        }
    }

    fun makeIndependent(): RValue {
        val new = RValue()
        new.copyFrom(this)
        return new
    }

    fun copyFrom(rvalue: RValue) {
        this.type = rvalue.type
        this.bits = rvalue.bits
        this.ref = rvalue.ref
        // GameMaker uses CoW for arrays, so we need to copy the ENTIRE array!
        if (rvalue.ref is GMLArray) {
            val oldGMLArray = rvalue.ref as GMLArray
            val newGMLArray = GMLArray()
            newGMLArray.entries.putAll(oldGMLArray.entries.mapValues { it.value.makeIndependent() })
            rvalue.ref = newGMLArray
        }
    }
}