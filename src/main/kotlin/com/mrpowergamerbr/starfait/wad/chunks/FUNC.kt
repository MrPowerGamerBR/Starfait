package com.mrpowergamerbr.starfait.wad.chunks

import com.mrpowergamerbr.starfait.StarfaitByteBuffer
import com.mrpowergamerbr.starfait.wad.Function
import com.mrpowergamerbr.starfait.wad.Variable

class FUNC(
    val functions: List<Function>
) {
    companion object {
        fun read(reader: StarfaitByteBuffer, chunkSize: Int): FUNC {
            val start = reader.position
            val count = reader.readInt()
            val functions = ArrayList<Function>(count)

            repeat(count) {
                val name = reader.readStringPointer()
                val occurenceCount = reader.readInt()
                val firstAddress = reader.readInt()

                functions.add(Function(name, occurenceCount, firstAddress))
            }

            return FUNC(functions)
        }
    }
}