package com.mrpowergamerbr.starfait.vm

import com.mrpowergamerbr.starfait.MathUtils
import com.mrpowergamerbr.starfait.StarfaitByteBuffer
import com.mrpowergamerbr.starfait.runner.Instance
import com.mrpowergamerbr.starfait.runner.ObjectBase
import com.mrpowergamerbr.starfait.wad.Code
import com.mrpowergamerbr.starfait.wad.GameWAD
import java.nio.ByteBuffer

class StarfaitVM(val gameWAD: GameWAD) {
    val stack = VMStack()
    val variables = mutableMapOf<Int, RValue>()
    val global = ObjectBase()
    var currentInstance: Instance? = null
    lateinit var currentCode: Code

    fun executeLoop(code: Code) {
        this.currentCode = code

        // We need to remap the bytecode relative offset field position
        val reader = StarfaitByteBuffer.fromByteBuffer(
            ByteBuffer.wrap(
                gameWAD.code.bytecode,
                // We do + on the offset because the offset is negative
                code.offset + (code.bytecodeRelativeOffsetFieldPosition + code.bytecodeRelativeOffset) - gameWAD.code.postAddressPosition,
                code.length.toInt() - code.offset
            )
        )

        while (reader.hasRemaining()) {
            val start = reader.position
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
                Op.PUSH -> handlePush(reader, type1, extra)
                Op.PUSH_GLOBAL -> handlePushGlobal(reader, type1)
                Op.PUSH_IMMEDIATE -> handlePushImmediate(reader, extra)
                Op.POP -> handlePop(reader, type1)
                Op.POPZ -> stack.pop()
                Op.CMP -> {
                    val right = stack.pop()
                    val left = stack.pop()
                    val diff = left.real - right.real
                    val comparisonFunction = comparisonFunction(word)
                    val result = when (comparisonFunction(word)) {
                        1 -> diff < -MathUtils.EPSILON
                        2 -> diff <= MathUtils.EPSILON
                        3 -> if (diff < -MathUtils.EPSILON) false else diff <= MathUtils.EPSILON
                        4 -> if (diff < -MathUtils.EPSILON) true else MathUtils.EPSILON < diff
                        5 -> -MathUtils.EPSILON <= diff
                        6 -> MathUtils.EPSILON < diff
                        else -> error("Unsupported comparsion function $comparisonFunction")
                    }
                    stack.push().setBoolean(result)
                }
                Op.CONV -> handleConv(reader, type1, type2)
                Op.BF -> {
                    val result = stack.pop().bool
                    if (!result)
                        reader.position = start + branchOffset(word)
                }
                Op.BT -> {
                    val result = stack.pop().bool
                    if (result)
                        reader.position = start + branchOffset(word)
                }
                Op.B -> {
                    reader.position = start + branchOffset(word)
                }
                Op.CALL -> handleCall(reader, extra)
                else -> error("Found unknown opcode (${opcode.toByte().toHexString()}) while executing \"${gameWAD.strg.getString(code.name)}\"")
            }
        }
    }

    private fun handlePush(reader: StarfaitByteBuffer, type1: Int, extra: Int) {
        val type1DataType = InstructionDataType.byId(type1)

        when (type1DataType) {
            InstructionDataType.INT16 -> TODO()
            InstructionDataType.INT32 -> TODO()
            InstructionDataType.INT64 -> TODO()
            InstructionDataType.DOUBLE -> TODO()
            InstructionDataType.FLOAT -> TODO()
            InstructionDataType.BOOLEAN -> TODO()
            InstructionDataType.VARIABLE -> {
                val operand = reader.readInt()

                val arrayIndex = if (hasArrayIndex(operand)) stack.pop().int else null
                val instanceId = if (hasInstanceIdOnStack(operand)) stack.pop().int else extra

                // TODO: Use correct instance here!
                pushVariableFromObject(
                    when (instanceId) {
                        SpecialObjectReferenceType.SELF -> requireNotNull(currentInstance)
                        SpecialObjectReferenceType.GLOBAL -> global
                        else -> error("Unsupported instance ID $instanceId!")
                    },
                    operand
                )
            }
            InstructionDataType.STRING -> stack.push().setString(gameWAD.strg.stringsByIndex[reader.readInt()])
        }
    }

    private fun handlePushGlobal(reader: StarfaitByteBuffer, type1: Int) {
        // TODO: Read the variable from the global variables
        val type1DataType = InstructionDataType.byId(type1)

        when (type1DataType) {
            InstructionDataType.INT16 -> TODO()
            InstructionDataType.INT32 -> TODO()
            InstructionDataType.INT64 -> TODO()
            InstructionDataType.DOUBLE -> TODO()
            InstructionDataType.FLOAT -> TODO()
            InstructionDataType.BOOLEAN -> TODO()
            InstructionDataType.VARIABLE -> {
                val operand = reader.readInt()
                pushVariableFromObject(global, operand)
            }
            InstructionDataType.STRING -> stack.push().setString(gameWAD.strg.stringsByIndex[reader.readInt()])
        }
    }

    private fun pushVariableFromObject(objectBase: ObjectBase, operand: Int) {
        val variable = objectBase.variables[variableIndex(operand)]
        val pushedRValue = stack.push()
        if (variable == null) {
            pushedRValue.setUndefined()
        } else {
            pushedRValue.copyFrom(variable)
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
                val operand = reader.readInt()
                variables[variableIndex(operand)] = stack.pop().makeIndependent()
            }
            InstructionDataType.STRING -> TODO()
            InstructionDataType.INT16 -> TODO()
        }
    }

    private fun handleConv(reader: StarfaitByteBuffer, type1: Int, type2: Int) {
        // type1 is the source type, type2 is the destination type
        val sourceType = InstructionDataType.byId(type1)
        val destinationType = InstructionDataType.byId(type2)
        println("Converting from $sourceType to $destinationType")
        val topOfTheStack = stack.peek()

        // no-op, this is only useful if some day we decide to go with tagged variables (that is, using two arrays, one for native values and another for RValues)
        // Because in that case, we would need to convert the RValue to the native type
        if (destinationType == InstructionDataType.VARIABLE)
            return

        TODO()
    }

    private fun handlePushImmediate(reader: StarfaitByteBuffer, extra: Int) {
        stack.push().setInt(extra)
    }

    private fun handleCall(reader: StarfaitByteBuffer, extra: Int) {
        val functionIndex = reader.readInt()

        // extra = parameter count
        val arguments = ArrayList<RValue>(extra)
        repeat(extra) {
            arguments.add(stack.pop())
        }

        // For now we don't really care
        println("[${gameWAD.strg.getString(currentCode.name)}] Unknown function! ${gameWAD.strg.getString(gameWAD.func.functions[functionIndex].name)}, parameters: ${arguments.joinToString { it.toStringTyped() }}")

        stack.push().setUndefined()
    }

    /**
     * By default, each variable and function reference in the code references a delta offset on the `extra` field to the next reference of the variable.
     *
     * The remapper walks through all VARI variables and remaps the delta offsets to be VARI indices.
     */
    fun remapReferences() {
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

        for ((index, function) in gameWAD.func.functions.withIndex()) {
            if (function.firstAddress != -1) {
                // This points to the INSTRUCTION address, NOT the operand address, which is why we do +4
                var operandAddress = (function.firstAddress - gameWAD.code.postAddressPosition) + 4

                repeat(function.occurrenceCount) {
                    reader.position = operandAddress

                    val operand = reader.readInt()
                    val delta = delta(operand)

                    reader.position = operandAddress
                    reader.writeInt(index) // For functions we don't need to "save" the nibble (w00t!)

                    operandAddress += delta
                }
            }
        }
    }

    private fun opcode(word: Int) = (word ushr 24) and 0xFF
    private fun type1(word: Int) = (word ushr 16) and 0xF
    private fun type2(word: Int) = (word ushr 20) and 0xF
    private fun extra(word: Int) = word.toShort().toInt()
    private fun delta(operand: Int) = (operand shl 4) shr 4 // low 28 bits
    private fun variableIndex(operand: Int) = (operand shl 4) shr 4 // low 28 bits
    private fun comparisonFunction(word: Int) = (word ushr 8) and 0xF
    private fun branchOffset(word: Int) = (word shl 9) shr 7
    private fun referenceFlags(operand: Int) = operand ushr 28
    private fun hasArrayIndex(operand: Int) = operand >= 0
    private fun hasInstanceIdOnStack(operand: Int) = (operand and 0x20000000) == 0
    private fun isRoomInstanceId(operand: Int) = (operand and 0x40000000) != 0
}