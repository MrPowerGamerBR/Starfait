package com.mrpowergamerbr.starfait

import com.mrpowergamerbr.starfait.vm.BuiltinVariable
import com.mrpowergamerbr.starfait.vm.FunctionReferenceOperand
import com.mrpowergamerbr.starfait.vm.VariableReferenceOperand
import com.mrpowergamerbr.starfait.wad.DumpNested
import com.mrpowergamerbr.starfait.wad.GameWAD
import com.mrpowergamerbr.starfait.wad.StringPointer
import com.mrpowergamerbr.starfait.wad.Variable
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

object StarfaitUtils {
    const val REGULAR_VARIABLES_BASE = 100_000

    /**
     * By default, each variable and function reference in the code references a delta offset on the `extra` field to the next reference of the variable.
     *
     * The remapper walks through all VARI variables and remaps the delta offsets to be VARI indices.
     */
    fun remapReferences(gameWAD: GameWAD): RemapResult {
        // I'm not sure WHY the YoYo Runner does this!!
        val reader = StarfaitByteBuffer.fromByteArray(gameWAD.code.bytecode)
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
            allocatedRegularVariables,
            allocatedBuiltinVariables
        )
    }

    data class RemapResult(
        val allocatedBuiltinVariables: List<Variable>,
        val allocatedRegularVariables: List<Variable>,
    )

    fun dump(gameWAD: GameWAD, starfaitObject: Any, depth: Int) {
        // We want to preserve the constructor order
        val constructorParametersName = starfaitObject::class
            .primaryConstructor!!
            .parameters
            .map { it.name }

        starfaitObject::class
            .memberProperties
            .sortedBy {
                val index = constructorParametersName.indexOf(it.name)
                if (index != -1) index else Int.MAX_VALUE
            }
            .forEach {
                var value = it.call(starfaitObject)
                if (it.hasAnnotation<DumpNested>()) {
                    println("  ".repeat(depth) + it.name + "[]: ")
                    for (element in value as List<*>) {
                        dump(gameWAD, element!!, depth + 1)
                    }
                } else {
                    if (value is StringPointer) {
                        value = gameWAD.strg.strings[value]
                    }

                    println("  ".repeat(depth) + it.name + ": " + value)
                }
            }
    }
}