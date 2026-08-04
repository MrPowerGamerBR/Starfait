package com.mrpowergamerbr.starfait.wad.chunks

import com.mrpowergamerbr.starfait.StarfaitByteBuffer
import com.mrpowergamerbr.starfait.wad.Background

class BGND(val backgrounds: List<Background>) {
    companion object {
        fun read(reader: StarfaitByteBuffer): BGND {
            val addresses = reader.readAddresses()

            val backgrounds = ArrayList<Background>(addresses.size)
            for (address in addresses) {
                reader.position = address

                val name = reader.readStringPointer()
                val transparent = reader.readIntBoolean()
                val smooth = reader.readIntBoolean()
                val preload = reader.readIntBoolean()
                val pointer = reader.readTexturePageEntryPointer()

                backgrounds.add(
                    Background(
                        name = name,
                        transparent = transparent,
                        smooth = smooth,
                        preload = preload,
                        texturePageEntryPointer = pointer
                    )
                )
            }

            return BGND(backgrounds)
        }
    }
}