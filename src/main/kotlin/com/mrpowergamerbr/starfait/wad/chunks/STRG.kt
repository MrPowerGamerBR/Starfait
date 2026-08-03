package com.mrpowergamerbr.starfait.wad.chunks

import com.mrpowergamerbr.starfait.StarfaitByteReader
import com.mrpowergamerbr.starfait.wad.StringPointer

class STRG(val strings: Map<StringPointer, String>) {
    companion object {
        fun read(reader: StarfaitByteReader): STRG {
            val addresses = reader.readAddresses()
            val strings = HashMap<StringPointer, String>(addresses.size)

            for (address in addresses) {
                reader.position = address
                val length = reader.readInt()
                val string = reader.readUTF8String(length)

                // The address includes the size of the string, however anywhere else in the WAD it references the string without the size
                strings[StringPointer(address + 4)] = string
            }

            return STRG(strings)
        }
    }

    fun getString(pointer: StringPointer): String {
        return strings[pointer] ?: error("String at $pointer not found!")
    }
}