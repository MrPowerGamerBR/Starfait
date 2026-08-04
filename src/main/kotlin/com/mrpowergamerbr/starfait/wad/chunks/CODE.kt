package com.mrpowergamerbr.starfait.wad.chunks

import com.mrpowergamerbr.starfait.StarfaitByteReader
import com.mrpowergamerbr.starfait.wad.Code

class CODE(val codes: List<Code>) {
    companion object {
        private val EMPTY_BYTECODE_STREAM = ByteArray(0)

        fun read(reader: StarfaitByteReader): CODE {
            val addresses = reader.readAddresses()

            val codes = ArrayList<Code>(addresses.size)
            val readBytecodeStreams = mutableMapOf<Int, ByteArray>()
            for (address in addresses) {
                reader.position = address

                val name = reader.readStringPointer()
                val length = reader.readUInt()
                val localsCount = reader.readUShort()
                val argumentsCount = reader.readUShort() // TODO: Is this even used?
                val bytecodeRelativeOffsetFieldPosition = reader.position
                val bytecodeRelativeOffset = reader.readInt()
                val offset = reader.readInt()

                val data = if (length == 0u) {
                    EMPTY_BYTECODE_STREAM
                } else {
                    reader.position = bytecodeRelativeOffsetFieldPosition + bytecodeRelativeOffset
                    readBytecodeStreams.getOrPut(reader.position) { reader.readBytes(length.toInt()) }
                }

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