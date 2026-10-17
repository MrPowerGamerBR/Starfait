package com.mrpowergamerbr.starfait.runner.resources

import com.mrpowergamerbr.starfait.renderer.TextureHandle
import com.mrpowergamerbr.starfait.wad.Font
import com.mrpowergamerbr.starfait.wad.Font.Glyph
import com.mrpowergamerbr.starfait.wad.GameWAD
import com.mrpowergamerbr.starfait.wad.StringPointer

class RunnerFont private constructor(
    val name: String,
    val fontName: String,
    val size: UInt,
    val bold: Boolean,
    val italic: Boolean,
    val rangeStart: UShort,
    val rangeEnd: UInt,
    val texturePageEntry: RunnerTexturePageEntry,
    val scaleX: Float,
    val scaleY: Float,
    val glyphs: List<Glyph>
) {
    companion object {
        fun fromWAD(gameWAD: GameWAD, font: Font, textureHandles: Map<Short, TextureHandle>) = RunnerFont(
            font.name,
            font.fontName,
            font.size,
            font.bold,
            font.italic,
            font.rangeStart,
            font.rangeEnd,
            RunnerTexturePageEntry.fromWAD(gameWAD.tpag.entries[font.texturePageEntryPointer]!!, textureHandles),
            font.scaleX,
            font.scaleY,
            font.glyphs
        )
    }

    val characterToGlyph = glyphs.associateBy { it.character }
}