package com.mrpowergamerbr.starfait.vm

import com.mrpowergamerbr.starfait.runner.EventType
import com.mrpowergamerbr.starfait.runner.Instance
import com.mrpowergamerbr.starfait.runner.PendingRoom
import com.mrpowergamerbr.starfait.runner.StarfaitRunner
import kotlin.reflect.KMutableProperty1

class VMBuiltins(val vm: StarfaitVM) {
    val runner: StarfaitRunner
        get() = vm.runner

    val registeredFunctions = mutableMapOf<String, Function2<List<RValue>, RValue, Unit>>()
    val registeredVariables = mutableMapOf<String, BuiltinVariable>()

    fun registerFunction(name: String, handler: (List<RValue>, RValue) -> Unit) {
        registeredFunctions[name] = handler
    }

    fun registerVariable(name: String, readHandler: (RValue, Int?) -> (Unit), writeHandler: (RValue, Int?) -> (Unit)) {
        registeredVariables[name] = BuiltinVariable(readHandler, writeHandler)
    }

    fun registerReadOnlyVariable(name: String, readHandler: (RValue, Int?) -> (Unit)) {
        registerVariable(name, readHandler, { _, _ -> error("Cannot write to read-only variable $name!") })
    }

    fun registerInstanceVariable(name: String, readHandler: (Instance, RValue, Int?) -> (Unit), writeHandler: (Instance, RValue, Int?) -> (Unit)) {
        registeredVariables[name] = BuiltinVariable(
            { source, arrayIndex -> readHandler(vm.currentInstance, source, arrayIndex) },
            { value, arrayIndex -> writeHandler(vm.currentInstance, value, arrayIndex) }
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
        registerReadOnlyVariable("os_type") { result, arrayIndex -> result.setReal(runner.operatingSystem.id.toDouble()) }
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
                { result, _ ->
                    result.copyFrom(vm.currentCallFrame.arguments[it])
                },
                { source, _ ->
                    vm.currentCallFrame.arguments[it].copyFrom(source)
                }
            )
        }
        // TODO: Pretty sure this is writeable
        registerReadOnlyVariable("argument") { result, arrayIndex ->
            result.copyFrom(vm.currentCallFrame.arguments[arrayIndex!!])
        }
        registerReadOnlyVariable("argument_count") { result, _ ->
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
        registerReadOnlyVariable("room_width") { result, _ -> result.setInt32(runner.currentRoom.width) }
        registerReadOnlyVariable("room_height") { result, _ -> result.setInt32(runner.currentRoom.height) }

        registerReadOnlyVariable("view_current") { result, arrayIndex -> result.setReal(0.0) } // Should be 0 outside of draw events
        registerReadOnlyVariable("view_xview") { result, arrayIndex -> result.setReal(0.0) }
        registerReadOnlyVariable("view_yview") { result, arrayIndex -> result.setReal(0.0) }
        registerReadOnlyVariable("view_wview") { result, arrayIndex -> result.setReal(640.0) }
        registerReadOnlyVariable("view_hview") { result, arrayIndex -> result.setReal(480.0) }
        registerReadOnlyVariable("application_surface") { result, arrayIndex -> result.setReal(-1.0) }

        // ===[ BUILTIN FUNCTIONS ]===
        registerFunction("script_execute") { args, result ->
            val scriptId = args[0].int
            val script = vm.gameWAD.scpt.scripts[scriptId]
            val code = vm.gameWAD.code.codes[script.codeIndex]
            val arguments = args.drop(1).map { it.makeIndependent() }.toTypedArray()
            val scriptResult = vm.executeCode(code, vm.currentInstance, arguments)
            if (scriptResult != null) {
                result.copyFrom(scriptResult)
            } else {
                result.setUndefined()
            }
        }

        registerFunction("show_debug_message", ::show_debug_message)
        registerFunction("string", ::string)
        // TODO: Add some stub warnings?
        registerFunction("steam_initialised") { args, result -> result.setBoolean(true) }
        registerFunction("steam_file_exists") { args, result -> result.setBoolean(false) }
        registerFunction("file_exists") { args, result -> result.setBoolean(false) }
        registerFunction("os_get_language") { args, result -> result.setString("pt") }
        registerFunction("os_get_region") { args, result -> result.setString("BR") }
        registerFunction("string_upper") { args, result -> result.setString(args[0].toGMLString().uppercase()) }
        registerFunction("variable_global_exists") { args, result ->
            val variableName = args[0].toGMLString()
            // TODO: This is kinda nasty, I think it would be better to have a proper variable name -> varId lookup table
            val varId = vm.getVariableIndexFromVariableName(variableName)
            println("varId: $varId")
            result.setBoolean(vm.global.variables.containsKey(varId))
        }
        registerFunction("room_goto_next") { args, result ->
            val newRoomIndex = vm.gameWAD.gen8.roomOrder.indexOf(runner.currentRoom.roomId) + 1
            val room = vm.gameWAD.room.rooms[newRoomIndex]
            runner.pendingRoom = PendingRoom(newRoomIndex, room)
        }
        registerFunction("is_undefined") { args, result ->
            result.setBoolean(args[0].type == RValueDataType.UNDEFINED)
        }
        registerFunction("instance_create") { args, result ->
            val objectId = args[2].int
            val instance = runner.currentRoom.createAndSpawnInstance(objectId)
            result.setInt32(instance.id)
        }
        registerFunction("keyboard_check_pressed") { args, result -> result.setBoolean(false) }
        registerFunction("ds_map_create") { args, result ->
            val newIndex = vm.dsMapPool.size
            vm.dsMapPool.add(DSMap())
            result.setInt32(newIndex)
        }

        registerFunction("ds_map_add") { args, result ->
            val dsMap = vm.dsMapPool[args[0].int]
            dsMap.set(args[1], args[2])
            result.setBoolean(true)
        }

        registerFunction("ds_map_find_value") { args, result ->
            val dsMap = vm.dsMapPool[args[0].int]
            val value = dsMap.get(args[1])
            if (value == null) {
                result.setUndefined()
                return@registerFunction
            }
            result.copyFrom(value)
        }

        registerFunction("string_length") { args, result ->
            if (args[0].type != RValueDataType.STRING) {
                result.setInt32(0)
                return@registerFunction
            }

            val string = args[0].refAsString()
            result.setInt32(string.length)
        }

        registerFunction("string_copy") { args, result ->
            if (args[0].type != RValueDataType.STRING) {
                println("string_copy called with $result")
                result.setString("")
                return@registerFunction
            }

            val index = args[1].int - 1 // "The position of the first character in the string to copy from (numbered from 1)"
            val count = args[2].int

            val string = args[0].refAsString()
            result.setString(string.substring(index, index + count).also { println(it) })
        }

        registerFunction("event_inherited") { args, result ->
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

            result.setUndefined()
        }

        registerFunction("string_replace_all") { args, result ->
            val string = args[0].refAsString()
            result.setString(string.replace(args[1].refAsString(), args[2].refAsString()))
        }

        registerFunction("string_char_at") { args, result ->
            val string = args[0].refAsString()

            // "however if the given index is equal to or smaller than 0, then the first character of the string is returned."
            val index = args[1].int.coerceAtLeast(0)
            val char = string.getOrNull(index)

            // If no character is found or the string is shorter than the given index value, an empty string "" is returned,
            if (char == null) {
                result.setUndefined()
                return@registerFunction
            }

            result.setString(char.toString())
        }

        registerFunction("random") { args, result -> result.setReal(0.0) }
    }

    fun show_debug_message(args: List<RValue>, result: RValue) {
        println("Game: ${args[0].toGMLString()}")
    }

    fun string(args: List<RValue>, result: RValue) {
        result.setString(args[0].toGMLString())
    }
}