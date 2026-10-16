package com.mrpowergamerbr.starfait.runner.resources

import com.mrpowergamerbr.starfait.renderer.TextureHandle
import com.mrpowergamerbr.starfait.wad.GameWAD
import com.mrpowergamerbr.starfait.wad.Texture
import com.mrpowergamerbr.starfait.wad.TexturePageEntry
import com.mrpowergamerbr.starfait.wad.TexturePageEntryPointer

class RunnerTexturePageEntry(
    val sourceX: UShort,
    val sourceY: UShort,
    val sourceWidth: UShort,
    val sourceHeight: UShort,
    val targetX: UShort,
    val targetY: UShort,
    val targetWidth: UShort,
    val targetHeight: UShort,
    val boundingWidth: UShort,
    val boundingHeight: UShort,
    val textureIndex: TextureHandle
) {
    companion object {
        fun fromWAD(texturePageEntry: TexturePageEntry, textureHandles: Map<Short, TextureHandle>): RunnerTexturePageEntry {
            return RunnerTexturePageEntry(
                texturePageEntry.sourceX,
                texturePageEntry.sourceY,
                texturePageEntry.sourceWidth,
                texturePageEntry.sourceHeight,
                texturePageEntry.targetX,
                texturePageEntry.targetY,
                texturePageEntry.targetWidth,
                texturePageEntry.targetHeight,
                texturePageEntry.boundingWidth,
                texturePageEntry.boundingHeight,

                textureHandles[texturePageEntry.textureIndex]!!
            )
        }

        fun fromWAD(gameWAD: GameWAD, texturePageEntryPointer: TexturePageEntryPointer, textureHandles: Map<Short, TextureHandle>): RunnerTexturePageEntry? {
            if (texturePageEntryPointer.isNull)
                return null

            return fromWAD(gameWAD.tpag.entries[texturePageEntryPointer]!!, textureHandles)
        }

        fun fromDynamic(gameWAD: GameWAD, texturePageEntryPointer: TexturePageEntryPointer, textureHandles: Map<Short, TextureHandle>): RunnerTexturePageEntry? {
            if (texturePageEntryPointer.isNull)
                return null

            return fromWAD(gameWAD.tpag.entries[texturePageEntryPointer]!!, textureHandles)
        }
    }
}