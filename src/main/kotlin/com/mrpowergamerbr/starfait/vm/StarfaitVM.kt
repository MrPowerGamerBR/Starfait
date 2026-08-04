package com.mrpowergamerbr.starfait.vm

import com.mrpowergamerbr.starfait.StarfaitByteReader
import com.mrpowergamerbr.starfait.wad.Code
import com.mrpowergamerbr.starfait.wad.GameWAD

class StarfaitVM(val gameWAD: GameWAD) {
    val stack = VMStack()

    fun executeLoop(code: Code) {
        val reader = StarfaitByteReader(code.data)

        while (code.data.size > reader.position) {
            val word = reader.readInt()

            val opcode = opcode(word)
            val type1 = type1(word)
            val type2 = type2(word)
            val extra = extra(word)

            println("opcode: ${opcode.toHexString()}, type1: ${type1.toHexString()}, type2: ${type2.toHexString()}, extra: ${extra.toHexString()}")

            if (stack.top != 0) {
                print("stack[${stack.top}] = ")
                for (i in 0 until stack.top) {
                    print(stack.stack[i].toStringTyped())
                    print(" ")
                }
                println()
            }

            when (opcode) {
                Op.PUSH -> handlePush(reader, type1)
                Op.POP -> handlePop(reader, type1)
                else -> error("Found unknown opcode (${opcode.toByte().toHexString()}) while executing \"${gameWAD.strg.getString(code.name)}\"")
            }
        }
    }

    private fun handlePush(reader: StarfaitByteReader, type1: Int) {
        val type1DataType = InstructionDataType.byId(type1)

        when (type1DataType) {
            InstructionDataType.INT16 -> {}
            InstructionDataType.INT32 -> reader.readInt()
            InstructionDataType.INT64 -> reader.readLong()
            InstructionDataType.DOUBLE -> reader.readDouble()
            InstructionDataType.FLOAT -> reader.readFloat()
            InstructionDataType.BOOLEAN -> reader.readIntBoolean()
            InstructionDataType.VARIABLE -> reader.readInt()
            InstructionDataType.STRING -> stack.push().setString(gameWAD.strg.stringsByIndex[reader.readInt()])
        }
    }

    private fun handlePop(reader: StarfaitByteReader, type1: Int) {
        val type1DataType = InstructionDataType.byId(type1)

        when (type1DataType) {
            InstructionDataType.DOUBLE -> TODO()
            InstructionDataType.FLOAT -> TODO()
            InstructionDataType.INT32 -> TODO()
            InstructionDataType.INT64 -> TODO()
            InstructionDataType.BOOLEAN -> TODO()
            InstructionDataType.VARIABLE -> {
                reader.readInt()
                stack.pop()
            }
            InstructionDataType.STRING -> TODO()
            InstructionDataType.INT16 -> TODO()
        }
    }

    private fun opcode(word: Int) = (word ushr 24) and 0xFF
    private fun type1(word: Int) = (word ushr 16) and 0xF
    private fun type2(word: Int) = (word ushr 20) and 0xF
    private fun extra(word: Int) = word.toShort().toInt()
}