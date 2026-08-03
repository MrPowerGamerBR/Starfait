package com.mrpowergamerbr.starfait.wad.chunks

import com.mrpowergamerbr.starfait.StarfaitByteReader
import com.mrpowergamerbr.starfait.wad.Script

class SCPT(val scripts: List<Script>) {
    companion object {
        fun read(reader: StarfaitByteReader): SCPT {
            val addresses = reader.readAddresses()

            val scripts = ArrayList<Script>(addresses.size)
            for (address in addresses) {
                reader.position = address

                val name = reader.readStringPointer()
                val codeIndex = reader.readInt()

                scripts.add(
                    Script(
                        name = name,
                        codeIndex = codeIndex
                    )
                )
            }

            return SCPT(scripts)
        }
    }
}