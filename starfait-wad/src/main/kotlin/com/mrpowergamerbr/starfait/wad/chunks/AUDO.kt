package com.mrpowergamerbr.starfait.wad.chunks

import com.mrpowergamerbr.starfait.StarfaitByteBuffer

class AUDO(val blobs: List<ByteArray>) {
    companion object {
        fun read(reader: StarfaitByteBuffer): AUDO {
            val addresses = reader.readAddresses()

            val blobs = ArrayList<ByteArray>(addresses.size)
            for (address in addresses) {
                reader.position = address

                val length = reader.readInt()
                val data = reader.readBytes(length)
                blobs.add(data)
            }

            return AUDO(blobs)
        }
    }
}