package com.mrpowergamerbr.starfait.wad.chunks

import com.mrpowergamerbr.starfait.StarfaitByteBuffer
import com.mrpowergamerbr.starfait.wad.Room
import com.mrpowergamerbr.starfait.wad.RoomFlags

class ROOM(val rooms: List<Room>) {
    companion object {
        fun read(reader: StarfaitByteBuffer): ROOM {
            val addresses = reader.readAddresses()

            val rooms = ArrayList<Room>(addresses.size)
            for (address in addresses) {
                reader.position = address

                val name = reader.readStringPointer()
                val caption = reader.readStringPointer()
                val width = reader.readInt()
                val height = reader.readInt()
                val speed = reader.readInt()
                val persistent = reader.readIntBoolean()
                val backgroundColor = reader.readInt()
                val drawBackgroundColor = reader.readIntBoolean()
                val creationCodeId = reader.readInt()
                val flags = reader.readUInt()

                val backgroundsAddress = reader.readInt()
                val viewsAddress = reader.readInt()
                val instancesAddress = reader.readInt()
                val tilesAddress = reader.readInt()

                val physicsWorld = reader.readIntBoolean()
                val physicsWorldTop = reader.readInt()
                val physicsWorldLeft = reader.readInt()
                val physicsWorldRight = reader.readInt()
                val physicsWorldBottom = reader.readInt()
                val physicsWorldGravityX = reader.readFloat()
                val physicsWorldGravityY = reader.readFloat()
                val physicsWorldPixelsToMeters = reader.readFloat()

                reader.position = backgroundsAddress
                val backgrounds = reader.readAddresses().map {
                    reader.position = it

                    val enabled = reader.readIntBoolean()
                    val foreground = reader.readIntBoolean()
                    val backgroundIndex = reader.readInt()
                    val x = reader.readInt()
                    val y = reader.readInt()
                    val tileX = reader.readIntBoolean()
                    val tileY = reader.readIntBoolean()
                    val speedX = reader.readInt()
                    val speedY = reader.readInt()
                    val stretch = reader.readIntBoolean()

                    Room.Background(
                        enabled = enabled,
                        foreground = foreground,
                        backgroundIndex = backgroundIndex,
                        x = x,
                        y = y,
                        tileX = tileX,
                        tileY = tileY,
                        speedX = speedX,
                        speedY = speedY,
                        stretch = stretch
                    )
                }

                reader.position = viewsAddress
                val views = reader.readAddresses().map {
                    reader.position = it

                    val visible = reader.readIntBoolean()
                    val viewX = reader.readInt()
                    val viewY = reader.readInt()
                    val viewWidth = reader.readInt()
                    val viewHeight = reader.readInt()
                    val portX = reader.readInt()
                    val portY = reader.readInt()
                    val portWidth = reader.readInt()
                    val portHeight = reader.readInt()
                    val borderX = reader.readInt()
                    val borderY = reader.readInt()
                    val speedX = reader.readInt()
                    val speedY = reader.readInt()
                    val objectId = reader.readInt()

                    Room.View(
                        visible = visible,
                        viewX = viewX,
                        viewY = viewY,
                        viewWidth = viewWidth,
                        viewHeight = viewHeight,
                        portX = portX,
                        portY = portY,
                        portWidth = portWidth,
                        portHeight = portHeight,
                        borderX = borderX,
                        borderY = borderY,
                        speedX = speedX,
                        speedY = speedY,
                        objectId = objectId
                    )
                }

                reader.position = instancesAddress
                val instances = reader.readAddresses().map {
                    reader.position = it

                    val x = reader.readInt()
                    val y = reader.readInt()
                    val objectIndex = reader.readInt()
                    val id = reader.readInt()
                    val instanceCreationCodeId = reader.readInt()
                    val scaleX = reader.readFloat()
                    val scaleY = reader.readFloat()
                    val color = reader.readInt()
                    val rotation = reader.readFloat()
                    val preCreateCodeId = reader.readInt()

                    Room.Instance(
                        x = x,
                        y = y,
                        objectIndex = objectIndex,
                        id = id,
                        creationCodeId = instanceCreationCodeId,
                        scaleX = scaleX,
                        scaleY = scaleY,
                        color = color,
                        rotation = rotation,
                        preCreateCodeId = preCreateCodeId
                    )
                }

                reader.position = tilesAddress
                val tiles = reader.readAddresses().map {
                    reader.position = it

                    val x = reader.readInt()
                    val y = reader.readInt()
                    val backgroundIndex = reader.readInt()
                    val sourceX = reader.readInt()
                    val sourceY = reader.readInt()
                    val tileWidth = reader.readInt()
                    val tileHeight = reader.readInt()
                    val depth = reader.readInt()
                    val id = reader.readInt()
                    val scaleX = reader.readFloat()
                    val scaleY = reader.readFloat()
                    val color = reader.readInt()

                    Room.Tile(
                        x = x,
                        y = y,
                        backgroundIndex = backgroundIndex,
                        sourceX = sourceX,
                        sourceY = sourceY,
                        width = tileWidth,
                        height = tileHeight,
                        depth = depth,
                        id = id,
                        scaleX = scaleX,
                        scaleY = scaleY,
                        color = color
                    )
                }

                rooms.add(
                    Room(
                        name = name,
                        caption = caption,
                        width = width,
                        height = height,
                        speed = speed,
                        persistent = persistent,
                        backgroundColor = backgroundColor,
                        drawBackgroundColor = drawBackgroundColor,
                        creationCodeId = creationCodeId,
                        flags = RoomFlags(flags),
                        physicsWorld = physicsWorld,
                        physicsWorldTop = physicsWorldTop,
                        physicsWorldLeft = physicsWorldLeft,
                        physicsWorldRight = physicsWorldRight,
                        physicsWorldBottom = physicsWorldBottom,
                        physicsWorldGravityX = physicsWorldGravityX,
                        physicsWorldGravityY = physicsWorldGravityY,
                        physicsWorldPixelsToMeters = physicsWorldPixelsToMeters,
                        backgrounds = backgrounds,
                        views = views,
                        instances = instances,
                        tiles = tiles
                    )
                )
            }

            return ROOM(rooms)
        }
    }
}
