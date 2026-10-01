package com.mrpowergamerbr.starfait.vm

import kotlin.collections.set

abstract class VariableContainer(val vm: StarfaitVM) {
    val variables = mutableMapOf<Int, RValue>()

    /**
     * Writes a variable at [variableId] to [value]
     */
    fun setVariable(variableId: Int, value: RValue, arrayIndex: Int?) {
        if (arrayIndex != null) {
            // Arrays a bit trickier...
            // In this case, we want to WRITE to a specific slot in the array
            val arrayRValue = this.variables[variableId]
            val gmlArray = if (arrayRValue is RValue.Array) {
                arrayRValue.value
            } else {
                GMLArray()
            }

            gmlArray.entries[arrayIndex] = value

            // println("GMLArray elements: ${gmlArray.entries}")

            this.variables[variableId] = RValue.Array(gmlArray)
        } else {
            this.variables[variableId] = value
        }
    }

    /**
     * Gets the variable [variableId]
     */
    fun getVariable(variableId: Int, arrayIndex: Int?): RValue {
        val variable = this.variables[variableId] ?: error("Variable with VarID $variableId (${vm.getVariableNameFromVariableIndex(variableId)}) is not set before reading it")
        if (arrayIndex != null) {
            check(variable is RValue.Array) { "Variable with VarID $variableId (${vm.getVariableNameFromVariableIndex(variableId)}) is not an array, but we wanted to read arrayIndex $arrayIndex from it!" }
            val array = variable.value
            val variable = array.entries[arrayIndex] ?: error("Trying to read from array index $arrayIndex, but the element does not exist!")
            return variable
        } else {
            return variable
        }
    }
}