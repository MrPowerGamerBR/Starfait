package com.mrpowergamerbr.starfait.vm

import com.mrpowergamerbr.starfait.StarfaitByteReader
import com.mrpowergamerbr.starfait.wad.Code
import com.mrpowergamerbr.starfait.wad.GameWAD

class StarfaitVM {
    fun executeLoop(gameWAD: GameWAD, code: Code) {
        val reader = StarfaitByteReader(code.data)

        while (code.data.size > reader.position) {
            val word = reader.readInt()

            val opcode = opcode(word)
            val type1 = type1(word)
            val type2 = type2(word)
            val extra = extra(word)

            println("opcode: ${opcode.toHexString()}, type1: ${type1.toHexString()}, type2: ${type2.toHexString()}, extra: ${extra.toHexString()}")

            when (opcode) {
                Op.PUSH -> {
                    when (type1) {
                        6 -> {
                            val stringIndex = reader.readInt()
                            println("Push ${gameWAD.strg.stringsByIndex[stringIndex]} to the stack")
                        }
                    }
                }
                Op.POP -> {
                    when (type1) {
                        5 -> {
                            reader.readInt()
                        }
                    }
                }
                else -> error("Found unknown opcode (${opcode.toByte().toHexString()}) while executing \"${gameWAD.strg.getString(code.name)}\"")
            }
        }
    }

    private fun opcode(word: Int) = (word ushr 24) and 0xFF
    private fun type1(word: Int) = (word ushr 16) and 0xF
    private fun type2(word: Int) = (word ushr 20) and 0xF
    private fun extra(word: Int) = word.toShort().toInt()
}