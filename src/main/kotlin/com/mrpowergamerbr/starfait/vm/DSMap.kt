package com.mrpowergamerbr.starfait.vm

class DSMap {
    val entries = mutableMapOf<Any, RValue>()

    fun get(key: RValue): RValue? {
        val realKey = when (key.type) {
            RValueDataType.INT32 -> key.real
            RValueDataType.STRING -> key.ref as String
            else -> error("Cannot use $key as a key in a DSMap!")
        }

        return entries[realKey]
    }

    fun set(key: RValue, value: RValue) {
        val realKey = when (key.type) {
            RValueDataType.INT32 -> key.real
            RValueDataType.STRING -> key.ref as String
            else -> error("Cannot use $key as a key in a DSMap!")
        }

        entries[realKey] = value.makeIndependent()
    }
}