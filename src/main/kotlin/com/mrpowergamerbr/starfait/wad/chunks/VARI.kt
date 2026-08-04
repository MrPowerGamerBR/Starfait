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
            require(remaining % ENTRY_SIZE == 0) { "VARI chunk has trailing bytes! There are $remaining bytes remaining!" }
            val entryCount = remaining / ENTRY_SIZE
            val variables = ArrayList<Variable>(entryCount)
            repeat(entryCount) {
                variables.add(
                    Variable(
                        name = reader.readStringPointer(),
                        instanceType = reader.readInt(),
                        varId = reader.readInt(),
                        occurrenceCount = reader.readInt(),
                        firstAddress = reader.readInt()
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