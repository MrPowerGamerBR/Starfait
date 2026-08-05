package com.mrpowergamerbr.starfait.vm

import com.mrpowergamerbr.starfait.runner.EventType
import com.mrpowergamerbr.starfait.runner.Instance
import com.mrpowergamerbr.starfait.runner.PendingRoom
import com.mrpowergamerbr.starfait.runner.StarfaitRunner
import kotlin.reflect.KMutableProperty1

class VMBuiltins(val vm: StarfaitVM) {
    val runner: StarfaitRunner
        get() = vm.runner

    val registeredFunctions = mutableMapOf<String, BuiltinFunction>()
    val registeredVariables = mutableMapOf<String, BuiltinVariable>()

    /**
     * Registers a function named [name] that has [parameterCount] parameters (null if variadic)
     */
    fun registerRawFunction(name: String, parameterCount: Int?, handler: (List<RValue>, RValue) -> Unit) {
        require(name !in registeredFunctions) { "Tried registering function $name, but it already exists!" }
        registeredFunctions[name] = BuiltinFunction(parameterCount, handler)
    }

    fun interface VoidFunction { fun invoke(args: List<RValue>) }
    fun interface BooleanFunction { fun invoke(args: List<RValue>): Boolean }
    fun interface RealFunction { fun invoke(args: List<RValue>): Double }
    fun interface StringFunction { fun invoke(args: List<RValue>): String }
    fun interface IntAsRealFunction { fun invoke(args: List<RValue>): Int }
    fun interface FloatAsRealFunction { fun invoke(args: List<RValue>): Float }

    fun registerVoidFunction(name: String, parameterCount: Int?, handler: VoidFunction) = registerRawFunction(name, parameterCount) { args, result -> handler.invoke(args); result.setUndefined() }
    fun registerBooleanFunction(name: String, parameterCount: Int?, handler: BooleanFunction) = registerRawFunction(name, parameterCount) { args, result -> result.setBoolean(handler.invoke(args)) }
    fun registerRealFunction(name: String, parameterCount: Int?, handler: RealFunction) = registerRawFunction(name, parameterCount) { args, result -> result.setReal(handler.invoke(args)) }
    fun registerStringFunction(name: String, parameterCount: Int?, handler: StringFunction) = registerRawFunction(name, parameterCount) { args, result -> result.setString(handler.invoke(args)) }
    fun registerIntAsRealFunction(name: String, parameterCount: Int?, handler: IntAsRealFunction) = registerRawFunction(name, parameterCount) { args, result -> result.setReal(handler.invoke(args).toDouble()) }
    fun registerFloatAsRealFunction(name: String, parameterCount: Int?, handler: FloatAsRealFunction) = registerRawFunction(name, parameterCount) { args, result -> result.setReal(handler.invoke(args).toDouble()) }

    fun registerVariable(name: String, readHandler: (Instance?, RValue, Int?) -> (Unit), writeHandler: (Instance?, RValue, Int?) -> (Unit)) {
        require(name !in registeredVariables) { "Tried registering variable $name, but it already exists!" }
        registeredVariables[name] = BuiltinVariable(readHandler, writeHandler)
    }

    fun registerReadOnlyVariable(name: String, readHandler: (Instance?, RValue, Int?) -> (Unit)) {
        registerVariable(name, readHandler, { _, _, _ -> error("Cannot write to read-only variable $name!") })
    }

    fun registerInstanceVariable(name: String, readHandler: (Instance, RValue, Int?) -> (Unit), writeHandler: (Instance, RValue, Int?) -> (Unit)) {
        registeredVariables[name] = BuiltinVariable(
            { instance, source, arrayIndex -> readHandler(requireNotNull(instance), source, arrayIndex) },
            { instance, value, arrayIndex -> writeHandler(requireNotNull(instance), value, arrayIndex) }
        )
    }

    fun registerInstanceRealVariable(name: String, property: KMutableProperty1<Instance, Float>) {
        registerInstanceVariable(
            name,
            { instance, source, _ ->
                source.setReal(property.get(instance).toDouble())
            },
            { instance, result, _ ->
                property.set(instance, result.real.toFloat())
            }
        )
    }

    fun registerReadOnlyInstanceVariable(name: String, readHandler: (Instance, RValue, Int?) -> (Unit)) {
        registerInstanceVariable(
            name,
            readHandler,
            { _, _, _ ->
                error("Cannot write to read-only variable $name!")
            }
        )
    }

