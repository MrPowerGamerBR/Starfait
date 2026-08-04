package com.mrpowergamerbr.starfait.wad.chunks

import com.mrpowergamerbr.starfait.StarfaitByteBuffer
import com.mrpowergamerbr.starfait.wad.Texture
import java.nio.ByteOrder

class TXTR(val textures: List<Texture>) {
    companion object {
        fun read(reader: StarfaitByteBuffer): TXTR {
            val addresses = reader.readAddresses()

            val textures = ArrayList<Texture>(addresses.size)
            for (address in addresses) {
                reader.position = address

                val flags = reader.readUInt()
                val blobPointer = reader.readTexturePageEntryPointer()

                reader.position = blobPointer.address

                reader.byteOrder = ByteOrder.BIG_ENDIAN

                // Parse it as a PNG file... which funningly enough is also a IFF format... or at least very similar to it
                reader.readBytes(8) // TODO: Actually parse it as a PNG?
                var end = 0
                while (true) {
                    val chunkSize = reader.readInt()
                    val chunkName = reader.readUTF8String(4)
                    // println("Length $chunkSize PNG chunk name $chunkName, size $chunkSize")
                    if (chunkName == "IEND") {
                        end = reader.position
                        break
                    }
                    reader.position += chunkSize
                    reader.readInt() // CRC
                }

                reader.byteOrder = ByteOrder.LITTLE_ENDIAN

                reader.position = blobPointer.address
                val blob = reader.readBytes(end - blobPointer.address)

                textures.add(
                    Texture(
                        flags = flags,
                        blobPointer = blobPointer,
                        blob = blob
                    )
                )
            }

            return TXTR(textures)
        }
    }
}