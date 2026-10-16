package com.mrpowergamerbr.starfait.vm

import kotlin.jvm.JvmInline

@JvmInline
value class FunctionReferenceOperand(val value: Int) {
    fun delta() = (value shl 4) shr 4 // low 28 bits
    fun functionIndex() = value
}