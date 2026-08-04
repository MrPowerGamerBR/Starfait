package com.mrpowergamerbr.starfait.wad.chunks

import com.mrpowergamerbr.starfait.StarfaitByteReader
import com.mrpowergamerbr.starfait.wad.Code

class CODE(val codes: List<Code>, val postAddressPosition: Int, val bytecode: ByteArray) {
    companion object {
        fun read(reader: StarfaitByteReader): CODE {
            val addresses = reader.readAddresses()

            val postAddressPosition = reader.position
            val codes = ArrayList<Code>(addresses.size)
            val minAddressTarget = addresses.min()

            // We want to read ALL the bytecode at once!
            val bytecode = reader.readBytes(minAddressTarget - postAddressPosition)

            for (address in addresses) {
                reader.position = address

                val name = reader.readStringPointer()
                val length = reader.readUInt()
                val localsCount = reader.readUShort()
                val argumentsCount = reader.readUShort() // TODO: Is this even used?
                val bytecodeRelativeOffsetFieldPosition = reader.position
                val bytecodeRelativeOffset = reader.readInt() // This is relative based on the BEGINNING of this field
                val offset = reader.readInt()

                codes.add(
                    Code(
                        name = name,
                        length = length,
                        localsCount = localsCount,
                        argumentsCount = argumentsCount,
                        bytecodeRelativeOffsetFieldPosition = bytecodeRelativeOffsetFieldPosition,
                        bytecodeRelativeOffset = bytecodeRelativeOffset,
                        offset = offset
                    )
                )
            }

            return CODE(codes, postAddressPosition, bytecode)
        }
    }
}