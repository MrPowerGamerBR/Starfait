package com.mrpowergamerbr.starfait.wad.chunks

import com.mrpowergamerbr.starfait.StarfaitByteReader
import com.mrpowergamerbr.starfait.wad.Code

class CODE(val codes: List<Code>) {
    companion object {
        fun read(reader: StarfaitByteReader): CODE {
            val addresses = reader.readAddresses()

            val codes = ArrayList<Code>(addresses.size)
            for (address in addresses) {
                reader.position = address

                val name = reader.readStringPointer()
                val length = reader.readUInt()
                val localsCount = reader.readUShort()
                val argumentsCount = reader.readUShort() // TODO: Is this even used?
                val bytecodeRelativeOffsetFieldPosition = reader.position
                val bytecodeRelativeOffset = reader.readInt()
                val offset = reader.readInt()

                reader.position = bytecodeRelativeOffsetFieldPosition + bytecodeRelativeOffset
                val data = reader.readBytes(length.toInt())

                codes.add(
                    Code(
                        name = name,
                        localsCount = localsCount,
                        argumentsCount = argumentsCount,
                        offset = offset,
                        data = data
                    )
                )
            }

            return CODE(codes)
        }
    }
}