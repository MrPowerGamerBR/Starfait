package com.mrpowergamerbr.starfait.vm

import com.mrpowergamerbr.starfait.MathUtils
import com.mrpowergamerbr.starfait.StarfaitByteBuffer
import com.mrpowergamerbr.starfait.runner.Instance
import com.mrpowergamerbr.starfait.runner.ObjectBase
import com.mrpowergamerbr.starfait.runner.StarfaitRunner
import com.mrpowergamerbr.starfait.wad.Code
import com.mrpowergamerbr.starfait.wad.GameObject
import com.mrpowergamerbr.starfait.wad.GameWAD
import com.mrpowergamerbr.starfait.wad.Variable
import java.nio.ByteBuffer

class StarfaitVM(
    val gameWAD: GameWAD,
    val gmlScriptsOpcodesToBeTraced: Set<String>,
    val functionCallsToBeTraced: Set<String>,
    val alarmsToBeTraced: Set<String>,
    val eventsToBeTraced: Set<String>
) {
    companion object {
        private const val REGULAR_VARIABLES_BASE = 100_000
    }

    lateinit var runner: StarfaitRunner
    val stack = VMStack()
    val global = ObjectBase(this)
    val builtins = VMBuiltins(this)
    lateinit var builtinVariableHandlers: Array<BuiltinVariable>
    lateinit var regularVariableNames: Array<String>

    // "codeName#functionName"
    val loggedUnknownFunctions = mutableSetOf<String>()
    val loggedStubbedFunctions = mutableSetOf<String>()
    val loggedStubbedVariables = mutableSetOf<String>()

    val callFrameStack = ArrayDeque<CallFrame>()

    private var currentOrNullCallFrame: CallFrame? = null

    val currentCallFrame: CallFrame
        get() = currentOrNullCallFrame ?: error("No current call frame!")
    val currentCallFrameCodeName: String
        get() = gameWAD.strg.getString(currentCallFrame.code.name)
    val currentSelfInstance: Instance
        get() = currentOrNullCallFrame?.self ?: error("No current call frame, so we can't get the current instance!")

    val dsMapPool = mutableListOf<DSMap>()

    // The YoYo Runner also stores this globally, nasty, but it works
    var dragAndDropRelativeValues = false

    fun executeCode(
        code: Code,
        currentInstance: Instance,
        otherInstance: Instance?,
        eventObject: GameObject,
        eventType: Int,
        eventSubtype: Int,
        arguments: Array<RValue>
    ): RValue? {
        val callFrame = CallFrame(this, code, currentInstance, otherInstance, eventObject, eventType, eventSubtype, arguments, stack.top)
        this.callFrameStack.addLast(callFrame)
        this.currentOrNullCallFrame = callFrame

        // We need to remap the bytecode relative offset field position
        val bytecodeInstructions = ByteBuffer.wrap(
            gameWAD.code.bytecode,
            // We do + on the offset because the offset is negative
            code.offset + (code.bytecodeRelativeOffsetFieldPosition + code.bytecodeRelativeOffset) - gameWAD.code.postAddressPosition,
            code.length.toInt() - code.offset
        )

        try {
            executeBytecodeInstructions(bytecodeInstructions)
        } catch (e: Throwable) {
            throw RuntimeException("Something went wrong while executing code $currentCallFrameCodeName!", e)
        }

        check(callFrame.stackTopWhenTheCallFrameWasCreated == stack.top) { "Unbalanced stack! Stack top is ${stack.top} when it should've been ${callFrame.stackTopWhenTheCallFrameWasCreated}! CallFrameStack is ${callFrameStack.joinToString(" -> ") { gameWAD.strg.getString(it.code.name) }}"}

        this.callFrameStack.removeLast()
        this.currentOrNullCallFrame = this.callFrameStack.lastOrNull()

        return callFrame.returnValue
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

            val scriptName = this.currentCallFrameCodeName
            if (gmlScriptsOpcodesToBeTraced.contains("*") || gmlScriptsOpcodesToBeTraced.contains(scriptName)) {
                print("VM: [$scriptName] ($start) [0x${word.value.toHexString()}] ${Op.getOpcodeName(opcode)} (type1: ${type1.toHexString()}, type2: ${type2.toHexString()}, extra: ${extra.toHexString()}) [stack=${stack.top} [")

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

                    val comparisonFunction = word.comparisonFunction()

                    if (left.type == RValueDataType.STRING || right.type == RValueDataType.STRING) {
                        val isEqual = left.ref == right.ref
                        stack.push().setBoolean(if (comparisonFunction == CmpOp.EQUAL) isEqual else !isEqual)
                        continue
                    }

                    val diff = left.real - right.real
                    val result = when (comparisonFunction) {
                        CmpOp.LESS_THAN -> diff < -MathUtils.EPSILON
                        CmpOp.LESS_THAN_OR_EQUAL -> diff <= MathUtils.EPSILON
                        CmpOp.EQUAL -> if (diff < -MathUtils.EPSILON) false else diff <= MathUtils.EPSILON
                        CmpOp.NOT_EQUAL -> if (diff < -MathUtils.EPSILON) true else MathUtils.EPSILON < diff
                        CmpOp.GREATER_THAN_OR_EQUAL -> -MathUtils.EPSILON <= diff
                        CmpOp.GREATER_THAN -> MathUtils.EPSILON < diff
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
                Op.PUSH_ENV -> handlePushEnv(reader, reader.position, word.branchOffset())
                Op.POP_ENV -> handlePopEnv(reader, word)
                Op.ADD -> handleAdd(reader, type1, type2)
                Op.SUB -> handleSub(reader, type1, type2)
                Op.MUL -> handleMul(reader, type1, type2)
                Op.DIV -> handleDiv(reader, type1, type2)
                Op.NOT -> handleNot(reader, type1)
                Op.NEG -> handleNeg(reader, type1)
                Op.MOD -> handleMod(reader, type1, type2)
                Op.CALL -> handleCall(reader, extra)
                Op.DUP -> handleDup(reader, type1, extra)
                Op.BREAK -> handleBreak(reader)
                Op.RET -> {
                    currentCallFrame.returnValue = stack.pop().makeIndependent()
                    return
                }
                Op.EXIT -> return
                else -> error("Found unknown opcode (${opcode.toByte().toHexString()} / ${Op.getOpcodeName(opcode)}) while executing \"$currentCallFrameCodeName\"")
            }
        }
    }

    private fun handlePush(reader: StarfaitByteBuffer, type1: Int, extra: Int) {
        val type1DataType = InstructionDataType.byId(type1)

        // println(type1DataType)
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

                // println("arrayIndex is $arrayIndex")
                // println("instanceId is $instanceId")

                // TODO: Use correct instance here!
                pushVariableFromVariableContainer(
                    when {
                        instanceId == VariableScope.SELF -> requireNotNull(currentSelfInstance)
                        instanceId == VariableScope.LOCAL -> this.currentCallFrame
                        instanceId == VariableScope.OTHER -> this.currentCallFrame.other!!
                        instanceId == VariableScope.GLOBAL -> this.global
                        instanceId >= 0 -> {
                            val matchedInstance = this.runner.firstInstance {
                                it.matchesObjectOrInstanceId(
                                    instanceId,
                                    null,
                                    skipDestroyed = true,
                                    checkHierarchy = true,
                                    checkSpecialSelf = true,
                                    checkSpecialOther = true,
                                    checkSpecialAll = true,
                                    checkSpecialNoone = true
                                )
                            }

                            if (matchedInstance == null) {
                                stack.push().setUndefined()
                                return
                            }

                            matchedInstance
                        }
                        else -> error("Unsupported instance ID $instanceId!")
                    },
                    operand,
                    arrayIndex
                )
            }
            InstructionDataType.STRING -> stack.push().setString(gameWAD.strg.stringsByIndex[reader.readInt()].also {
                // println("Pushed string \"$it\" to the stack")
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

        builtinVariable.readHandler.invoke(null, pushed, null)
    }

    private fun handlePushLocal(reader: StarfaitByteBuffer, type1: Int, extra: Int) {
        val type1DataType = InstructionDataType.byId(type1)

        // println("push local $type1DataType")
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

                // println("array index? $arrayIndex")
                // println("instanceID? $instanceId")

                val pushed = stack.push()
                val localVariable = this.currentCallFrame.getVariable(operand.variableIndex(), arrayIndex)
                pushed.copyFrom(localVariable)
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
                pushVariableFromVariableContainer(global, operand, null)
            }
            InstructionDataType.STRING -> stack.push().setString(gameWAD.strg.stringsByIndex[reader.readInt()])
        }
    }

    private fun writeVariableToInstanceId(varId: Int, arrayIndex: Int?, instanceId: Int, poppedValue: RValue) {
        // TECHNICALLY I'm pretty sure that not all push paths write to builtin vs regular variable
        // But to make everything consistent, we'll use the same path for everything
        val variableContainers = when {
            instanceId == VariableScope.SELF -> listOf(this.currentSelfInstance)
            instanceId == VariableScope.OTHER -> listOf(requireNotNull(this.currentCallFrame.other))
            instanceId == VariableScope.LOCAL -> listOf(this.currentCallFrame)
            instanceId == VariableScope.GLOBAL -> listOf(this.global)
            instanceId >= 0 -> runner.filterInstances {
                it.matchesObjectOrInstanceId(
                    instanceId,
                    null,
                    skipDestroyed = true,
                    checkHierarchy = true,
                    checkSpecialAll = true,
                    checkSpecialNoone = true,
                    checkSpecialSelf = true,
                    checkSpecialOther = true
                )
            }
            else -> error("Unsupported instance ID $instanceId!")
        }

        for (variableContainer in variableContainers) {
            // We need to create a new independent value for each container!
            // Because if we don't, because RValues are mutable, a different caller may change a RValue data, changing it on EVERY VariableContainer
            val independentValue = poppedValue.makeIndependent()

            if (varId >= REGULAR_VARIABLES_BASE) {
                variableContainer.setVariable(varId, independentValue, arrayIndex)
            } else {
                // Builtin variable!
                check(variableContainer is Instance) { "Cannot write a built-in variable to a non-instance!" }

                val builtinVariableHandler = builtinVariableHandlers[varId]
                builtinVariableHandler.writeHandler.invoke(variableContainer, independentValue, arrayIndex)
            }
        }
    }

    private fun pushVariableFromVariableContainer(variableContainer: VariableContainer, operand: VariableReferenceOperand, arrayIndex: Int?) {
        // println("The VarID of the operand is ${operand.variableIndex()}")
        val variableId = operand.variableIndex()
        // println("The variable on ${objectBase} is ${variableId?.toStringTyped()} (VarID: ${operand.variableIndex()})")

        val result = stack.push()
        if (variableId >= REGULAR_VARIABLES_BASE) {
            result.copyFrom(variableContainer.getVariable(variableId, arrayIndex))
        } else {
            check(variableContainer is Instance) { "Cannot read a built-in variable from a non-instance!" }

            val builtinVariableHandler = this.builtinVariableHandlers[variableId]
            builtinVariableHandler.readHandler.invoke(variableContainer, result, arrayIndex)
        }
    }

    private fun handlePop(reader: StarfaitByteBuffer, type1: Int, extra: Int) {
        val type1DataType = InstructionDataType.byId(type1)
        when (type1DataType) {
            InstructionDataType.DOUBLE -> TODO()
            InstructionDataType.FLOAT -> TODO()
            InstructionDataType.INT32 -> {
                // Seems to be used for global array writes, for example: "global.flag[70] += 1;"
                // It is similar to the VARIABLE path, with the change that the value comes BEFORE, not AFTER
                // TODO: It also seems to be used for any kind of array write? Test later
                val operand = VariableReferenceOperand(reader.readInt())

                val varId = operand.variableIndex()
                val poppedValue = stack.pop().makeIndependent()
                val arrayIndex = if (operand.hasArrayIndex()) stack.pop().int else null
                val instanceId = if (operand.hasInstanceIdOnStack()) stack.pop().int else extra

                writeVariableToInstanceId(
                    varId,
                    arrayIndex,
                    instanceId,
                    poppedValue
                )
            }
            InstructionDataType.INT64 -> TODO()
            InstructionDataType.BOOLEAN -> TODO()
            InstructionDataType.VARIABLE -> {
                val operand = VariableReferenceOperand(reader.readInt())

                val varId = operand.variableIndex()
                val arrayIndex = if (operand.hasArrayIndex()) stack.pop().int else null
                val instanceId = if (operand.hasInstanceIdOnStack()) stack.pop().int else extra

                val poppedValue = stack.pop()
                writeVariableToInstanceId(
                    varId,
                    arrayIndex,
                    instanceId,
                    poppedValue
                )
            }
            InstructionDataType.STRING -> TODO()
            InstructionDataType.INT16 -> TODO()
        }
    }

    private fun handleConv(reader: StarfaitByteBuffer, type1: Int, type2: Int) {
        // type1 is the source type, type2 is the destination type
        val sourceType = InstructionDataType.byId(type1)
        val destinationType = InstructionDataType.byId(type2)
        // println("Converting from $sourceType to $destinationType")
        val topOfTheStack = stack.peek()

        // println("Top of the stack is ${topOfTheStack.toStringTyped()}")

        when (destinationType) {
            // no-op, this is only useful if some day we decide to go with non-tagged RValues (that is, using two arrays, one for native values and another for RValues)
            // Because in that case, we would need to convert the RValue to the native type
            InstructionDataType.VARIABLE -> return
            InstructionDataType.DOUBLE -> topOfTheStack.setReal(topOfTheStack.real)
            InstructionDataType.FLOAT -> TODO()
            InstructionDataType.INT32 -> topOfTheStack.setInt32(topOfTheStack.int)
            InstructionDataType.INT64 -> TODO()
            InstructionDataType.BOOLEAN -> topOfTheStack.setBoolean(topOfTheStack.bool)
            InstructionDataType.STRING -> TODO()
            InstructionDataType.INT16 -> TODO()
        }
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
        if (functionCallsToBeTraced.contains(functionName) || functionCallsToBeTraced.contains(currentCallFrameCodeName) || functionCallsToBeTraced.contains("*")) {
            println("VM: [$currentCallFrameCodeName] Calling function \"$functionName(${arguments.joinToString(", ")})\"")
        }

        val builtinFunction = builtins.registeredFunctions[functionName]

        if (builtinFunction != null) {
            // Technically the YoYo Runner does NOT validate the parameter count during runtime, only during compilation
            // But we validate it because we are just built like frfr (and because it helps debugging any call issues)
            if (builtinFunction.parameterCount != null && builtinFunction.parameterCount != arguments.size)
                error("Wrong number of arguments to built-in function ${functionName}! Expected ${builtinFunction.parameterCount}, got ${arguments.size} instead, with the following arguments: $arguments")

            val result = stack.push()
            builtinFunction.handler.invoke(arguments, result)
            if (functionCallsToBeTraced.contains(functionName) || functionCallsToBeTraced.contains(currentCallFrameCodeName) || functionCallsToBeTraced.contains("*")) {
                println("VM: [$currentCallFrameCodeName] Called built-in function \"$functionName(${arguments.joinToString(", ")})\"! Result: $result")
            }
            return
        }

        // This may be a user script!
        // TODO: This could be a O(1) by providing the script function index directly
        val script = gameWAD.scpt.scripts.firstOrNull { gameWAD.strg.getString(it.name) == functionName }
        if (script != null) {
            // The script result (if it is a script) will be on the top of the stack
            val callFrame = this.currentCallFrame

            val returnValue = executeCode(
                gameWAD.code.codes[script.codeIndex],
                callFrame.self,
                callFrame.other,
                callFrame.eventObject,
                callFrame.eventType,
                callFrame.eventSubtype,
                arguments.toTypedArray()
            )

            if (functionCallsToBeTraced.contains(functionName) || functionCallsToBeTraced.contains(currentCallFrameCodeName) || functionCallsToBeTraced.contains("*")) {
                println("VM: [$currentCallFrameCodeName] Called script function \"$functionName(${arguments.joinToString(", ")})\"! Result: ${returnValue ?: "undefined"}")
            }

            if (returnValue == null) {
                // This happens when...
                // * Op.EXIT is called
                // * The script exits without any "return" or "exit" statement
                stack.push().setUndefined()
                return
            }

            stack.push().copyFrom(returnValue)
            return
        }

        val dedupKey = "$currentCallFrameCodeName#$functionName"
        if (dedupKey !in loggedUnknownFunctions) {
            println("VM: [$currentCallFrameCodeName] Unknown function! \"$functionName(${arguments.joinToString { it.toStringTyped() }})\"")
            loggedUnknownFunctions.add(dedupKey)
        }

        // As a fallback, we'll push undefined to the stack if the function does not exist
        val result = stack.push()
        result.setUndefined()
    }

    private fun handleAdd(reader: StarfaitByteBuffer, type1: Int, type2: Int) {
        val type1DataType = InstructionDataType.byId(type1)
        val type2DataType = InstructionDataType.byId(type2)

        // println("ADD type1 is $type1DataType, type2 is $type2DataType")

        // The YoYo Runner uses the type1/type2 data types to know how many bytes to read from the stack
        // Because we use tagged RValues, we don't need them for this
        val right = stack.pop()
        val left = stack.pop()

        val pushed = stack.push()

        // println("ADD left type is ${left.type}, right type is ${right.type}")
        if (left.type == RValueDataType.STRING || right.type == RValueDataType.STRING) {
            if (left.type != RValueDataType.STRING || right.type != RValueDataType.STRING)
                error("DoAdd :: Execution Error")

            val leftRef = left.ref
            val rightRef = right.ref

            // If the ref is not a string, then something went deeply wrong
            check(leftRef is String)
            check(rightRef is String)

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

    private fun handleDiv(reader: StarfaitByteBuffer, type1: Int, type2: Int) {
        val right = stack.pop()
        val left = stack.pop()

        val pushed = stack.push()

        pushed.setReal(left.real / right.real)
    }

    private fun handleMod(reader: StarfaitByteBuffer, type1: Int, type2: Int) {
        val right = stack.pop()
        val left = stack.pop()

        val pushed = stack.push()

        pushed.setReal(left.real % right.real)
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
        // println("BREAK opcode called, it isn't implemented yet")
    }

    private fun handleNot(reader: StarfaitByteBuffer, type1: Int) {
        val targetType = InstructionDataType.byId(type1)
        val poppedValue = stack.pop().makeIndependent() // Create a copy so that the push below doesn't overwrite it
        val pushedValue = stack.push()

        when (targetType) {
            InstructionDataType.DOUBLE -> TODO()
            InstructionDataType.FLOAT -> TODO()
            InstructionDataType.INT32 -> pushedValue.setInt32(poppedValue.int.inv()) // Same thing as ~Number
            InstructionDataType.INT64 -> TODO()
            InstructionDataType.BOOLEAN -> pushedValue.setBoolean(!poppedValue.bool)
            InstructionDataType.VARIABLE -> TODO()
            InstructionDataType.STRING -> TODO()
            InstructionDataType.INT16 -> TODO()
        }
    }

    private fun handleNeg(reader: StarfaitByteBuffer, type1: Int) {
        val targetType = InstructionDataType.byId(type1)
        val poppedValue = stack.pop().makeIndependent() // Create a copy so that the push below doesn't overwrite it
        val pushedValue = stack.push()

        when (targetType) {
            InstructionDataType.VARIABLE -> {
                when (poppedValue.type) {
                    RValueDataType.INT32 -> pushedValue.setInt32(-poppedValue.int)
                    RValueDataType.REAL -> pushedValue.setReal(-poppedValue.real)
                    else -> error("I don't know how to handle a ${poppedValue.type}!")
                }
            }
            InstructionDataType.DOUBLE -> pushedValue.setReal(-poppedValue.real)
            InstructionDataType.FLOAT -> TODO()
            InstructionDataType.INT32 -> pushedValue.setInt32(-poppedValue.int)
            InstructionDataType.INT64 -> pushedValue.setInt64(-poppedValue.bits)
            InstructionDataType.BOOLEAN -> TODO()
            InstructionDataType.STRING -> TODO()
            InstructionDataType.INT16 -> pushedValue.setInt32(-poppedValue.int)
        }
    }

    private fun handlePushEnv(reader: StarfaitByteBuffer, ip: Int, branchOffset: Int) {
        val objectId = stack.pop().int

        val instances = runner.filterInstances {
            it.matchesObjectOrInstanceId(
                objectId,
                null,
                skipDestroyed = true,
                checkHierarchy = true,
                checkSpecialAll = true,
                checkSpecialNoone = true,
                checkSpecialSelf = true,
                checkSpecialOther = true
            )
        }

        if (instances.isEmpty()) {
            // The branchOffset jumps to the PopEnv IN RELATION to the beginning of the PushEnv
            // But for this case, we can just jump directly AFTER the PushEnv (we are already +4 because we read the word)
            // So we don't need to care about handling a "trying to pop a frame that doesn't exist"
            reader.position += branchOffset
            return
        }

        val callFrame = this.currentCallFrame

        val envFrame = EnvironmentFrame(
            callFrame.self,
            instances,
            ip
        )

        callFrame.environmentFrameStack.add(envFrame)
        updateCallFrameInstancesToMatchEnvFrame(currentCallFrame, envFrame)
    }

    private fun handlePopEnv(reader: StarfaitByteBuffer, word: OpWord) {
        val callFrame = this.currentCallFrame

        val pushEnvFrame = callFrame.environmentFrameStack.last()
        pushEnvFrame.cursor++

        // The 0xBBF00000 word is used when an "exit" is called within a "with" statement
        if (pushEnvFrame.cursor >= pushEnvFrame.instances.size || word.value == 0xBBF00000.toInt()) {
            callFrame.environmentFrameStack.removeLastOrNull()
            val activeEnvFrame = callFrame.environmentFrameStack.lastOrNull()
            if (activeEnvFrame != null) {
                // If we have an active environment frame...
                updateCallFrameInstancesToMatchEnvFrame(callFrame, activeEnvFrame)
            } else {
                callFrame.self = callFrame.initialSelf
                callFrame.other = callFrame.initialOther
            }
            return
        }

        reader.position = pushEnvFrame.startIp

        updateCallFrameInstancesToMatchEnvFrame(callFrame, pushEnvFrame)
    }

    private fun updateCallFrameInstancesToMatchEnvFrame(callFrame: CallFrame, environmentFrame: EnvironmentFrame) {
        callFrame.other = environmentFrame.savedSelf
        callFrame.self = environmentFrame.instances[environmentFrame.cursor]
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
                { _, _, _ -> error("Tried reading from unimplemented builtin variable \"$name\"!") },
                { _, _, _ -> error("Tried writing to unimplemented builtin variable \"$name\"!") },
            )
        }

        // Register the regular variable names
        regularVariableNames = Array(allocatedRegularVariables.size) { index -> gameWAD.strg.getString(allocatedRegularVariables[index].name) }
    }

    fun getVariableIndexFromVariableName(name: String): Int {
        val variable = gameWAD.vari.variables.firstOrNull { gameWAD.strg.getString(it.name) == name } ?: return -1
        return variable.varId
    }

    fun getVariableNameFromVariableIndex(index: Int): String {
        require(index >= REGULAR_VARIABLES_BASE) { "Trying to get the name of a builtin variable!" }
        return regularVariableNames.getOrNull(index - REGULAR_VARIABLES_BASE) ?: return "<UnknownVariable>"
    }
}