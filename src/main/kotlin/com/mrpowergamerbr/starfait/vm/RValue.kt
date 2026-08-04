package com.mrpowergamerbr.starfait.vm

// Yes this looks wonky as hell, but that's because we are trying to squeeze performance from the JVM (to avoid unnecessary allocations)
class RValue {
    var type = RValueDataType.UNDEFINED
    var bits = 0L
    var ref: Any? = null

    val real: Double
        get() = when (type) {
            RValueDataType.BOOLEAN, RValueDataType.INT16 -> bits.toDouble()
            RValueDataType.UNDEFINED -> 0.0
            else -> error("Cannot read $type as a real!")
        }

    fun setUndefined() {
        this.type = RValueDataType.UNDEFINED
        this.ref = null
    }

    fun setString(value: String) {
        this.type = RValueDataType.STRING
        this.ref = value
    }

    fun setInt(value: Int) {
        this.type = RValueDataType.INT16
        this.bits = value.toLong()
        this.ref = null
    }

    fun setBoolean(value: Boolean) {
        this.type = RValueDataType.BOOLEAN
        this.bits = if (value) 1 else 0
        this.ref = null
    }

    fun toStringTyped(): String {
        return when (type) {
            RValueDataType.STRING -> "\"$ref\""
            RValueDataType.INT16 -> "int16($bits)"
            RValueDataType.BOOLEAN -> "boolean($bits)"
            RValueDataType.UNDEFINED -> "undefined"
            else -> error("Unknown type $type!")
        }
    }

    fun makeIndependent(): RValue {
        val rvalue = RValue()
        rvalue.type = this.type
        rvalue.bits = this.bits
        rvalue.ref = this.ref
        return rvalue
    }

    fun copyFrom(rvalue: RValue) {
        this.type = rvalue.type
        this.bits = rvalue.bits
        this.ref = rvalue.ref
    }
}