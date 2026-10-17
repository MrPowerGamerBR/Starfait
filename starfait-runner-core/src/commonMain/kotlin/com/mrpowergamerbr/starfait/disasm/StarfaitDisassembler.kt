package com.mrpowergamerbr.starfait.disasm

import com.mrpowergamerbr.starfait.StarfaitByteBuffer
import com.mrpowergamerbr.starfait.vm.InstructionDataType
import com.mrpowergamerbr.starfait.vm.Op
import com.mrpowergamerbr.starfait.vm.OpWord
import com.mrpowergamerbr.starfait.vm.VMStack
import com.mrpowergamerbr.starfait.wad.GameWAD

class StarfaitDisassembler(val gameWAD: GameWAD) {
    companion object {
        fun readAndDisassembleOp(gameWAD: GameWAD, reader: StarfaitByteBuffer, stack: VMStack?): String {
            val start = reader.position
            val word = OpWord(reader.readInt())

            val opcode = word.opcode()
            val type1 = word.type1()
            val type2 = word.type2()
            val extra = word.extra()

            val type1DataTypeName = try { InstructionDataType.byId(type1) } catch (e: IllegalStateException) { "UNKNOWN" }
            val type2DataTypeName = try { InstructionDataType.byId(type2) } catch (e: IllegalStateException) { "UNKNOWN" }

            val operand4 = if (word.operandSize() == 4) reader.readInt() else null
            val operand8 = if (word.operandSize() == 8) reader.readLong() else null

            val additionalParams = mutableMapOf<String, Any>()

            val string = buildString {
                append("($start) [0x${word.value.toHexString()}] ${Op.getOpcodeName(opcode)} (type1: ${type1.toHexString()} ($type1DataTypeName), type2: ${type2.toHexString()} ($type2DataTypeName), extra: ${extra.toHexString()})")

                when (opcode) {
                    Op.CALL -> {
                        val func = gameWAD.func.functions[operand4!!]
                        additionalParams["argCount"] = extra
                        additionalParams["func"] = func.name
                    }
                }

                if (additionalParams.isNotEmpty()) {
                    var isFirst = false
                    append(" {")
                    additionalParams.forEach { (name, value) ->
                        if (isFirst) {
                            append(",")
                        } else {
                            isFirst = true
                        }
                        append("$name=$value")
                    }
                    append("}")
                }

                if (stack != null) {
                    append(" [stack=${stack.top} [")
                    for (i in 0 until stack.top) {
                        if (i != 0)
                            append(", ")
                        val value = stack.values[i]
                        append(value.toStringTyped())
                    }
                    append("]]")
                }

                appendLine()
            }
            return string
        }
    }

    fun disassembleAndPrint(bytecodeStream: ByteArray) {
        val reader = StarfaitByteBuffer.fromByteArray(bytecodeStream, 0)

        while (reader.hasRemaining()) {
            print(readAndDisassembleOp(gameWAD, reader, null))
        }
    }
}