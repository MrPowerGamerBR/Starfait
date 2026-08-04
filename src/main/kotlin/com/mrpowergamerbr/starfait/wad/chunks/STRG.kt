package com.mrpowergamerbr.starfait.wad.chunks

import com.mrpowergamerbr.starfait.StarfaitByteReader
import com.mrpowergamerbr.starfait.wad.StringPointer

class STRG(
    val strings: Map<StringPointer, String>,
    val stringsByIndex: List<String>
) {
    companion object {
        fun read(reader: StarfaitByteReader): STRG {
            val addresses = reader.readAddresses()
            val strings = HashMap<StringPointer, String>(addresses.size)
            val stringsByIndex = ArrayList<String>(addresses.size)

            for ((index, address) in addresses.withIndex()) {
                reader.position = address
                val length = reader.readInt()
                val string = reader.readUTF8String(length)

                // The address includes the size of the string, however anywhere else in the WAD it references the string without the size
                strings[StringPointer(address + 4)] = string
                // Used for the VM, which references strings by index
                stringsByIndex.add(string)
            }

            return STRG(strings, stringsByIndex)
        }
    }

    fun getString(pointer: StringPointer): String {
        return strings[pointer] ?: error("String at $pointer not found!")
    }
}