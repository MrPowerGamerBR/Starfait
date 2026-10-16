package com.mrpowergamerbr.starfait

import com.mrpowergamerbr.starfait.vm.FunctionReferenceOperand
import com.mrpowergamerbr.starfait.vm.VariableReferenceOperand
import com.mrpowergamerbr.starfait.wad.GameWAD
import com.mrpowergamerbr.starfait.wad.Variable

object StarfaitUtils {
    const val REGULAR_VARIABLES_BASE = 100_000

    /**
     * By default, each variable and function reference in the code references a delta offset on the `extra` field to the next reference of the variable.
     *
     * The remapper walks through all VARI variables and remaps the delta offsets to be VARI indices.
     */
    fun remapReferences(gameWAD: GameWAD): RemapResult {
        // I'm not sure WHY the YoYo Runner does this!!
        val reader = StarfaitByteBuffer.fromByteArray(gameWAD.code.bytecode, 0)
        val allocatedBuiltinVariables = mutableListOf<Variable>()
        val allocatedRegularVariables = mutableListOf<Variable>()

        for (variable in gameWAD.vari.variables) {
            if (variable.firstAddress != -1) {
                // Here's the thing:
                // For builtin variables, we store it sequentially, starting from 0
                // For regular variables, we store it sequentially, starting from REGULAR_VARIABLES_BASE
                // This way we can differentiate directly on the operand itself, and we can store the handlers densely
                var variableHandlerId: Int
                if (variable.varId == -6) {
                    val index = allocatedBuiltinVariables.size
                    allocatedBuiltinVariables.add(variable)
                    variableHandlerId = index
                } else {
                    val index = allocatedRegularVariables.size
                    allocatedRegularVariables.add(variable)
                    variableHandlerId = index + REGULAR_VARIABLES_BASE
                }

                // This points to the INSTRUCTION address, NOT the operand address, which is why we do +4
                var operandAddress = (variable.firstAddress - gameWAD.code.postAddressPosition) + 4

                repeat(variable.occurrenceCount) {
                    reader.position = operandAddress

                    val operand = VariableReferenceOperand(reader.readInt())
                    val delta = operand.delta()

                    reader.position = operandAddress
                    reader.writeInt((operand.value and 0xF0000000.toInt()) or (variableHandlerId and 0x0FFFFFFF))

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

                    val operand = FunctionReferenceOperand(reader.readInt())
                    val delta = operand.delta()

                    reader.position = operandAddress
                    reader.writeInt(index) // For functions we don't need to "save" the nibble (w00t!)

                    operandAddress += delta
                }
            }
        }

        return RemapResult(
            allocatedBuiltinVariables,
            allocatedRegularVariables
        )
    }

    data class RemapResult(
        val allocatedBuiltinVariables: List<Variable>,
        val allocatedRegularVariables: List<Variable>,
    )
}