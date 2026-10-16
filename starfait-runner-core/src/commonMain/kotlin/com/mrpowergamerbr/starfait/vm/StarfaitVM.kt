package com.mrpowergamerbr.starfait.vm

import com.mrpowergamerbr.starfait.MathUtils
import com.mrpowergamerbr.starfait.StarfaitByteBuffer
import com.mrpowergamerbr.starfait.StarfaitUtils
import com.mrpowergamerbr.starfait.disasm.StarfaitDisassembler
import com.mrpowergamerbr.starfait.putIfAbsent
import com.mrpowergamerbr.starfait.runner.GlobalObject
import com.mrpowergamerbr.starfait.runner.Instance
import com.mrpowergamerbr.starfait.runner.StarfaitRunner
import com.mrpowergamerbr.starfait.wad.Code
import com.mrpowergamerbr.starfait.wad.GameObject
import com.mrpowergamerbr.starfait.wad.GameWAD
import kotlin.collections.mutableMapOf

class StarfaitVM(
    val gameWAD: GameWAD,
    val gmlScriptsOpcodesToBeTraced: Set<String>,
    val functionCallsToBeTraced: Set<String>,
    val alarmsToBeTraced: Set<String>,
    val eventsToBeTraced: Set<String>,
    val traceVariableReads: Set<String>,
    val traceVariableWrites: Set<String>,
    val traceCodeExecution: Set<String>
) {
    lateinit var runner: StarfaitRunner
    val stack = VMStack()
    val global = GlobalObject(this)
    val builtins = VMBuiltins(this)
    lateinit var builtinVariableHandlers: Array<BuiltinVariable>
    lateinit var regularVariableNames: Array<String>
    lateinit var variableNameToId: Map<Int, Map<String, Int>>

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
    val currentOrNullCallFrameCodeName: String?
        get() = currentOrNullCallFrame?.code?.name?.let { gameWAD.strg.getString(it) }
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
        if (traceCodeExecution.contains("*") || traceCodeExecution.contains(gameWAD.strg.getString(code.name)))
            println("VM: [$currentOrNullCallFrameCodeName] Executing code ${gameWAD.strg.getString(code.name)}")

        val callFrame = CallFrame(this, code, currentInstance, otherInstance, eventObject, eventType, eventSubtype, arguments, stack.top)
        this.callFrameStack.addLast(callFrame)
        this.currentOrNullCallFrame = callFrame

        // We need to remap the bytecode relative offset field position
        val start = code.offset + (code.bytecodeRelativeOffsetFieldPosition + code.bytecodeRelativeOffset) - gameWAD.code.postAddressPosition
        val bytecodeInstructions = StarfaitByteBuffer.fromByteArray(gameWAD.code.bytecode, start, code.length.toInt())

        try {
            executeBytecodeInstructions(bytecodeInstructions)
        } catch (e: Throwable) {
            throw RuntimeException("Something went wrong while executing code $currentCallFrameCodeName!", e)
        }

        // The "repeat" statement in GameMaker is busted
        // If you call "exit" or "return" within a "repeat" statement, the compiler won't emit POPZ calls to pop the integer used for the repeat statement,
        // causing the stack to "leak" values!
        //
        // To workaround this, we'll just pop them ourselves
        check(stack.top >= callFrame.stackTopWhenTheCallFrameWasCreated) { "Unbalanced stack! Stack top is ${stack.top} when it should've been equals to or more than ${callFrame.stackTopWhenTheCallFrameWasCreated}! CallFrameStack is ${callFrameStack.joinToString(" -> ") { gameWAD.strg.getString(it.code.name) }}"}
        repeat(stack.top - callFrame.stackTopWhenTheCallFrameWasCreated) {
            stack.popAndThrowAway()
        }

        this.callFrameStack.removeLast()
        this.currentOrNullCallFrame = this.callFrameStack.lastOrNull()

        return callFrame.returnValue
    }

    private fun executeBytecodeInstructions(reader: StarfaitByteBuffer) {
        while (reader.hasRemaining()) {
            val start = reader.position
            val word = OpWord(reader.readInt())

            val opcode = word.opcode()
            val type1 = word.type1()
            val type2 = word.type2()
            val extra = word.extra()

            val scriptName = this.currentCallFrameCodeName
            if (gmlScriptsOpcodesToBeTraced.contains("*") || gmlScriptsOpcodesToBeTraced.contains(scriptName) || gmlScriptsOpcodesToBeTraced.contains(currentCallFrame.self.objectName)) {
                val beforeDisassembling = reader.position
                reader.position = start
                print("VM: [$currentCallFrameCodeName] ${StarfaitDisassembler.readAndDisassembleOp(gameWAD, reader, stack)}")
                reader.position = beforeDisassembling
            }

            when (opcode) {
                Op.PUSH -> handlePush(reader, type1, extra)
                Op.PUSH_LOCAL -> handlePushLocal(reader, type1, extra)
                Op.PUSH_GLOBAL -> handlePushGlobal(reader, type1)
                Op.PUSH_IMMEDIATE -> handlePushImmediate(reader, extra)
                Op.PUSH_BUILTIN -> handlePushBuiltin(reader, type1, extra)
                Op.POP -> handlePop(reader, type1, type2, extra)
                Op.POPZ -> when (InstructionDataType.byId(type1)) {
                    InstructionDataType.DOUBLE -> TODO()
                    InstructionDataType.FLOAT -> TODO()
                    InstructionDataType.INT32 -> stack.popInt32()
                    InstructionDataType.INT64 -> TODO()
                    InstructionDataType.BOOLEAN -> TODO()
                    InstructionDataType.VARIABLE -> stack.popVariable()
                    InstructionDataType.STRING -> TODO()
                    InstructionDataType.INT16 -> TODO()
                }
                Op.CMP -> {
                    val type1DataType = InstructionDataType.byId(type1)
                    val type2DataType = InstructionDataType.byId(type2)

                    val right = stack.popByInstructionType(type1DataType)
                    val left = stack.popByInstructionType(type2DataType)
                    val rightType = right.cmpType()
                    val leftType = left.cmpType()

                    val comparisonFunction = word.comparisonFunction()

                    if (leftType == CmpType.STRING || rightType == CmpType.STRING) {
                        val isEqual = left.convertToGMLString() == right.convertToGMLString()
                        val result = if (comparisonFunction == CmpOp.EQUAL) isEqual else !isEqual
                        stack.pushBoolean(result)
                        continue
                    }

                    val diff = left.convertToReal() - right.convertToReal()
                    val result = when (comparisonFunction) {
                        CmpOp.LESS_THAN -> diff < -MathUtils.EPSILON
                        CmpOp.LESS_THAN_OR_EQUAL -> diff <= MathUtils.EPSILON
                        CmpOp.EQUAL -> if (diff < -MathUtils.EPSILON) false else diff <= MathUtils.EPSILON
                        CmpOp.NOT_EQUAL -> if (diff < -MathUtils.EPSILON) true else MathUtils.EPSILON < diff
                        CmpOp.GREATER_THAN_OR_EQUAL -> -MathUtils.EPSILON <= diff
                        CmpOp.GREATER_THAN -> MathUtils.EPSILON < diff
                        else -> error("Unsupported comparsion function $comparisonFunction")
                    }

                    stack.pushBoolean(result)
                }
                Op.CONV -> handleConv(reader, type1, type2)
                // Branches ALWAYS except a 4-byte value
                Op.BF -> {
                    val result = stack.popBooleanLikeAndUnwrap()
                    if (!result)
                        reader.position = start + word.branchOffset()
                }
                Op.BT -> {
                    val result = stack.popBooleanLikeAndUnwrap()
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
                    currentCallFrame.returnValue = stack.popVariable().value
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
            InstructionDataType.INT16 -> stack.pushInt32(extra)
            InstructionDataType.INT32 -> stack.pushInt32(reader.readInt())
            InstructionDataType.INT64 -> stack.pushInt64(reader.readLong())
            InstructionDataType.DOUBLE -> stack.pushDouble(reader.readDouble())
            InstructionDataType.FLOAT -> TODO()
            InstructionDataType.BOOLEAN -> TODO()
            InstructionDataType.VARIABLE -> {
                val operand = VariableReferenceOperand(reader.readInt())

                val arrayIndex = if (operand.hasArrayIndex()) stack.popInt32().value else null
                val instanceId = if (operand.hasInstanceIdOnStack()) stack.popInt32().value else extra
                val varId = operand.variableSlot()

                // println("arrayIndex is $arrayIndex")
                // println("instanceId is $instanceId")

                // TODO: Use correct instance here!
                readVariableFromInstanceId(varId, arrayIndex, instanceId)
            }
            InstructionDataType.STRING -> stack.pushString(gameWAD.strg.stringsByIndex[reader.readInt()])
        }
    }

    private fun handlePushBuiltin(reader: StarfaitByteBuffer, type1: Int, extra: Int) {
        // Pushes a builtin variable to the stack
        // handlePushBuiltin type1 is always "VARIABLE"
        val type = InstructionDataType.byId(type1)
        val operand = VariableReferenceOperand(reader.readInt())
        val varId = operand.variableSlot()

        val builtinVariable = builtinVariableHandlers[varId]

        val result = builtinVariable.readHandler.invoke(null, null)
        stack.pushVariable(result)
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

                val arrayIndex = if (operand.hasArrayIndex()) stack.popInt32AndUnwrap() else null
                val instanceId = if (operand.hasInstanceIdOnStack()) stack.popInt32AndUnwrap() else extra

                // println("array index? $arrayIndex")
                // println("instanceID? $instanceId")

                val localVariable = this.currentCallFrame.getVariable(operand.variableSlot(), arrayIndex)
                stack.pushVariable(localVariable)
            }
            InstructionDataType.STRING -> stack.pushString(gameWAD.strg.stringsByIndex[reader.readInt()])
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
                readVariableFromInstanceId(operand.variableSlot(), null, VariableScope.GLOBAL)
            }
            InstructionDataType.STRING -> stack.pushString(gameWAD.strg.stringsByIndex[reader.readInt()])
        }
    }

    private fun readVariableFromInstanceId(varId: Int, arrayIndex: Int?, instanceId: Int) {
        // TECHNICALLY I'm pretty sure that not all push paths write to builtin vs regular variable
        // But to make everything consistent, we'll use the same path for everything
        val variableContainer = when {
            instanceId == VariableScope.SELF -> this.currentSelfInstance
            instanceId == VariableScope.OTHER -> requireNotNull(this.currentCallFrame.other)
            instanceId == VariableScope.LOCAL -> this.currentCallFrame
            instanceId == VariableScope.GLOBAL -> this.global
            instanceId >= 0 -> runner.firstInstance {
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

        if (variableContainer == null) {
            if (instanceId >= StarfaitRunner.BASE_INSTANCE_ID) {
                error("Unable to find any instance with ID $instanceId to read variable \"${getVariableNameFromVariableSlot(varId)}\"")
            } else {
                val gameObjectName = gameWAD.strg.getString(gameWAD.objt.objects[instanceId].name)
                error("Unable to find any instance for object \"$gameObjectName\" (Object Index: $instanceId) to read variable \"${getVariableNameFromVariableSlot(varId)}\"")
            }
        }

        val result = if (varId >= StarfaitUtils.REGULAR_VARIABLES_BASE) {
            variableContainer.getVariable(varId, arrayIndex)
        } else {
            // Builtin variable!
            check(variableContainer is Instance) { "Cannot write a built-in variable to a non-instance!" }

            val builtinVariableHandler = builtinVariableHandlers[varId]
            builtinVariableHandler.readHandler.invoke(variableContainer, arrayIndex)
        }
        stack.pushVariable(result)

        logVariableAccess(varId, variableContainer, arrayIndex, this.traceVariableReads, result, false)
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
            logVariableAccess(varId, variableContainer, arrayIndex, this.traceVariableWrites, poppedValue, true)

            if (varId >= StarfaitUtils.REGULAR_VARIABLES_BASE) {
                variableContainer.setVariable(varId, poppedValue, arrayIndex)
            } else {
                // Builtin variable!
                check(variableContainer is Instance) { "Cannot write a built-in variable to a non-instance!" }

                val builtinVariableHandler = builtinVariableHandlers[varId]
                builtinVariableHandler.writeHandler.invoke(variableContainer, poppedValue, arrayIndex)
            }
        }
    }

    private fun convertVariableContainerToHumanized(variableContainer: VariableContainer, includeInstanceId: Boolean): String {
        return when (variableContainer) {
            is Instance -> buildString {
                append(variableContainer.objectName)
                if (includeInstanceId)
                    append("<${variableContainer.id}>")
            }
            is GlobalObject -> "global"
            is CallFrame -> "local"
            else -> error("Unsupported variable container type!")
        }
    }

    private fun logVariableAccess(varId: Int, variableContainer: VariableContainer, arrayIndex: Int?, set: Set<String>, value: RValue, isWrite: Boolean) {
        if (set.isNotEmpty()) {
            val variableName = getVariableNameFromVariableSlot(varId)
            val variableContainerName = convertVariableContainerToHumanized(variableContainer, false)

            if (set.contains(currentCallFrameCodeName) || set.contains(variableName) || set.contains(variableContainerName) || set.contains("$variableContainerName.$variableName") || set.contains("*")) {
                print("VM: [$currentCallFrameCodeName] Variable ${if (isWrite) "Write" else "Read"} ${convertVariableContainerToHumanized(variableContainer, true)}.${getVariableNameFromVariableSlot(varId)}")
                if (arrayIndex != null)
                    print("[$arrayIndex]")
                println(" = ${value.toStringTyped()}")
            }
        }
    }

    private fun handlePop(reader: StarfaitByteBuffer, type1: Int, type2: Int, extra: Int) {
        val type1DataType = InstructionDataType.byId(type1)
        val typeOnTheStack = InstructionDataType.byId(type2)

        when (type1DataType) {
            InstructionDataType.DOUBLE -> TODO()
            InstructionDataType.FLOAT -> TODO()
            InstructionDataType.INT32 -> {
                // Seems to be used for global array writes, for example: "global.flag[70] += 1;"
                // It is similar to the VARIABLE path, with the change that the value comes BEFORE, not AFTER
                // TODO: It also seems to be used for any kind of array write? Test later
                val operand = VariableReferenceOperand(reader.readInt())

                val varId = operand.variableSlot()
                val poppedValue = stack.popVariable().value
                val arrayIndex = if (operand.hasArrayIndex()) stack.popInt32AndUnwrap() else null
                val instanceId = if (operand.hasInstanceIdOnStack()) stack.popInt32AndUnwrap() else extra

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

                val varId = operand.variableSlot()
                val arrayIndex = if (operand.hasArrayIndex()) stack.popInt32AndUnwrap() else null
                val instanceId = if (operand.hasInstanceIdOnStack()) stack.popInt32AndUnwrap() else extra

                val poppedValue = when (typeOnTheStack) {
                    InstructionDataType.DOUBLE -> stack.popDoubleAndUnwrap().let { RValue.Real(it) }
                    InstructionDataType.FLOAT -> TODO()
                    InstructionDataType.INT32 -> stack.popInt32AndUnwrap().let { RValue.Int32(it) }
                    InstructionDataType.INT64 -> stack.popInt64AndUnwrap().let { RValue.Int64(it) }
                    InstructionDataType.BOOLEAN -> stack.popBooleanLikeAndUnwrap().let { RValue.Boolean(it) }
                    InstructionDataType.VARIABLE -> stack.popVariableAndUnwrap()
                    InstructionDataType.STRING -> stack.popStringAndUnwrap().let { RValue.String(it) }
                    InstructionDataType.INT16 -> TODO()
                }

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

        val topOfTheStack = when (sourceType) {
            InstructionDataType.DOUBLE -> stack.popDouble()
            InstructionDataType.FLOAT -> TODO()
            InstructionDataType.INT32 -> stack.popInt32()
            InstructionDataType.INT64 -> stack.popInt64()
            InstructionDataType.BOOLEAN -> stack.popBoolean()
            InstructionDataType.VARIABLE -> stack.popVariable()
            InstructionDataType.STRING -> stack.popString()
            InstructionDataType.INT16 -> TODO()
        }

        // println("Converting from $sourceType to $destinationType")
        // println("Top of the stack is ${topOfTheStack.toStringTyped()}")

        when (destinationType) {
            InstructionDataType.DOUBLE -> stack.pushDouble(topOfTheStack.convertToReal())
            InstructionDataType.FLOAT -> TODO()
            InstructionDataType.INT32 -> stack.pushInt32(topOfTheStack.convertToInt32())
            InstructionDataType.INT64 -> TODO()
            InstructionDataType.BOOLEAN -> stack.pushBoolean(topOfTheStack.convertToBoolean())
            InstructionDataType.VARIABLE -> stack.pushVariable(topOfTheStack.convertToVariable())
            InstructionDataType.STRING -> TODO()
            InstructionDataType.INT16 -> TODO()
        }
    }

    private fun handlePushImmediate(reader: StarfaitByteBuffer, extra: Int) {
        stack.pushInt32(extra)
    }

    private fun handleCall(reader: StarfaitByteBuffer, extra: Int) {
        val operand = FunctionReferenceOperand(reader.readInt())
        val functionIndex = operand.functionIndex()

        // extra = parameter count
        val arguments = ArrayList<RValue>(extra)
        repeat(extra) {
            arguments.add(stack.popVariableAndUnwrap())
        }

        val functionName = gameWAD.strg.getString(gameWAD.func.functions[functionIndex].name)
        if (functionCallsToBeTraced.contains(functionName) || functionCallsToBeTraced.contains(currentCallFrameCodeName) || functionCallsToBeTraced.contains(currentCallFrame.self.objectName) || functionCallsToBeTraced.contains("*")) {
            println("VM: [$currentCallFrameCodeName] Calling function \"$functionName(${arguments.joinToString(", ")})\"")
        }

        val builtinFunction = builtins.registeredFunctions[functionName]

        if (builtinFunction != null) {
            // Technically the YoYo Runner does NOT validate the parameter count during runtime, only during compilation
            // But we validate it because we are just built like frfr (and because it helps debugging any call issues)
            if (arguments.size !in builtinFunction.parameterRange)
                error("Wrong number of arguments to built-in function ${functionName}! Expected ${builtinFunction.parameterRange}, got ${arguments.size} instead, with the following arguments: $arguments")

            val result = builtinFunction.execute(this.runner, this, arguments)
            if (functionCallsToBeTraced.contains(functionName) || functionCallsToBeTraced.contains(currentCallFrameCodeName) || functionCallsToBeTraced.contains(currentCallFrame.self.objectName) || functionCallsToBeTraced.contains("*")) {
                println("VM: [$currentCallFrameCodeName] Called built-in function \"$functionName(${arguments.joinToString(", ")})\"! Result: $result")
            }
            stack.pushVariable(result)
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

            if (functionCallsToBeTraced.contains(functionName) || functionCallsToBeTraced.contains(currentCallFrameCodeName) || functionCallsToBeTraced.contains(currentCallFrame.self.objectName) || functionCallsToBeTraced.contains("*")) {
                println("VM: [$currentCallFrameCodeName] Called script function \"$functionName(${arguments.joinToString(", ")})\"! Result: ${returnValue ?: "undefined"}")
            }

            if (returnValue == null) {
                // This happens when...
                // * Op.EXIT is called
                // * The script exits without any "return" or "exit" statement
                stack.pushVariable(RValue.Real(0.0))
                return
            }

            stack.pushVariable(returnValue)
            return
        }

        val dedupKey = "$currentCallFrameCodeName#$functionName"
        if (dedupKey !in loggedUnknownFunctions) {
            println("VM: [$currentCallFrameCodeName] Unknown function \"$functionName(${arguments.joinToString { it.toStringTyped() }})\"!")
            loggedUnknownFunctions.add(dedupKey)
        }

        // As a fallback, we'll push undefined to the stack if the function does not exist
        stack.pushUndefinedVariable()
    }

    private fun handleArithmetic(
        type1: Int,
        type2: Int,
        string: (String, String) -> String,
        real: (Double, Double) -> Double,
        int64: (Long, Long) -> Long,
        int32: (Int, Int) -> Int,
    ) {
        val type1DataType = InstructionDataType.byId(type1)
        val type2DataType = InstructionDataType.byId(type2)

        // println("ADD type1 is $type1DataType, type2 is $type2DataType")
        val right = stack.popByInstructionType(type1DataType)
        val left = stack.popByInstructionType(type2DataType)

        val rightType = right.arithmeticType()
        val leftType = left.arithmeticType()
        val isVariable = type1DataType == InstructionDataType.VARIABLE || type2DataType == InstructionDataType.VARIABLE

        // When pushing variables, it seems that the variable type is ALWAYS a Real
        when {
            leftType == ArithmeticType.STRING && rightType == ArithmeticType.STRING -> {
                val concat = string(left.convertToGMLString(), right.convertToGMLString())

                if (isVariable)
                    stack.pushVariable(RValue.String(concat))
                else
                    stack.pushString(concat)
            }
            // TODO: This shouldn't be always DoAdd
            leftType == ArithmeticType.STRING || rightType == ArithmeticType.STRING -> error("DoAdd :: Execution Error")
            leftType == ArithmeticType.REAL || rightType == ArithmeticType.REAL -> {
                val sum = real(left.convertToReal(), right.convertToReal())

                if (isVariable)
                    stack.pushVariable(RValue.Real(sum))
                else
                    stack.pushDouble(sum)
            }
            leftType == ArithmeticType.INT64 || rightType == ArithmeticType.INT64 -> {
                val sum = int64(left.convertToInt64(), right.convertToInt64())

                if (isVariable)
                    stack.pushVariable(RValue.Real(sum.toDouble()))
                else
                    stack.pushInt64(sum)
            }
            leftType == ArithmeticType.INT32 || rightType == ArithmeticType.INT32 -> {
                val sum = int32(left.convertToInt32(), right.convertToInt32())

                if (isVariable)
                    stack.pushVariable(RValue.Real(sum.toDouble()))
                else
                    stack.pushInt32(sum)
            }
            else -> error("I don't know how to handle $leftType and $rightType!")
        }
    }

    private fun handleAdd(reader: StarfaitByteBuffer, type1: Int, type2: Int) {
        handleArithmetic(
            type1,
            type2,
            { left, right ->
                left + right
            },
            { left, right ->
                left + right
            },
            { left, right ->
                left + right
            },
            { left, right ->
                left + right
            }
        )
    }

    private fun handleSub(reader: StarfaitByteBuffer, type1: Int, type2: Int) {
        handleArithmetic(
            type1,
            type2,
            { _, _ ->
                error("DoSub :: Execution Engine - Cannot operate on string type")
            },
            { left, right ->
                left - right
            },
            { left, right ->
                left - right
            },
            { left, right ->
                left - right
            }
        )
    }

    private fun handleMul(reader: StarfaitByteBuffer, type1: Int, type2: Int) {
        handleArithmetic(
            type1,
            type2,
            { _, _ ->
                // TODO: This supports multiplication, we need to pass the raw StackValue to here
                error("DoMul :: Execution Error")
            },
            { left, right ->
                left * right
            },
            { left, right ->
                left * right
            },
            { left, right ->
                left * right
            }
        )
    }

    private fun handleDiv(reader: StarfaitByteBuffer, type1: Int, type2: Int) {
        handleArithmetic(
            type1,
            type2,
            { _, _ ->
                // TODO: This supports multiplication
                error("DoMul :: Execution Error")
            },
            { left, right ->
                left / right
            },
            { left, right ->
                left / right
            },
            { left, right ->
                left / right
            }
        )
    }

    private fun handleMod(reader: StarfaitByteBuffer, type1: Int, type2: Int) {
        handleArithmetic(
            type1,
            type2,
            { _, _ ->
                // TODO: This supports multiplication
                error("DoMul :: Execution Error")
            },
            { left, right ->
                left % right
            },
            { left, right ->
                left % right
            },
            { left, right ->
                left % right
            }
        )
    }

    private fun handleDup(reader: StarfaitByteBuffer, type1: Int, extra: Int) {
        // In the YoYo Runner, the type1 would've been used to figure out how many bytes to be copied from the stack
        // Because the YoYo Runner stores all the stack in a contiguous byte array, so it NEEDS which type is going to be copied
        // We don't do that because we store everything using tagged RValues, but we need to "unconvert" the type1 value by walking back and figuring out how many "bytes" will be copied
        //
        // extra = how many additional elements will be copied from the stack, that is...
        // If the stack is [a, b], and extra is 1, the result will be [a, b, a, b]
        require(extra >= 0) { "Negative extra value on the dup!" }
        val total = extra + 1

        val type1DataType = InstructionDataType.byId(type1)
        val wantedBytes = total * type1DataType.stackSize.toInt()
        var filledBytes = 0

        var bottom = stack.top - 1
        val stackValuesToBeCopied = mutableListOf<StackValue>()
        while (wantedBytes > filledBytes) {
            val stackValue = stack.values[bottom]
            filledBytes += stackValue.dataType.stackSize
            stackValuesToBeCopied.add(stackValue)
            bottom--
        }

        require(wantedBytes == filledBytes) { "Tried copying too many bytes from the stack! Wanted $wantedBytes bytes but walked $filledBytes bytes" }

        for (stackValue in stackValuesToBeCopied.reversed()) {
            stack.push(stackValue)
        }
    }

    private fun handleBreak(reader: StarfaitByteBuffer) {
        // For *real* games, the BREAK opcode is only used to check the array size to avoid querying out of bounds
        // println("BREAK opcode called, it isn't implemented yet")
    }

    private fun handleNot(reader: StarfaitByteBuffer, type1: Int) {
        val targetType = InstructionDataType.byId(type1)

        when (targetType) {
            InstructionDataType.DOUBLE -> TODO()
            InstructionDataType.FLOAT -> TODO()
            InstructionDataType.INT32 -> stack.pushInt32(stack.popInt32AndUnwrap().inv()) // Same thing as ~Number
            InstructionDataType.INT64 -> TODO()
            InstructionDataType.BOOLEAN -> stack.pushBoolean(!stack.popBooleanLikeAndUnwrap())
            InstructionDataType.VARIABLE -> TODO()
            InstructionDataType.STRING -> TODO()
            InstructionDataType.INT16 -> TODO()
        }
    }

    private fun handleNeg(reader: StarfaitByteBuffer, type1: Int) {
        val targetType = InstructionDataType.byId(type1)

        when (targetType) {
            InstructionDataType.VARIABLE -> {
                when (val poppedValue = stack.popVariableAndUnwrap()) {
                    is RValue.Int32 -> stack.pushVariable(RValue.Int32(-poppedValue.convertToInt32()))
                    is RValue.Real -> stack.pushVariable(RValue.Real(-poppedValue.convertToReal()))
                    else -> error("I don't know how to handle a ${poppedValue}!")
                }
            }
            InstructionDataType.DOUBLE -> stack.pushDouble(stack.popDoubleAndUnwrap())
            InstructionDataType.FLOAT -> TODO()
            InstructionDataType.INT32 -> stack.pushInt32(stack.popInt32AndUnwrap())
            InstructionDataType.INT64 -> stack.pushInt64(stack.popInt64AndUnwrap())
            InstructionDataType.BOOLEAN -> TODO()
            InstructionDataType.STRING -> TODO()
            InstructionDataType.INT16 -> TODO()
        }
    }

    private fun handlePushEnv(reader: StarfaitByteBuffer, ip: Int, branchOffset: Int) {
        val objectId = stack.popInt32AndUnwrap()

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

    fun associateBuiltins(remapResult: StarfaitUtils.RemapResult) {
        // Register the builtin variable handlers
        builtinVariableHandlers = Array(remapResult.allocatedBuiltinVariables.size) { index ->
            val name = gameWAD.strg.getString(remapResult.allocatedBuiltinVariables[index].name)

            // TODO: In the future this would be nicer if we could toggle a "strict mode" (throws exception) and a lenient mode (only logs)
            builtins.registeredVariables[name] ?: BuiltinVariable(
                name,
                { instance, arrayIndex -> error("Tried reading from unimplemented builtin variable \"$name\"! (instance: $instance, arrayIndex: $arrayIndex)") },
                { instance, value, arrayIndex -> error("Tried writing to unimplemented builtin variable \"$name\"! (instance: $instance, value: $value, arrayIndex: $arrayIndex)") },
            )
        }

        // Register the regular variable names
        regularVariableNames = Array(remapResult.allocatedRegularVariables.size) { index -> gameWAD.strg.getString(remapResult.allocatedRegularVariables[index].name) }

        val variableTypeAndNameToSlot = mutableMapOf<Int, MutableMap<String, Int>>()
        for ((index, variable) in remapResult.allocatedBuiltinVariables.withIndex()) {
            val thisType = variableTypeAndNameToSlot.getOrPut(variable.instanceType) { mutableMapOf() }
            val variableName = gameWAD.strg.getString(variable.name)
            val previousValue = thisType.putIfAbsent(gameWAD.strg.getString(variable.name), index)
            check(previousValue == null) { "Variable $variableName was already registered in the name -> variable slot map! Bug?" }
        }

        for ((index, variable) in remapResult.allocatedRegularVariables.withIndex()) {
            // There are TONS of duplicated variable names in the LOCAL instance type, so we'll just bail
            // *Maybe* in the future we could be fancy and add a map including the variable container that holds the variable, but for now, this shall be like this
            if (variable.instanceType == VariableInstanceType.LOCAL)
                continue

            val thisType = variableTypeAndNameToSlot.getOrPut(variable.instanceType) { mutableMapOf() }
            val variableName = gameWAD.strg.getString(variable.name)
            val previousValue = thisType.putIfAbsent(gameWAD.strg.getString(variable.name), index + StarfaitUtils.REGULAR_VARIABLES_BASE)
            check(previousValue == null) { "Variable $variableName was already registered in the name -> variable slot map! Bug?" }
        }
        this.variableNameToId = variableTypeAndNameToSlot
    }

    fun getVariableSlotFromVariableName(variableInstanceType: Int, name: String): Int {
        return variableNameToId[variableInstanceType]!![name] ?: error("I don't know about a variable with name \"$name\"!")
    }

    fun getVariableNameFromVariableSlot(slot: Int): String {
        if (slot >= StarfaitUtils.REGULAR_VARIABLES_BASE) {
            return regularVariableNames.getOrNull(slot - StarfaitUtils.REGULAR_VARIABLES_BASE) ?: return "<UnknownVariable>"
        } else {
            return builtinVariableHandlers[slot].name
        }
    }

    fun reset() {
        this.global.variables.clear()
        this.dsMapPool.clear()
    }
}