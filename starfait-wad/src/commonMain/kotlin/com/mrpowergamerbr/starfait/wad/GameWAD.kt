package com.mrpowergamerbr.starfait.wad

import com.mrpowergamerbr.starfait.StarfaitByteBuffer
import com.mrpowergamerbr.starfait.wad.chunks.AUDO
import com.mrpowergamerbr.starfait.wad.chunks.BGND
import com.mrpowergamerbr.starfait.wad.chunks.CODE
import com.mrpowergamerbr.starfait.wad.chunks.FONT
import com.mrpowergamerbr.starfait.wad.chunks.FUNC
import com.mrpowergamerbr.starfait.wad.chunks.GEN8
import com.mrpowergamerbr.starfait.wad.chunks.OBJT
import com.mrpowergamerbr.starfait.wad.chunks.OPTN
import com.mrpowergamerbr.starfait.wad.chunks.PATH
import com.mrpowergamerbr.starfait.wad.chunks.ROOM
import com.mrpowergamerbr.starfait.wad.chunks.SCPT
import com.mrpowergamerbr.starfait.wad.chunks.SOND
import com.mrpowergamerbr.starfait.wad.chunks.SPRT
import com.mrpowergamerbr.starfait.wad.chunks.STRG
import com.mrpowergamerbr.starfait.wad.chunks.TPAG
import com.mrpowergamerbr.starfait.wad.chunks.TXTR
import com.mrpowergamerbr.starfait.wad.chunks.VARI

class GameWAD(
    val gen8: GEN8,
    val strg: STRG,
    val path: PATH,
    val audo: AUDO,
    val sond: SOND,
    val scpt: SCPT,
    val tpag: TPAG,
    val optn: OPTN,
    val bgnd: BGND,
    val txtr: TXTR,
    val font: FONT,
    val sprt: SPRT,
    val objt: OBJT,
    val room: ROOM,
    val code: CODE,
    val vari: VARI,
    val func: FUNC,
    val tpagRegistry: TexturePageEntryRegistry
) {
    companion object {
        fun read(reader: StarfaitByteBuffer): GameWAD {
            val magic = reader.readUTF8String(4)

            require(magic == "FORM") { "Invalid magic value!" }

            val wadSize = reader.readInt()

            // wadSize + the form magic + the wad size int
            // This is essentially an IFF file format
            data class ChunkInfo(val size: Int, val position: Int)
            val chunkNameToPosition = mutableMapOf<String, ChunkInfo>()

            while (wadSize + 4 + 4 > reader.position) {
                val chunkName = reader.readUTF8String(4)
                val chunkSize = reader.readInt()

                val startOfChunk = reader.position

                chunkNameToPosition[chunkName] = ChunkInfo(chunkSize, startOfChunk)

                reader.position = startOfChunk + chunkSize
            }

            fun <T> readChunkAtPosition(chunkInfo: ChunkInfo, block: (StarfaitByteBuffer, Int) -> T): T {
                reader.position = chunkInfo.position
                return block(reader, chunkInfo.size)
            }

            // You may be wondering "wow this SUCKS why does THIS EXIST?!?!"
            // Because some WAD files were modded with WinPack/TranslaTale, and those WADs decided that it was a good idea:tm: to store data outside of the TPAG chunk!
            // So we need to have a separate TPAG registry
            val tpagRegistry = TexturePageEntryRegistry()

            val gen8 = readChunkAtPosition(chunkNameToPosition["GEN8"]!!) { reader, _ -> GEN8.read(reader) }
            val strg = readChunkAtPosition(chunkNameToPosition["STRG"]!!) { reader, _ -> STRG.read(reader) }
            val path = readChunkAtPosition(chunkNameToPosition["PATH"]!!) { reader, _ -> PATH.read(reader) }
            val audo = readChunkAtPosition(chunkNameToPosition["AUDO"]!!) { reader, _ -> AUDO.read(reader) }
            val sond = readChunkAtPosition(chunkNameToPosition["SOND"]!!) { reader, _ -> SOND.read(reader) }
            val scpt = readChunkAtPosition(chunkNameToPosition["SCPT"]!!) { reader, _ -> SCPT.read(reader) }
            val tpag = readChunkAtPosition(chunkNameToPosition["TPAG"]!!) { reader, _ -> TPAG.read(reader, tpagRegistry) }
            val optn = readChunkAtPosition(chunkNameToPosition["OPTN"]!!) { reader, _ -> OPTN.read(reader) }
            val bgnd = readChunkAtPosition(chunkNameToPosition["BGND"]!!) { reader, _ -> BGND.read(reader, tpagRegistry) }
            val txtr = readChunkAtPosition(chunkNameToPosition["TXTR"]!!) { reader, _ -> TXTR.read(reader) }
            val font = readChunkAtPosition(chunkNameToPosition["FONT"]!!) { reader, chunkSize -> FONT.read(reader, chunkSize, tpagRegistry) }
            val sprt = readChunkAtPosition(chunkNameToPosition["SPRT"]!!) { reader, _ -> SPRT.read(reader, tpagRegistry) }
            val objt = readChunkAtPosition(chunkNameToPosition["OBJT"]!!) { reader, _ -> OBJT.read(reader) }
            val room = readChunkAtPosition(chunkNameToPosition["ROOM"]!!) { reader, _ -> ROOM.read(reader, gen8.wadVersion) }
            val code = readChunkAtPosition(chunkNameToPosition["CODE"]!!) { reader, _ -> CODE.read(reader) }
            val vari = readChunkAtPosition(chunkNameToPosition["VARI"]!!) { reader, chunkSize -> VARI.read(reader, chunkSize) }
            val func = readChunkAtPosition(chunkNameToPosition["FUNC"]!!) { reader, chunkSize -> FUNC.read(reader, chunkSize) }

            return GameWAD(
                gen8,
                strg,
                path,
                audo,
                sond,
                scpt,
                tpag,
                optn,
                bgnd,
                txtr,
                font,
                sprt,
                objt,
                room,
                code,
                vari,
                func,
                tpagRegistry
            )
        }
    }
}