package com.mrpowergamerbr.starfait.wad.chunks

import com.mrpowergamerbr.starfait.StarfaitByteBuffer
import com.mrpowergamerbr.starfait.wad.Path

class PATH(val paths: List<Path>) {
    companion object {
        fun read(reader: StarfaitByteBuffer): PATH {
            val addresses = reader.readAddresses()

            val paths = ArrayList<Path>(addresses.size)
            for (address in addresses) {
                reader.position = address

                val name = reader.readStringPointer()
                val kind = reader.readInt()
                val closed = reader.readIntBoolean()
                val precision = reader.readInt()
                val pointCount = reader.readInt()

                val points = ArrayList<Path.Point>(pointCount)
                repeat(pointCount) {
                    val x = reader.readFloat()
                    val y = reader.readFloat()
                    val speed = reader.readFloat()

                    points.add(
                        Path.Point(
                            x = x,
                            y = y,
                            speed = speed
                        )
                    )
                }

                paths.add(
                    Path(
                        name = name,
                        kind = if (kind == 0) Path.PathKind.LINEAR else Path.PathKind.CURVED,
                        closed = closed,
                        precision = precision,
                        points = points
                    )
                )
            }

            return PATH(paths)
        }
    }
}