package com.mrpowergamerbr.starfait.vm

enum class InstructionDataType(val id: Int, val stackSize: Byte) {
    DOUBLE(0, 8),
    FLOAT(1, 4),
    INT32(2, 4),
    INT64(3, 8),
    BOOLEAN(4, 4),
    VARIABLE(5, 16),
    STRING(6, 4),
    INT16(15, -1);

    companion object {
        private val BY_ID = arrayOfNulls<InstructionDataType>(16)
            .also { table -> entries.forEach { table[it.id] = it } }

        fun byId(id: Int) = BY_ID[id] ?: error("Unknown instruction data type $id")
    }
}