package com.mrpowergamerbr.starfait.wad.chunks

import com.mrpowergamerbr.starfait.StarfaitByteBuffer
import com.mrpowergamerbr.starfait.wad.Constant
import com.mrpowergamerbr.starfait.wad.OPTNFlags
import com.mrpowergamerbr.starfait.wad.Pointer

class OPTN(
    val flags: OPTNFlags,
    val scale: Int,
    val colorDepth: UInt,
    val windowColor: UInt,
    val resolution: UInt,
    val frequency: UInt,
    val vertexSync: UInt,
    val priority: UInt,
    val loadImage: Pointer,
    val loadAlpha: Int,
    val constants: List<Constant>
) {
    companion object {
        fun read(reader: StarfaitByteBuffer): OPTN {
            reader.readUInt() // magic
            reader.readUInt() // format version
            val flags = reader.readUInt()
            reader.readUInt() // unused
            val scale = reader.readInt()
            val windowColor = reader.readUInt()
            val colorDepth = reader.readUInt()
            val resolution = reader.readUInt()
            val frequency = reader.readUInt()
            val vertexSync = reader.readUInt()
            val priority = reader.readUInt()
            reader.readUInt() // unused
            reader.readUInt() // unused
            val loadImage = reader.readPointer()
            val loadAlpha = reader.readInt()
            val constantCount = reader.readUInt()
            val constants = ArrayList<Constant>(constantCount.toInt())
            repeat(constantCount.toInt()) {
                val name = reader.readStringPointer()
                val value = reader.readStringPointer()
                constants.add(Constant(name = name, value = value))
            }

            return OPTN(
                flags = OPTNFlags(flags),
                scale = scale,
                colorDepth = colorDepth,
                windowColor = windowColor,
                resolution = resolution,
                frequency = frequency,
                vertexSync = vertexSync,
                priority = priority,
                loadImage = loadImage,
                loadAlpha = loadAlpha,
                constants = constants
            )
        }
    }
}