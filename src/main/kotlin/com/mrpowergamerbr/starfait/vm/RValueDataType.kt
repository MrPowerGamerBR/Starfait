package com.mrpowergamerbr.starfait.vm

// We don't use a enum because a enum is a *real* object, and we don't want that
@JvmInline
value class RValueDataType(val id: Byte) {
    companion object {
        val UNDEFINED = RValueDataType(0)
        val STRING = RValueDataType(1)
        val INT16 = RValueDataType(2)
        val BOOLEAN = RValueDataType(3)
    }
}