package com.mrpowergamerbr.starfait.vm

class DSMap {
    val entries = mutableMapOf<Any, RValue>()

    private fun convertToMapKey(key: RValue): Any {
        return when (key.type) {
            RValueDataType.INT32 -> key.real
            RValueDataType.STRING -> key.ref as String
            else -> error("Cannot use $key as a key in a DSMap!")
        }
    }

    fun get(key: RValue): RValue? {
        val realKey = convertToMapKey(key)
        return entries[realKey]
    }

    fun set(key: RValue, value: RValue) {
        val realKey = convertToMapKey(key)
        entries[realKey] = value.makeIndependent()
    }

    fun containsKey(key: RValue) = entries.containsKey(convertToMapKey(key))
}