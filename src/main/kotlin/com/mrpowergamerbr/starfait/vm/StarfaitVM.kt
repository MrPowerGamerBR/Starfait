package com.mrpowergamerbr.starfait.vm

import com.mrpowergamerbr.starfait.MathUtils
import com.mrpowergamerbr.starfait.StarfaitByteBuffer
import com.mrpowergamerbr.starfait.runner.Instance
import com.mrpowergamerbr.starfait.runner.ObjectBase
import com.mrpowergamerbr.starfait.runner.StarfaitRunner
import com.mrpowergamerbr.starfait.wad.Code
import com.mrpowergamerbr.starfait.wad.GameWAD
import com.mrpowergamerbr.starfait.wad.Variable
import java.nio.ByteBuffer

class StarfaitVM(
    val gameWAD: GameWAD,
    val gmlScriptsOpcodesToBeTraced: Set<String>
) {
    companion object {
        private const val REGULAR_VARIABLES_BASE = 100_000
    }

    lateinit var runner: StarfaitRunner
    val stack = VMStack()
    val global = ObjectBase()
    var currentInstance: Instance? = null
    val builtins = VMBuiltins(this)
    lateinit var builtinVariableHandlers: Array<BuiltinVariable>
    val callFrameStack = ArrayDeque<CallFrame>()
    private var currentOrNullCallFrame: CallFrame? = null
    val currentCallFrame: CallFrame
        get() = currentOrNullCallFrame ?: error("No current call frame!")

    fun executeCode(code: Code, arguments: Array<RValue>) {
        val callFrame = CallFrame(code, arguments, mutableMapOf(), stack.top)
        this.callFrameStack.addLast(callFrame)
        this.currentOrNullCallFrame = callFrame

        // We need to remap the bytecode relative offset field position
        val bytecodeInstructions = ByteBuffer.wrap(
            gameWAD.code.bytecode,
            // We do + on the offset because the offset is negative
            code.offset + (code.bytecodeRelativeOffsetFieldPosition + code.bytecodeRelativeOffset) - gameWAD.code.postAddressPosition,
            code.length.toInt() - code.offset
        )

        executeBytecodeInstructions(bytecodeInstructions)

        check(callFrame.stackTopWhenTheCallFrameWasCreated == stack.top) { "Unbalanced stack! Stack top is ${stack.top} when it should've been ${callFrame.stackTopWhenTheCallFrameWasCreated}! CallFrameStack is ${callFrameStack.joinToString { gameWAD.strg.getString(it.code.name) }}"}

        this.callFrameStack.removeLast()
        this.currentOrNullCallFrame = this.callFrameStack.lastOrNull()
    }

    private fun executeBytecodeInstructions(bytecodeStream: ByteBuffer) {
        val reader = StarfaitByteBuffer.fromByteBuffer(bytecodeStream)

        while (reader.hasRemaining()) {
            val start = reader.position
            val word = OpWord(reader.readInt())

            val opcode = word.opcode()
            val type1 = word.type1()
            val type2 = word.type2()
            val extra = word.extra()

            val scriptName = gameWAD.strg.getString(currentCallFrame.code.name)
            if (gmlScriptsOpcodesToBeTraced.contains("*") || gmlScriptsOpcodesToBeTraced.contains(scriptName)) {
                print("VM: [${gameWAD.strg.getString(currentCallFrame.code.name)}] [0x${word.value.toHexString()}] ${Op.getOpcodeName(opcode)} (type1: ${type1.toHexString()}, type2: ${type2.toHexString()}, extra: ${extra.toHexString()}) [stack=${stack.top} [")

                for (i in 0 until stack.top) {
                    if (i != 0)
                        print(", ")
                    print(stack.stack[i].toStringTyped())
                }
                print("]]")
                println()
            }

            when (opcode) {
                Op.PUSH -> handlePush(reader, type1, extra)
                Op.PUSH_LOCAL -> handlePushLocal(reader, type1, extra)
                Op.PUSH_GLOBAL -> handlePushGlobal(reader, type1)
                Op.PUSH_IMMEDIATE -> handlePushImmediate(reader, extra)
                Op.PUSH_BUILTIN -> handlePushBuiltin(reader, type1, extra)
                Op.POP -> handlePop(reader, type1, extra)
                Op.POPZ -> stack.pop()
                Op.CMP -> {
                    val right = stack.pop()
                    val left = stack.pop()
                    val diff = left.real - right.real
                    val comparisonFunction = word.comparisonFunction()
                    val result = when (comparisonFunction) {
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
                        reader.position = start + word.branchOffset()
                }
                Op.BT -> {
                    val result = stack.pop().bool
                    if (result)
                        reader.position = start + word.branchOffset()
                }
                Op.B -> {
                    reader.position = start + word.branchOffset()
                }
                Op.ADD -> handleAdd(reader, type1, type2)
                Op.SUB -> handleSub(reader, type1, type2)
                Op.MUL -> handleMul(reader, type1, type2)
                Op.CALL -> handleCall(reader, extra)
                Op.DUP -> handleDup(reader, type1, extra)
                Op.BREAK -> handleBreak(reader)
                Op.RET -> return
                Op.EXIT -> return
                else -> error("Found unknown opcode (${opcode.toByte().toHexString()} / ${Op.getOpcodeName(opcode)}) while executing \"${gameWAD.strg.getString(currentCallFrame.code.name)}\"")
            }
        }
    }

    private fun handlePush(reader: StarfaitByteBuffer, type1: Int, extra: Int) {
        val type1DataType = InstructionDataType.byId(type1)

        println(type1DataType)
        when (type1DataType) {
            // While there's already PUSH_IMMEDIATE for INT16, it seems that GameMaker also does use handlePush with INT16 sometimes, in this case the value is the "extra" field
            InstructionDataType.INT16 -> stack.push().setInt32(extra)
            InstructionDataType.INT32 -> stack.push().setInt32(reader.readInt())
            InstructionDataType.INT64 -> stack.push().setInt64(reader.readLong())
            InstructionDataType.DOUBLE -> stack.push().setReal(reader.readDouble())
            InstructionDataType.FLOAT -> TODO()
            InstructionDataType.BOOLEAN -> TODO()
            InstructionDataType.VARIABLE -> {
                val operand = VariableReferenceOperand(reader.readInt())

                val arrayIndex = if (operand.hasArrayIndex()) stack.pop().int else null
                val instanceId = if (operand.hasInstanceIdOnStack()) stack.pop().int else extra
                val varId = operand.variableIndex()

                println("instanceId is $instanceId")

                // TODO: Use correct instance here!
                pushVariableFromObject(
                    when {
                        instanceId == SpecialObjectReferenceType.SELF -> requireNotNull(currentInstance)
                        instanceId == SpecialObjectReferenceType.LOCAL -> {
                            // ewww this is a horrible hack
                            stack.push().copyFrom(currentCallFrame.localVariables[varId]!!)
                            return
                        }
                        instanceId == SpecialObjectReferenceType.GLOBAL -> global
                        instanceId >= 0 -> runner.currentRoom.instances.first { it.objectIndex == instanceId }
                        else -> error("Unsupported instance ID $instanceId!")
                    },
                    operand,
                    arrayIndex
                )
            }
            InstructionDataType.STRING -> stack.push().setString(gameWAD.strg.stringsByIndex[reader.readInt()].also {
                println("Pushed string \"$it\" to the stack")
            })
        }
    }

    private fun handlePushBuiltin(reader: StarfaitByteBuffer, type1: Int, extra: Int) {
        // Pushes a builtin variable to the stack
        // handlePushBuiltin type1 is always "VARIABLE"
        val operand = VariableReferenceOperand(reader.readInt())
        val varId = operand.variableIndex()

        val pushed = stack.push()
        val builtinVariable = builtinVariableHandlers[varId]

        builtinVariable.readHandler.invoke(pushed)
    }

    private fun handlePushLocal(reader: StarfaitByteBuffer, type1: Int, extra: Int) {
        val type1DataType = InstructionDataType.byId(type1)

        println("push local $type1DataType")
        when (type1DataType) {
            InstructionDataType.INT16 -> TODO()
            InstructionDataType.INT32 -> TODO()
            InstructionDataType.INT64 -> TODO()
            InstructionDataType.DOUBLE -> TODO()
            InstructionDataType.FLOAT -> TODO()
            InstructionDataType.BOOLEAN -> TODO()
            InstructionDataType.VARIABLE -> {
                val operand = VariableReferenceOperand(reader.readInt())

                val arrayIndex = if (operand.hasArrayIndex()) stack.pop().int else null
                val instanceId = if (operand.hasInstanceIdOnStack()) stack.pop().int else extra

                println("array index? $arrayIndex")
                println("instanceID? $instanceId")

                val pushed = stack.push()
                val localVariable = currentCallFrame.localVariables[operand.variableIndex()]

                println("Local Variable with VarID ${operand.variableIndex()} is ${localVariable?.toStringTyped()}")

                if (localVariable != null) {
                    pushed.copyFrom(localVariable)
                } else {
                    pushed.setUndefined()
                }
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
                val operand = VariableReferenceOperand(reader.readInt())
                pushVariableFromObject(global, operand, null)
            }
            InstructionDataType.STRING -> stack.push().setString(gameWAD.strg.stringsByIndex[reader.readInt()])
        }
    }

    private fun pushVariableFromObject(objectBase: ObjectBase, operand: VariableReferenceOperand, arrayIndex: Int?) {
        println("The VarID of the operand is ${operand.variableIndex()}")
        val variableId = operand.variableIndex()
        // println("The variable on ${objectBase} is ${variableId?.toStringTyped()} (VarID: ${operand.variableIndex()})")

        val result = stack.push()
        if (variableId >= REGULAR_VARIABLES_BASE) {
            val variable = objectBase.variables[variableId] ?: error("Variable with VarID ${operand.variableIndex()} (${getVariableNameFromVariableIndex(operand.variableIndex())}) is not set before reading it")
            if (arrayIndex != null) {
                require(variable.type == RValueDataType.ARRAY) { "Variable with VarID ${operand.variableIndex()} (${getVariableNameFromVariableIndex(operand.variableIndex())}) is not an array, but we wanted to read arrayIndex $arrayIndex from it!" }
                val array = variable.ref as GMLArray
                val variable = array.entries[arrayIndex] ?: error("Trying to read from array index $arrayIndex, but the element does not exist!")
                result.copyFrom(variable)
            } else {
                result.copyFrom(variable)
            }
        } else {
            val builtinVariableHandler = builtinVariableHandlers[variableId]
            builtinVariableHandler.readHandler.invoke(result)
        }
    }

    private fun handlePop(reader: StarfaitByteBuffer, type1: Int, extra: Int) {
        val type1DataType = InstructionDataType.byId(type1)
        when (type1DataType) {
            InstructionDataType.DOUBLE -> TODO()
            InstructionDataType.FLOAT -> TODO()
            InstructionDataType.INT32 -> TODO()
            InstructionDataType.INT64 -> TODO()
            InstructionDataType.BOOLEAN -> TODO()
            InstructionDataType.VARIABLE -> {
                val operand = VariableReferenceOperand(reader.readInt())

                val varId = operand.variableIndex()
                val arrayIndex = if (operand.hasArrayIndex()) stack.pop().int else null
                val instanceId = if (operand.hasInstanceIdOnStack()) stack.pop().int else extra

                println("array index? $arrayIndex")
                println("instanceID? $instanceId")

                // TODO: Use correct instance here!
                val instance = when {
                    instanceId == SpecialObjectReferenceType.SELF -> requireNotNull(currentInstance)
                    instanceId == SpecialObjectReferenceType.LOCAL -> {
                        // ewww this is a horrible hack
                        val popped = stack.pop()
                        println("Writing ${popped.toStringTyped()} to local variable VarID $varId")
                        currentCallFrame.localVariables[varId] = popped.makeIndependent()
                        return
                    }
                    instanceId == SpecialObjectReferenceType.GLOBAL -> global
                    instanceId >= 0 -> {
                        // This is a reference to all instances with a specific object index
                        // Once again this is a horrible hack, fix it later!
                        for (instance in runner.currentRoom.instances) {
                            if (instance.objectIndex == instanceId) {
                                instance.variables[varId] = stack.pop().makeIndependent().also {
                                    println("Setting variable $varId to ${it.toStringTyped()}")
                                }
                            }
                        }
                        return
                    }
                    else -> error("Unsupported instance ID $instanceId!")
                }

                if (varId >= REGULAR_VARIABLES_BASE) {
                    if (arrayIndex != null) {
                        // Arrays a bit trickier...
                        // In this case, we want to WRITE to a specific slot in the array
                        val arrayRValue = instance.variables[varId] ?: RValue().apply { this.setUndefined() }
                        val gmlArray = if (arrayRValue.type == RValueDataType.ARRAY) {
                            arrayRValue.ref as GMLArray
                        } else {
                            GMLArray()
                        }

                        gmlArray.entries[arrayIndex] = stack.pop().makeIndependent()

                        arrayRValue.setArray(gmlArray)

                        println("GMLArray elements: ${gmlArray.entries}")

                        // We don't need to make it independent because or it is a RValue that we already own, or it was the already existing RValue
                        instance.variables[varId] = arrayRValue
                    } else {
                        instance.variables[varId] = stack.pop().makeIndependent()
                    }
                } else {
                    // Builtin variable!
                    val builtinVariableHandler = builtinVariableHandlers[varId]
                    builtinVariableHandler.writeHandler.invoke(stack.pop().makeIndependent())
                }
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

        println("Top of the stack is ${topOfTheStack.toStringTyped()}")

        // no-op, this is only useful if some day we decide to go with tagged variables (that is, using two arrays, one for native values and another for RValues)
        // Because in that case, we would need to convert the RValue to the native type
        if (destinationType == InstructionDataType.VARIABLE)
            return

        if (destinationType == InstructionDataType.INT32) {
            topOfTheStack.setInt32(topOfTheStack.int)
            return
        }

        if (destinationType == InstructionDataType.BOOLEAN) {
            topOfTheStack.setBoolean(topOfTheStack.bool)
            return
        }

        TODO()
    }

    private fun handlePushImmediate(reader: StarfaitByteBuffer, extra: Int) {
        stack.push().setInt32(extra)
    }

    private fun handleCall(reader: StarfaitByteBuffer, extra: Int) {
        val operand = FunctionReferenceOperand(reader.readInt())
        val functionIndex = operand.functionIndex()

        // extra = parameter count
        val arguments = ArrayList<RValue>(extra)
        repeat(extra) {
            // We make it independent because we push the stack right down below, and that can cause these variables to change
            arguments.add(stack.pop().makeIndependent())
        }

        val functionName = gameWAD.strg.getString(gameWAD.func.functions[functionIndex].name)
        val functionHandler = builtins.registeredFunctions[functionName]

        if (functionHandler != null) {
            val result = stack.push()
            functionHandler.invoke(arguments, result)
            return
        }

        // This may be a user script!
        val script = gameWAD.scpt.scripts.firstOrNull { gameWAD.strg.getString(it.name) == functionName }
        if (script != null) {
            // The script result (if it is a script) will be on the top of the stack
            executeCode(gameWAD.code.codes[script.codeIndex], arguments.toTypedArray())
            return
        }

        println("[${gameWAD.strg.getString(currentCallFrame.code.name)}] Unknown function! ${gameWAD.strg.getString(gameWAD.func.functions[functionIndex].name)}, parameters: ${arguments.joinToString { it.toStringTyped() }}")

        // As a fallback, we'll push undefined to the stack if the function does not exist
        val result = stack.push()
        result.setUndefined()
    }

    private fun handleAdd(reader: StarfaitByteBuffer, type1: Int, type2: Int) {
        val type1DataType = InstructionDataType.byId(type1)
        val type2DataType = InstructionDataType.byId(type2)

        println("ADD type1 is $type1DataType, type2 is $type2DataType")

        // The YoYo Runner uses the type1/type2 data types to know how many bytes to read from the stack
        // Because we use tagged RValues, we don't need them for this
        val right = stack.pop()
        val left = stack.pop()

        val pushed = stack.push()

        println("ADD left type is ${left.type}, right type is ${right.type}")
        if (left.type == RValueDataType.STRING || right.type == RValueDataType.STRING) {
            if (left.type != RValueDataType.STRING || right.type != RValueDataType.STRING)
                error("DoAdd :: Execution Error")

            val leftRef = left.ref
            val rightRef = right.ref

            // If the ref is not a string, then something went deeply wrong
            require(leftRef is String)
            require(rightRef is String)

            pushed.setString(leftRef + rightRef)
            return
        }

        pushed.setReal(left.real + right.real)
    }

    private fun handleSub(reader: StarfaitByteBuffer, type1: Int, type2: Int) {
        val type1DataType = InstructionDataType.byId(type1)
        val type2DataType = InstructionDataType.byId(type2)

        // The YoYo Runner uses the type1/type2 data types to know how many bytes to read from the stack
        // Because we use tagged RValues, we don't need them for this
        val right = stack.pop()
        val left = stack.pop()

        val pushed = stack.push()

        pushed.setReal(left.real - right.real)
    }

    private fun handleMul(reader: StarfaitByteBuffer, type1: Int, type2: Int) {
        val type1DataType = InstructionDataType.byId(type1)
        val type2DataType = InstructionDataType.byId(type2)

        // The YoYo Runner uses the type1/type2 data types to know how many bytes to read from the stack
        // Because we use tagged RValues, we don't need them for this
        val right = stack.pop()
        val left = stack.pop()

        val pushed = stack.push()

        pushed.setReal(left.real * right.real)
    }

    private fun handleDup(reader: StarfaitByteBuffer, type1: Int, extra: Int) {
        // In the YoYo Runner, the type1 would've been used to figure out how many bytes to be copied from the stack
        // Because all of our types are tagged RValues, we don't need to rely on it (yay)
        // extra = how many additional elements will be copied from the stack, that is...
        // If the stack is [a, b], and extra is 1, the result will be [a, b, a, b]
        require(extra >= 0) { "Negative extra value on the dup!" }
        val total = extra + 1
        val dupBottom = stack.top - 1 - extra
        repeat(total) {
            val target = stack.peekAt(dupBottom + it)
            stack.push().copyFrom(target)
        }
    }

    private fun handleBreak(reader: StarfaitByteBuffer) {
        // For *real* games, the BREAK opcode is only used to check the array size to avoid querying out of bounds
        println("BREAK opcode called, it isn't implemented yet")
    }

    /**
     * By default, each variable and function reference in the code references a delta offset on the `extra` field to the next reference of the variable.
     *
     * The remapper walks through all VARI variables and remaps the delta offsets to be VARI indices.
     */
    fun remapReferences() {
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

        // Register the builtin variable handlers
        builtinVariableHandlers = Array(allocatedBuiltinVariables.size) { index ->
            val name = gameWAD.strg.getString(allocatedBuiltinVariables[index].name)

            // TODO: In the future this would be nicer if we could toggle a "strict mode" (throws exception) and a lenient mode (only logs)
            builtins.registeredVariables[name] ?: BuiltinVariable(
                { error("Tried reading from unimplemented builtin variable \"$name\"!") },
                { error("Tried writing to unimplemented builtin variable \"$name\"!") },
            )
        }
    }

    private fun getVariableNameFromVariableIndex(index: Int): String {
        require(index >= REGULAR_VARIABLES_BASE) { "Trying to get the name of a builtin variable!" }
        val variable = gameWAD.vari.variables.getOrNull(index - REGULAR_VARIABLES_BASE)?.name ?: return "<UnknownVariable>"
        return gameWAD.strg.getString(variable)
    }
}