package com.mrpowergamerbr.starfait.runner.text

import com.mrpowergamerbr.starfait.runner.resources.RunnerFont
import kotlin.math.max
import kotlin.text.iterator

object TextUtils {
    /**
     * Gets the position of each glyph when rendering the [text] with the specified [font], using the [xscale] and [yscale]
     *
     * @param font the font used for the text
     * @param text the text to measure
     * @param xscale the xscale of the text
     * @param yscale the yscale of the text
     * @return the position of each glyph, with the width and height of the text
     */
    fun getTextMetrics(font: RunnerFont, text: String, xscale: Float, yscale: Float): TextMetrics {
        // TODO: We need to change this for GameMaker: Studio 2+!
        val lines = text.split("#")

        var x = 0f
        var y = 0f

        val characterLines = mutableListOf<TextMetrics.LineMetrics>()

        for (line in lines) {
            val characters = mutableListOf<TextMetrics.Character>()

            var stringWidth = 0f

            var maxHeightOnThisLine = 0f
            for (char in line) {
                val glyph = font.characterToGlyph[char]

                if (glyph != null) {
                    x += (glyph.offset * xscale)
                    stringWidth += (glyph.offset * xscale)

                    characters.add(
                        TextMetrics.Character(
                            x,
                            y,
                            glyph
                        )
                    )

                    stringWidth += (glyph.shift * xscale)
                    x += (glyph.shift * xscale)

                    maxHeightOnThisLine = max(maxHeightOnThisLine, (glyph.height.toFloat() * yscale))
                }
            }

            x = 0.0f
            y += maxHeightOnThisLine

            characterLines.add(TextMetrics.LineMetrics(stringWidth, maxHeightOnThisLine, characters))
        }

        return TextMetrics(
            width = characterLines.maxOfOrNull { it.width } ?: 0f,
            height = characterLines.maxOfOrNull { it.height } ?: 0f,
            lines = characterLines
        )
    }
}