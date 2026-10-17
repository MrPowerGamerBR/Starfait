package com.mrpowergamerbr.starfait.wad

import com.mrpowergamerbr.starfait.StarfaitByteBuffer

class TexturePageEntryRegistry {
    val entries = mutableMapOf<TexturePageEntryPointer, TexturePageEntry>()

    fun readTexturePageEntryAtPointer(reader: StarfaitByteBuffer, pointer: TexturePageEntryPointer, fromTPAGChunk: Boolean): TexturePageEntry {
        if (this.entries.containsKey(pointer))
            return this.entries.getValue(pointer)

        val start = reader.position
        reader.position = pointer.address
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
        val textureIndex = reader.readShort().toInt()
        // This hack is because of TranslaTale/WinPack
        // It stores +1 because it stores the values as the *runtime* TPAG entry index, not the WAD entry index
        // (because the first texture on the runner is always a 1x1 white texture)
        val realTextureIndex = if (fromTPAGChunk)
            textureIndex
        else textureIndex - 1
        val entry = TexturePageEntry(
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
            textureIndex = realTextureIndex.toShort()
        )
        entries[pointer] = entry
        reader.position = start
        return entry
    }
}