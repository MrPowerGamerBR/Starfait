package com.mrpowergamerbr.starfait.wad.chunks

import com.mrpowergamerbr.starfait.StarfaitByteBuffer
import com.mrpowergamerbr.starfait.wad.TexturePageEntry
import com.mrpowergamerbr.starfait.wad.TexturePageEntryPointer
import com.mrpowergamerbr.starfait.wad.TexturePageEntryRegistry

class TPAG(val entries: Map<TexturePageEntryPointer, TexturePageEntry>) {
    companion object {
        fun read(reader: StarfaitByteBuffer, tpagRegistry: TexturePageEntryRegistry): TPAG {
            val addresses = reader.readAddresses()
            val entries = HashMap<TexturePageEntryPointer, TexturePageEntry>(addresses.size)

            for (address in addresses) {
                val pointer = TexturePageEntryPointer(address)
                val entry = tpagRegistry.readTexturePageEntryAtPointer(reader, pointer, true)
                entries[pointer] = entry
            }

            return TPAG(entries)
        }
    }
}