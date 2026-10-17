package com.mrpowergamerbr.starfait.wad.chunks

import com.mrpowergamerbr.starfait.StarfaitByteBuffer
import com.mrpowergamerbr.starfait.wad.Variable

class VARI(
    val globalVariables: Int,
    val instanceVariables: Int,
    val localVariables: Int,
    val variables: List<Variable>
) {
    companion object {
        // See the variable entry format below
        private const val ENTRY_SIZE = 20

        fun read(reader: StarfaitByteBuffer, chunkSize: Int): VARI {
            val start = reader.position
            val globalVariables = reader.readInt()
            val instanceVariables = reader.readInt() // Somehow it seems that modern GM games always have globalVariables == instanceVariables
            val localVariables = reader.readInt()

            // The WAD doesn't give us the entry count (fun!)
            val remaining = ((start + chunkSize) - reader.position)
            val entryCount = remaining / ENTRY_SIZE
            val variables = ArrayList<Variable>(entryCount)
            repeat(entryCount) {
                val name = reader.readStringPointerAndReadNullTerminatedString()
                val instanceType = reader.readInt()
                val varId = reader.readInt()
                val occurrenceCount = reader.readInt()
                val firstAddress = reader.readInt()

                variables.add(
                    Variable(
                        name = name,
                        instanceType = instanceType,
                        varId = varId,
                        occurrenceCount = occurrenceCount,
                        firstAddress = firstAddress
                    )
                )
            }

            return VARI(
                globalVariables,
                instanceVariables,
                localVariables,
                variables
            )
        }
    }
}