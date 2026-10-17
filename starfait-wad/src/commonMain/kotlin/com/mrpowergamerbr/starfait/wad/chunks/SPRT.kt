package com.mrpowergamerbr.starfait.wad.chunks

import com.mrpowergamerbr.starfait.StarfaitByteBuffer
import com.mrpowergamerbr.starfait.wad.CollisionMask
import com.mrpowergamerbr.starfait.wad.Sprite
import com.mrpowergamerbr.starfait.wad.Sprite.BoundingBoxMode
import com.mrpowergamerbr.starfait.wad.TexturePageEntryPointer

class SPRT(val sprites: List<Sprite>) {
    companion object {
        // A frame count of -1 means the sprite is a SWF or a Spine skeleton sprite instead of a regular one, which stores completely different data after this point
        private const val SPECIAL_SPRITE_FRAME_COUNT = -1

        fun read(reader: StarfaitByteBuffer): SPRT {
            val addresses = reader.readAddresses()

            val sprites = ArrayList<Sprite>(addresses.size)
            for (address in addresses) {
                reader.position = address

                val name = reader.readStringPointerAndReadNullTerminatedString()
                val width = reader.readInt()
                val height = reader.readInt()
                val bboxLeft = reader.readInt()
                val bboxRight = reader.readInt()
                val bboxBottom = reader.readInt()
                val bboxTop = reader.readInt()
                val transparent = reader.readIntBoolean()
                val smooth = reader.readIntBoolean()
                val preload = reader.readIntBoolean()
                val bboxMode = reader.readInt()
                val precise = reader.readIntBoolean()
                val originX = reader.readInt()
                val originY = reader.readInt()

                var frameCount = reader.readInt()
                var type = Sprite.Type.NORMAL

                if (frameCount == SPECIAL_SPRITE_FRAME_COUNT) {
                    reader.readInt() // Unused
                    type = Sprite.Type.fromValue(reader.readInt())

                    require(type == Sprite.Type.NORMAL) { "Sprite at $address is a $type sprite, which isn't supported yet!" }

                    // A special sprite has no frames of its own, the mask count comes right after
                    frameCount = 0
                }

                val frames = ArrayList<TexturePageEntryPointer>(frameCount)
                repeat(frameCount) {
                    frames.add(reader.readTexturePageEntryPointer())
                }

                // Every mask is one bit per pixel, with each row padded to a whole byte
                val maskCount = reader.readInt()
                val maskSize = ((width + 7) / 8) * height

                val collisionMasks = ArrayList<ByteArray>(maskCount)
                repeat(maskCount) {
                    collisionMasks.add(reader.readBytes(maskSize))
                }

                sprites.add(
                    Sprite(
                        name = name,
                        width = width,
                        height = height,
                        bboxLeft = bboxLeft,
                        bboxRight = bboxRight,
                        bboxBottom = bboxBottom,
                        bboxTop = bboxTop,
                        transparent = transparent,
                        smooth = smooth,
                        preload = preload,
                        bboxMode = BoundingBoxMode.fromValue(bboxMode),
                        precise = precise,
                        originX = originX,
                        originY = originY,
                        type = type,
                        frames = frames,
                        collisionMasks = collisionMasks.map { CollisionMask(it) }
                    )
                )
            }

            return SPRT(sprites)
        }
    }
}
