package com.mrpowergamerbr.starfait.vm

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
            { source, arrayIndex ->
                val instance = vm.currentInstance ?: error("No instance bound!")
                readHandler(instance, source, arrayIndex)
            },
            { value, arrayIndex ->
                val instance = vm.currentInstance ?: error("No instance bound!")
                writeHandler(instance, value, arrayIndex)
            }
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
                instance.alarms[arrayIndex!!] = value.int
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

        // ===[ BUILTIN FUNCTIONS ]===
        registerFunction("script_execute", ::script_execute)
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
    }

    fun script_execute(args: List<RValue>, result: RValue) {
        println("Called script_execute! :3 - Args are ${args.joinToString { it.toStringTyped() }}")
        result.setString("howdy!!")
    }

    fun show_debug_message(args: List<RValue>, result: RValue) {
        println("Game: ${args[0].toGMLString()}")
    }

    fun string(args: List<RValue>, result: RValue) {
        result.setString(args[0].toGMLString())
    }
}