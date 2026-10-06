package com.mrpowergamerbr.starfait.wad

class Font(
    val name: StringPointer,
    val fontName: StringPointer,
    val size: UInt,
    val bold: Boolean,
    val italic: Boolean,
    val rangeStart: UShort,
    val rangeEnd: UInt,
    val texturePageEntryPointer: TexturePageEntryPointer,
    val scaleX: Float,
    val scaleY: Float,
    val glyphs: List<Glyph>
) {
    val characterToGlyph = glyphs.associateBy { it.character }

    class Glyph(
        val character: Char,
        // Relative to the font's texture page entry, not to the texture page itself
        val x: UShort,
        val y: UShort,
        val width: UShort,
        val height: UShort,
        val shift: Short,
        val offset: Short
    )
}
