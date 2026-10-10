package com.mrpowergamerbr.starfait.runner

import com.mrpowergamerbr.starfait.runner.TextMetrics
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

        var stringWidth = 0f
        var stringHeight = 0f

        var x = 0f
        var y = 0f

        val characters = mutableListOf<TextMetrics.Character>()

        for (line in lines) {
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

            stringHeight += maxHeightOnThisLine
            x = 0.0f
            y += maxHeightOnThisLine
        }

        return TextMetrics(
            width = stringWidth,
            height = stringHeight,
            characters = characters
        )
    }
}