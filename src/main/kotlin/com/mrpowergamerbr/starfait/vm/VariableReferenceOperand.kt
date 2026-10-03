package com.mrpowergamerbr.starfait.vm

@JvmInline
value class VariableReferenceOperand(val value: Int) {
    fun delta() = (value shl 4) shr 4 // low 28 bits
    fun variableSlot() = (value shl 4) shr 4 // low 28 bits
    fun referenceFlags() = value ushr 28
    fun hasArrayIndex() = value >= 0
    fun hasInstanceIdOnStack() = (value and 0x20000000) == 0
    fun isRoomInstanceId() = (value and 0x40000000) != 0
}