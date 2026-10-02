package com.mrpowergamerbr.starfait.disasm

import com.mrpowergamerbr.starfait.StarfaitByteBuffer
import com.mrpowergamerbr.starfait.vm.InstructionDataType
import com.mrpowergamerbr.starfait.vm.Op
import com.mrpowergamerbr.starfait.vm.OpWord
import com.mrpowergamerbr.starfait.vm.VariableReferenceOperand
import com.mrpowergamerbr.starfait.wad.GameWAD
import java.nio.ByteBuffer

class StarfaitDisassembler(val gameWAD: GameWAD) {
    fun disassembleAndPrint(bytecodeStream: ByteBuffer) {
        val reader = StarfaitByteBuffer.fromByteBuffer(bytecodeStream)

        while (reader.hasRemaining()) {
            val start = reader.position
            val word = OpWord(reader.readInt())

            val opcode = word.opcode()
            val type1 = word.type1()
            val type2 = word.type2()
            val extra = word.extra()

            val type1DataType = InstructionDataType.byId(type1)

            println("($start) [0x${word.value.toHexString()}] ${Op.getOpcodeName(opcode)} (type1: ${type1.toHexString()}, type2: ${type2.toHexString()}, extra: ${extra.toHexString()})")

            when (word.operandSize()) {
                0 -> {}
                4 -> {
                    when (type1DataType) {
                        InstructionDataType.DOUBLE -> TODO()
                        InstructionDataType.FLOAT -> TODO()
                        InstructionDataType.INT32 -> {
                            println("  operand (int32): ${reader.readInt()}")
                        }
                        InstructionDataType.INT64 -> TODO()
                        InstructionDataType.BOOLEAN -> TODO()
                        InstructionDataType.VARIABLE -> {
                            val variableOperand = VariableReferenceOperand(reader.readInt())
                            println("  operand (variable): ${variableOperand.variableIndex()} (has array index? ${variableOperand.hasArrayIndex()}, has instance ID from stack? ${variableOperand.hasInstanceIdOnStack()})")
                        }
                        InstructionDataType.STRING -> {
                            val index = reader.readInt()
                            println("  operand (string): $index -> \"${gameWAD.strg.stringsByIndex[index]}\"")
                        }
                        InstructionDataType.INT16 -> TODO()
                    }
                }
                8 -> {
                    when (type1DataType) {
                        InstructionDataType.DOUBLE -> {
                            println("  operand (int64): ${reader.readDouble()}")
                        }
                        InstructionDataType.FLOAT -> TODO()
                        InstructionDataType.INT32 -> TODO()
                        InstructionDataType.INT64 -> {
                            println("  operand (int64): ${reader.readLong()}")
                        }
                        InstructionDataType.BOOLEAN -> TODO()
                        InstructionDataType.VARIABLE -> TODO()
                        InstructionDataType.STRING -> TODO()
                        InstructionDataType.INT16 -> TODO()
                    }
                }
            }

            reader.position = start + 4 + word.operandSize()
        }
    }
}