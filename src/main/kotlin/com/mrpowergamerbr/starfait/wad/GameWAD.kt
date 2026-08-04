package com.mrpowergamerbr.starfait.wad

import com.mrpowergamerbr.starfait.StarfaitByteBuffer
import com.mrpowergamerbr.starfait.wad.chunks.AUDO
import com.mrpowergamerbr.starfait.wad.chunks.BGND
import com.mrpowergamerbr.starfait.wad.chunks.CODE
import com.mrpowergamerbr.starfait.wad.chunks.FONT
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
    val vari: VARI
) {
    companion object {
        fun read(reader: StarfaitByteBuffer): GameWAD {
            val magic = reader.readUTF8String(4)

            require(magic == "FORM") { "Invalid magic value!" }

            val wadSize = reader.readInt()

            var gen8: GEN8? = null
            var strg: STRG? = null
            var path: PATH? = null
            var audo: AUDO? = null
            var sond: SOND? = null
            var scpt: SCPT? = null
            var tpag: TPAG? = null
            var optn: OPTN? = null
            var bgnd: BGND? = null
            var txtr: TXTR? = null
            var font: FONT? = null
            var sprt: SPRT? = null
            var objt: OBJT? = null
            var room: ROOM? = null
            var code: CODE? = null
            var vari: VARI? = null

            // wadSize + the form magic + the wad size int
            // This is essentially an IFF file format
            while (wadSize + 4 + 4 > reader.position) {
                val chunkName = reader.readUTF8String(4)
                val chunkSize = reader.readInt()

                val startOfChunk = reader.position

                when (chunkName) {
                    "GEN8" -> gen8 = GEN8.read(reader)
                    "STRG" -> strg = STRG.read(reader)
                    "PATH" -> path = PATH.read(reader)
                    "AUDO" -> audo = AUDO.read(reader)
                    "SOND" -> sond = SOND.read(reader)
                    "SCPT" -> scpt = SCPT.read(reader)
                    "TPAG" -> tpag = TPAG.read(reader)
                    "OPTN" -> optn = OPTN.read(reader)
                    "BGND" -> bgnd = BGND.read(reader)
                    "TXTR" -> txtr = TXTR.read(reader)
                    "FONT" -> font = FONT.read(reader, chunkSize)
                    "SPRT" -> sprt = SPRT.read(reader)
                    "OBJT" -> objt = OBJT.read(reader)
                    "ROOM" -> room = ROOM.read(reader)
                    "CODE" -> code = CODE.read(reader)
                    "VARI" -> vari = VARI.read(reader, chunkSize)
                    else -> println("Unknown chunk $chunkName! (size: $chunkSize)")
                }

                reader.position = startOfChunk + chunkSize
            }

            requireNotNull(gen8)
            requireNotNull(strg)
            requireNotNull(path)
            requireNotNull(audo)
            requireNotNull(sond)
            requireNotNull(scpt)
            requireNotNull(tpag)
            requireNotNull(optn)
            requireNotNull(bgnd)
            requireNotNull(txtr)
            requireNotNull(font)
            requireNotNull(sprt)
            requireNotNull(objt)
            requireNotNull(room)
            requireNotNull(code)
            requireNotNull(vari)

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
                vari
            )
        }
    }
}