    init {
        // ===[ BUILTIN VARIABLES ]===
        registerReadOnlyVariable("os_type") { _, result, arrayIndex -> result.setReal(runner.operatingSystem.id.toDouble()) }
        registerReadOnlyInstanceVariable("id") { instance, result, arrayIndex -> result.setReal(instance.id.toDouble()) }
        registerInstanceRealVariable("x", Instance::x)
        registerInstanceRealVariable("y", Instance::y)
        registerInstanceRealVariable("image_speed", Instance::imageSpeed)
        registerInstanceRealVariable("image_xscale", Instance::imageXScale)
        registerInstanceRealVariable("image_yscale", Instance::imageYScale)
        registerInstanceRealVariable("image_alpha", Instance::imageAlpha)
        registerInstanceRealVariable("image_index", Instance::imageIndex)
        registerInstanceRealVariable("sprite_index", Instance::spriteIndex)
        repeat(16) {
            registerVariable(
                "argument$it",
                { _, result, _ ->
                    result.copyFrom(vm.currentCallFrame.arguments[it])
                },
                { _, source, _ ->
                    vm.currentCallFrame.arguments[it].copyFrom(source)
                }
            )
        }
        // TODO: Pretty sure this is writeable
        registerReadOnlyVariable("argument") { _, result, arrayIndex ->
            result.copyFrom(vm.currentCallFrame.arguments[arrayIndex!!])
        }
        registerReadOnlyVariable("argument_count") { _, result, _ ->
            result.setReal(vm.currentCallFrame.arguments.size.toDouble())
        }
        registerInstanceVariable(
            "alarm",
            { instance, source, arrayIndex ->
                source.setReal(instance.alarms[arrayIndex!!].toDouble())
            },
            { instance, value, arrayIndex ->
                instance.setAlarm(arrayIndex!!, value.int)
            }
        )
        registerInstanceVariable(
            "visible",
            { instance, source, _ ->
                source.setBoolean(instance.visible)
            },
            { instance, value, _ ->
                instance.visible = value.bool
            }
        )
        // TODO: Those should be writeable
        registerReadOnlyVariable("room_width") { _, result, _ -> result.setInt32(runner.currentRoom.width) }
        registerReadOnlyVariable("room_height") { _, result, _ -> result.setInt32(runner.currentRoom.height) }

        registerReadOnlyVariable("view_current") { _, result, arrayIndex -> result.setReal(0.0) } // Should be 0 outside of draw events
        registerReadOnlyVariable("view_xview") { _, result, arrayIndex -> result.setReal(0.0) }
        registerReadOnlyVariable("view_yview") { _, result, arrayIndex -> result.setReal(0.0) }
        registerReadOnlyVariable("view_wview") { _, result, arrayIndex -> result.setReal(640.0) }
        registerReadOnlyVariable("view_hview") { _, result, arrayIndex -> result.setReal(480.0) }
        registerReadOnlyVariable("application_surface") { _, result, arrayIndex -> result.setReal(-1.0) }

        // ===[ BUILTIN FUNCTIONS ]===
        registerRawFunction("script_execute", null) { args, result ->
            val scriptId = args[0].int
            val script = vm.gameWAD.scpt.scripts[scriptId]
            val code = vm.gameWAD.code.codes[script.codeIndex]
            val arguments = args.drop(1).map { it.makeIndependent() }.toTypedArray()
            val scriptResult = vm.executeCode(code, vm.currentInstance, arguments)
            if (scriptResult != null) {
                return@registerRawFunction result.copyFrom(scriptResult)
            } else {
                return@registerRawFunction result.setUndefined()
            }
        }

        registerVoidFunction("show_debug_message", null) { args -> println("Game: ${args[0].toGMLString()}") }
        registerStringFunction("string", 1) { args -> args[0].toGMLString() }

        // TODO: Add some stub warnings?
        registerBooleanFunction("steam_initialised", 0) { _ -> true }
        registerBooleanFunction("steam_file_exists", 1) { _ -> false }
        registerBooleanFunction("file_exists", 1) { _ -> false }
        registerStringFunction("os_get_language", 0) { _ -> "pt" }
        registerStringFunction("os_get_region", 0) { _ -> "BR" }
        registerStringFunction("string_upper", 1) { args -> args[0].toGMLString().uppercase() }
        registerBooleanFunction("variable_global_exists", null) { args ->
            val variableName = args[0].toGMLString()
            // TODO: This is kinda nasty, I think it would be better to have a proper variable name -> varId lookup table
            val varId = vm.getVariableIndexFromVariableName(variableName)
            println("varId: $varId")
            return@registerBooleanFunction vm.global.variables.containsKey(varId)
        }
        registerVoidFunction("room_goto_next", 0) { args ->
            val newRoomIndex = vm.gameWAD.gen8.roomOrder.indexOf(runner.currentRoom.roomId) + 1
            val room = vm.gameWAD.room.rooms[newRoomIndex]
            runner.pendingRoom = PendingRoom(newRoomIndex, room)
        }
        registerBooleanFunction("is_undefined", 1) { args -> args[0].type == RValueDataType.UNDEFINED }
        registerIntAsRealFunction("instance_create", null) { args ->
            val objectId = args[2].int
            val instance = runner.currentRoom.createAndSpawnInstance(objectId)
            return@registerIntAsRealFunction instance.id
        }
        registerBooleanFunction("keyboard_check_pressed", null) { args -> false }

        registerIntAsRealFunction("ds_map_create", 0) { args ->
            val newIndex = vm.dsMapPool.size
            vm.dsMapPool.add(DSMap())
            return@registerIntAsRealFunction newIndex
        }

        registerBooleanFunction("ds_map_add", 3) { args ->
            val dsMap = vm.dsMapPool[args[0].int]
            dsMap.set(args[1], args[2])
            return@registerBooleanFunction true
        }

        registerRawFunction("ds_map_find_value", null) { args, result ->
            val dsMap = vm.dsMapPool[args[0].int]
            val value = dsMap.get(args[1])
            if (value == null)
                return@registerRawFunction result.setUndefined()

            return@registerRawFunction result.copyFrom(value)
        }

        registerIntAsRealFunction("string_length", 1) { args ->
            if (args[0].type != RValueDataType.STRING)
                return@registerIntAsRealFunction 0

            val string = args[0].refAsString()
            return@registerIntAsRealFunction string.length
        }

        registerStringFunction("string_copy", null) { args ->
            if (args[0].type != RValueDataType.STRING)
                return@registerStringFunction ""

            val index = args[1].int - 1 // "The position of the first character in the string to copy from (numbered from 1)"
            val count = args[2].int

            val string = args[0].refAsString()
            return@registerStringFunction string.substring(index, index + count)
        }

        registerVoidFunction("event_inherited", 0) { args ->
            // event_inherited is like a script_execute, but for parent events
            val currentInstance = vm.currentInstance
            println("My parent ID is ${currentInstance.gameObject.parentId}")
            var gameObject = this.runner.gameWAD.objt.objects[currentInstance.gameObject.parentId]
            val events = gameObject.events[EventType.CREATE]

            var subtypeEvents = events
                .filter { it.subtype == 0 }
                .flatMap { it.actions }

            // TODO: This SUCKS, we really should precalculate all events ahead of time on boot
            var attempts = 0
            while (subtypeEvents.isEmpty()) {
                if (0 > gameObject.parentId)
                    break

                gameObject = runner.gameWAD.objt.objects[gameObject.parentId]
                subtypeEvents = gameObject.events[EventType.CREATE]
                    .filter { it.subtype == 0 }
                    .flatMap { it.actions }
                attempts++
            }

            for (action in subtypeEvents) {
                val code = this.runner.gameWAD.code.codes[action.codeId]
                this.runner.vm.executeCode(code, currentInstance, emptyArray())
            }
        }

        registerStringFunction("string_replace_all", 3) { args ->
            val string = args[0].refAsString()
            return@registerStringFunction string.replace(args[1].refAsString(), args[2].refAsString())
        }

        registerStringFunction("string_char_at", 2) { args ->
            val string = args[0].refAsString()

            // "with the index starting at 1 for the first character"
            // "however if the given index is equal to or smaller than 0, then the first character of the string is returned."
            val index = args[1].int.coerceAtLeast(1) - 1

            // If no character is found or the string is shorter than the given index value, an empty string "" is returned,
            val char = string.getOrNull(index) ?: return@registerStringFunction ""

            return@registerStringFunction char.toString()
        }

        registerRealFunction("random", null) { args -> 0.0 }
    }
}