package com.mrpowergamerbr.starfait.vm

enum class InstructionDataType(val id: Int) {
    DOUBLE(0),
    FLOAT(1),
    INT32(2),
    INT64(3),
    BOOLEAN(4),
    VARIABLE(5),
    STRING(6),
    INT16(15);

    companion object {
        private val BY_ID = arrayOfNulls<InstructionDataType>(16)
            .also { table -> entries.forEach { table[it.id] = it } }

        fun byId(id: Int) = BY_ID[id] ?: error("Unknown instruction data type $id")
    }
}