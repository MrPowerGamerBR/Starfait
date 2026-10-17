package com.mrpowergamerbr.starfait.wad.chunks

import com.mrpowergamerbr.starfait.StarfaitByteBuffer
import com.mrpowergamerbr.starfait.wad.Font

class FONT(
    val fonts: List<Font>,
    val codePageTranslation: List<UShort>
) {
    companion object {
        // The last 0x200 bytes of the chunk are a code page translation table
        private const val CODE_PAGE_TRANSLATION_SIZE = 0x200

        fun read(reader: StarfaitByteBuffer, chunkSize: Int): FONT {
            val startOfChunk = reader.position

            val addresses = reader.readAddresses()

            val fonts = ArrayList<Font>(addresses.size)
            for (address in addresses) {
                reader.position = address

                val name = reader.readStringPointerAndReadNullTerminatedString()
                val fontName = reader.readStringPointerAndReadNullTerminatedString()
                val size = reader.readUInt()
                val bold = reader.readIntBoolean()
                val italic = reader.readIntBoolean()
                // rangeStart and the two unnamed bytes share a single dword
                val rangeStart = reader.readUShort()
                reader.readByte() // unknown
                reader.readByte() // unknown
                val rangeEnd = reader.readUInt()
                val texturePageEntryPointer = reader.readTexturePageEntryPointer()
                val scaleX = reader.readFloat()
                val scaleY = reader.readFloat()
                val glyphAddresses = reader.readAddresses()

                val glyphs = ArrayList<Font.Glyph>(glyphAddresses.size)
                for (glyphAddress in glyphAddresses) {
                    reader.position = glyphAddress

                    val character = Char(reader.readUShort())
                    val x = reader.readUShort()
                    val y = reader.readUShort()
                    val width = reader.readUShort()
                    val height = reader.readUShort()
                    val shift = reader.readShort()
                    val offset = reader.readShort()
                    reader.readUShort() // Padding, always zero in every glyph of every font

                    glyphs.add(
                        Font.Glyph(
                            character = character,
                            x = x,
                            y = y,
                            width = width,
                            height = height,
                            shift = shift,
                            offset = offset
                        )
                    )
                }

                fonts.add(
                    Font(
                        name = name,
                        fontName = fontName,
                        size = size,
                        bold = bold,
                        italic = italic,
                        rangeStart = rangeStart,
                        rangeEnd = rangeEnd,
                        texturePageEntryPointer = texturePageEntryPointer,
                        scaleX = scaleX,
                        scaleY = scaleY,
                        glyphs = glyphs
                    )
                )
            }

            reader.position = startOfChunk + chunkSize - CODE_PAGE_TRANSLATION_SIZE

            // Used for font_add to check if a character is in the ASCII table or not
            // Why it is done like that instead of just checking if 127 >= c? I have no idea
            val codePageTranslation = ArrayList<UShort>(CODE_PAGE_TRANSLATION_SIZE / 2)
            repeat(CODE_PAGE_TRANSLATION_SIZE / 2) {
                codePageTranslation.add(reader.readUShort())
            }

            return FONT(fonts, codePageTranslation)
        }
    }
}
