package com.mrpowergamerbr.starfait.wad.chunks

import com.mrpowergamerbr.starfait.StarfaitByteReader
import com.mrpowergamerbr.starfait.wad.TexturePageEntry
import com.mrpowergamerbr.starfait.wad.TexturePageEntryPointer

class TPAG(val entries: Map<TexturePageEntryPointer, TexturePageEntry>, ) {
    companion object {
        fun read(reader: StarfaitByteReader): TPAG {
            val addresses = reader.readAddresses()
            val entries = HashMap<TexturePageEntryPointer, TexturePageEntry>(addresses.size)

            for (address in addresses) {
                reader.position = address
                val sourceX = reader.readUShort()
                val sourceY = reader.readUShort()
                val sourceWidth = reader.readUShort()
                val sourceHeight = reader.readUShort()
                val targetX = reader.readUShort()
                val targetY = reader.readUShort()
                val targetWidth = reader.readUShort()
                val targetHeight = reader.readUShort()
                val boundingWidth = reader.readUShort()
                val boundingHeight = reader.readUShort()
                val textureIndex = reader.readShort()

                entries[TexturePageEntryPointer(address)] = TexturePageEntry(
                    sourceX = sourceX,
                    sourceY = sourceY,
                    sourceWidth = sourceWidth,
                    sourceHeight = sourceHeight,
                    targetX = targetX,
                    targetY = targetY,
                    targetWidth = targetWidth,
                    targetHeight = targetHeight,
                    boundingWidth = boundingWidth,
                    boundingHeight = boundingHeight,
                    textureIndex = textureIndex
                )
            }

            return TPAG(entries)
        }
    }
}