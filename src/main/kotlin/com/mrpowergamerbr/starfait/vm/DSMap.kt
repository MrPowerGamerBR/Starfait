package com.mrpowergamerbr.starfait.vm

class DSMap {
    val entries = mutableMapOf<Any, RValue>()

    private fun convertToMapKey(key: RValue): Any {
        return when (key) {
            is RValue.Array -> TODO()
            is RValue.Boolean -> TODO()
            is RValue.Int32 -> TODO()
            is RValue.Int64 -> TODO()
            is RValue.Real -> TODO()
            is RValue.String -> key.convertToGMLString()
            RValue.Undefined -> TODO()
        }
    }

    fun get(key: RValue): RValue? {
        val realKey = convertToMapKey(key)
        return entries[realKey]
    }

    fun set(key: RValue, value: RValue) {
        val realKey = convertToMapKey(key)
        entries[realKey] = value
    }

    fun containsKey(key: RValue) = entries.containsKey(convertToMapKey(key))
}