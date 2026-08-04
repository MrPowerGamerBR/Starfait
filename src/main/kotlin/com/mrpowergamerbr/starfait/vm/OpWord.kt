package com.mrpowergamerbr.starfait.vm

@JvmInline
value class OpWord(val value: Int) {
    fun opcode() = (value ushr 24) and 0xFF
    fun type1() = (value ushr 16) and 0xF
    fun type2() = (value ushr 20) and 0xF
    fun extra() = value.toShort().toInt()
    fun comparisonFunction() = (value ushr 8) and 0xF
    fun branchOffset() = (value shl 9) shr 7
}