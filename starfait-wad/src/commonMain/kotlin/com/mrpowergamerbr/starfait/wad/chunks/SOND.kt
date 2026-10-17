package com.mrpowergamerbr.starfait.wad.chunks

import com.mrpowergamerbr.starfait.StarfaitByteBuffer
import com.mrpowergamerbr.starfait.wad.Sound

class SOND(val sounds: List<Sound>) {
    companion object {
        fun read(reader: StarfaitByteBuffer): SOND {
            val addresses = reader.readAddresses()

            val sounds = ArrayList<Sound>(addresses.size)
            for (address in addresses) {
                reader.position = address

                val name = reader.readStringPointerAndReadNullTerminatedString()
                val kind = reader.readInt()
                val fileType = reader.readStringPointerAndReadNullTerminatedString()
                val fileName = reader.readStringPointerAndReadNullTerminatedString()
                val effects = reader.readInt()
                val volume = reader.readFloat()
                val pan = reader.readFloat()
                val preload = reader.readIntBoolean()
                val audoIndex = reader.readInt()

                sounds.add(
                    Sound(
                        name = name,
                        kind = Sound.Kind.fromValue(kind),
                        fileType = fileType,
                        fileName = fileName,
                        effects = effects,
                        volume = volume,
                        pan = pan,
                        preload = preload,
                        audoIndex = audoIndex
                    )
                )
            }

            return SOND(sounds)
        }
    }
}