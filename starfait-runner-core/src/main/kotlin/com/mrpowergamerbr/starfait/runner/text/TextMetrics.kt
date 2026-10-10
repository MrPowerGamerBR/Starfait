package com.mrpowergamerbr.starfait.runner.text

import com.mrpowergamerbr.starfait.wad.Font

data class TextMetrics(
    val width: Float,
    val height: Float,
    val lines: List<List<Character>>
) {
    data class Character(val x: Float, val y: Float, val glyph: Font.Glyph)
}