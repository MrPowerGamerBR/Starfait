package com.mrpowergamerbr.starfait.vm

import com.mrpowergamerbr.starfait.StarfaitByteBuffer
import com.mrpowergamerbr.starfait.wad.Code
import com.mrpowergamerbr.starfait.wad.GameWAD
import java.nio.ByteBuffer

class StarfaitVM(val gameWAD: GameWAD) {
    val stack = VMStack()

    /**
     * By default, each variable reference in the code references a delta offset on the `extra` field to the next reference of the variable.
     *
     * The remapper walks through all VARI variables and remaps the delta offsets to be VARI indices.
     */
    fun remapVariableReferences() {
        // I'm not sure WHY the YoYo Runner does this!!
        val reader = StarfaitByteBuffer.fromByteArray(gameWAD.code.bytecode)

        for ((index, variable) in gameWAD.vari.variables.withIndex()) {
            if (variable.firstAddress != -1) {
                // This points to the INSTRUCTION address, NOT the operand address, which is why we do +4
                var operandAddress = (variable.firstAddress - gameWAD.code.postAddressPosition) + 4

                repeat(variable.occurrenceCount) {
                    reader.position = operandAddress

                    val operand = reader.readInt()
                    val delta = delta(operand)

                    reader.position = operandAddress
                    reader.writeInt((operand and 0xF0000000.toInt()) or (index and 0x0FFFFFFF))

                    operandAddress += delta
                }
            }
        }
    }

    fun executeLoop(code: Code) {
        // We need to remap the bytecode relative offset field position
        val reader = StarfaitByteBuffer.fromByteBuffer(
            ByteBuffer.wrap(
                gameWAD.code.bytecode,
                // We do + on the offset because the offset is negative
                (code.bytecodeRelativeOffsetFieldPosition + code.bytecodeRelativeOffset) - gameWAD.code.postAddressPosition,
                code.length.toInt()
            )
        )

        while (reader.hasRemaining()) {
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

    private fun handlePush(reader: StarfaitByteBuffer, type1: Int) {
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

    private fun handlePop(reader: StarfaitByteBuffer, type1: Int) {
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
    private fun delta(operand: Int) = (operand shl 4) shr 4 // low 28 bits
}