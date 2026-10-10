package com.mrpowergamerbr.starfait.runner

import com.mrpowergamerbr.starfait.wad.Font

data class TextMetrics(
    val width: Float,
    val height: Float,
    val characters: List<Character>
) {
    data class Character(val x: Float, val y: Float, val glyph: Font.Glyph)
}