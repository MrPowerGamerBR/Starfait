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
    fun registerFunction(name: String, parameterCount: Int?, handler: (List<RValue>) -> BuiltinResult) {
        require(name !in registeredFunctions) { "Tried registering function $name, but it already exists!" }
        registeredFunctions[name] = BuiltinFunction(parameterCount) { args, result ->
            val builtinResult = handler(args)
            when (builtinResult) {
                is BuiltinResult.RValue -> result.copyFrom(builtinResult.value)
                is BuiltinResult.Boolean -> result.setBoolean(builtinResult.value)
                is BuiltinResult.Real -> result.setReal(builtinResult.value)
                is BuiltinResult.String -> result.setString(builtinResult.value)
                BuiltinResult.Undefined -> result.setUndefined()
            }
        }
    }

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
        registerFunction("script_execute", null) { args ->
            val scriptId = args[0].int
            val script = vm.gameWAD.scpt.scripts[scriptId]
            val code = vm.gameWAD.code.codes[script.codeIndex]
            val arguments = args.drop(1).map { it.makeIndependent() }.toTypedArray()
            val scriptResult = vm.executeCode(code, vm.currentInstance, arguments)
            if (scriptResult != null) {
                return@registerFunction BuiltinResult.RValue(scriptResult)
            } else {
                return@registerFunction BuiltinResult.Undefined
            }
        }

        registerFunction("show_debug_message", null) { args ->
            println("Game: ${args[0].toGMLString()}")
            return@registerFunction BuiltinResult.Undefined
        }
        registerFunction("string", 1) { args ->
            return@registerFunction BuiltinResult.String(args[0].toGMLString())
        }
        // TODO: Add some stub warnings?
        registerFunction("steam_initialised", null) { args -> BuiltinResult.Boolean(true) }
        registerFunction("steam_file_exists", null) { args -> BuiltinResult.Boolean(false) }
        registerFunction("file_exists", null) { args -> BuiltinResult.Boolean(false) }
        registerFunction("os_get_language", null) { args -> BuiltinResult.String("pt") }
        registerFunction("os_get_region", null) { args -> BuiltinResult.String("BR") }
        registerFunction("string_upper", null) { args -> BuiltinResult.String(args[0].toGMLString().uppercase()) }
        registerFunction("variable_global_exists", null) { args ->
            val variableName = args[0].toGMLString()
            // TODO: This is kinda nasty, I think it would be better to have a proper variable name -> varId lookup table
            val varId = vm.getVariableIndexFromVariableName(variableName)
            println("varId: $varId")
            return@registerFunction BuiltinResult.Boolean(vm.global.variables.containsKey(varId))
        }
        registerFunction("room_goto_next", null) { args ->
            val newRoomIndex = vm.gameWAD.gen8.roomOrder.indexOf(runner.currentRoom.roomId) + 1
            val room = vm.gameWAD.room.rooms[newRoomIndex]
            runner.pendingRoom = PendingRoom(newRoomIndex, room)
            return@registerFunction BuiltinResult.Undefined
        }
        registerFunction("is_undefined", null) { args ->
            return@registerFunction BuiltinResult.Boolean(args[0].type == RValueDataType.UNDEFINED)
        }
        registerFunction("instance_create", null) { args ->
            val objectId = args[2].int
            val instance = runner.currentRoom.createAndSpawnInstance(objectId)
            return@registerFunction BuiltinResult.Real(instance.id.toDouble())
        }
        registerFunction("keyboard_check_pressed", null) { args -> BuiltinResult.Boolean(false) }
        registerFunction("ds_map_create", null) { args ->
            val newIndex = vm.dsMapPool.size
            vm.dsMapPool.add(DSMap())
            return@registerFunction BuiltinResult.Real(newIndex.toDouble())
        }

        registerFunction("ds_map_add", null) { args ->
            val dsMap = vm.dsMapPool[args[0].int]
            dsMap.set(args[1], args[2])
            return@registerFunction BuiltinResult.Boolean(true)
        }

        registerFunction("ds_map_find_value", null) { args ->
            val dsMap = vm.dsMapPool[args[0].int]
            val value = dsMap.get(args[1])
            if (value == null)
                return@registerFunction BuiltinResult.Undefined

            return@registerFunction BuiltinResult.RValue(value)
        }

        registerFunction("string_length", null) { args ->
            if (args[0].type != RValueDataType.STRING)
                return@registerFunction BuiltinResult.Real(0.0)

            val string = args[0].refAsString()
            return@registerFunction BuiltinResult.Real(string.length.toDouble())
        }

        registerFunction("string_copy", null) { args ->
            if (args[0].type != RValueDataType.STRING)
                return@registerFunction BuiltinResult.String("")

            val index = args[1].int - 1 // "The position of the first character in the string to copy from (numbered from 1)"
            val count = args[2].int

            val string = args[0].refAsString()
            return@registerFunction BuiltinResult.String(string.substring(index, index + count))
        }

        registerFunction("event_inherited", null) { args ->
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

            return@registerFunction BuiltinResult.Undefined
        }

        registerFunction("string_replace_all", null) { args ->
            val string = args[0].refAsString()
            return@registerFunction BuiltinResult.String(string.replace(args[1].refAsString(), args[2].refAsString()))
        }

        registerFunction("string_char_at", null) { args ->
            val string = args[0].refAsString()

            // "with the index starting at 1 for the first character"
            // "however if the given index is equal to or smaller than 0, then the first character of the string is returned."
            val index = args[1].int.coerceAtLeast(1) - 1
            val char = string.getOrNull(index)

            // If no character is found or the string is shorter than the given index value, an empty string "" is returned,
            if (char == null)
                return@registerFunction BuiltinResult.String("")

            return@registerFunction BuiltinResult.String(char.toString())
        }

        registerFunction("random", null) { args -> return@registerFunction BuiltinResult.Real(0.0) }
    }
}