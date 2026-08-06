package com.mrpowergamerbr.starfait.vm

import com.mrpowergamerbr.starfait.GMLColor
import com.mrpowergamerbr.starfait.runner.EventType
import com.mrpowergamerbr.starfait.runner.Instance
import com.mrpowergamerbr.starfait.runner.PendingRoom
import com.mrpowergamerbr.starfait.runner.StarfaitRunner
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round
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

    fun warnFunctionStub(name: String) {
        val dedupKey = "${vm.currentCallFrameCodeName}#${name}"
        if (dedupKey !in vm.loggedStubbedFunctions) {
            println("VM: [${vm.currentCallFrameCodeName}] Stubbed function \"$name\"!")
            vm.loggedStubbedFunctions.add(dedupKey)
        }
    }

    fun warnFunctionSemiStub(name: String) {
        val dedupKey = "${vm.currentCallFrameCodeName}#${name}"
        if (dedupKey !in vm.loggedStubbedFunctions) {
            println("VM: [${vm.currentCallFrameCodeName}] Semi-Stubbed function \"$name\"!")
            vm.loggedStubbedFunctions.add(dedupKey)
        }
    }

    fun warnVariableStub(name: String) {
        val dedupKey = "${vm.currentCallFrameCodeName}#${name}"
        if (dedupKey !in vm.loggedStubbedVariables) {
            println("VM: [${vm.currentCallFrameCodeName}] Stubbed built-in variable \"$name\"!")
            vm.loggedStubbedVariables.add(dedupKey)
        }
    }

    fun registerBooleanStubbedFunction(name: String, parameterCount: Int?, value: Boolean) = registerBooleanFunction(name, parameterCount) { _ -> warnFunctionStub(name); value }
    fun registerRealStubbedFunction(name: String, parameterCount: Int?, value: Double) = registerRealFunction(name, parameterCount) { _ -> warnFunctionStub(name); value }
    fun registerStringStubbedFunction(name: String, parameterCount: Int?, value: String) = registerStringFunction(name, parameterCount) { _ -> warnFunctionStub(name); value }

    fun registerVariable(name: String, readHandler: (Instance?, RValue, Int?) -> (Unit), writeHandler: (Instance?, RValue, Int?) -> (Unit)) {
        require(name !in registeredVariables) { "Tried registering variable $name, but it already exists!" }
        registeredVariables[name] = BuiltinVariable(readHandler, writeHandler)
    }

    fun registerReadOnlyVariable(name: String, readHandler: (Instance?, RValue, Int?) -> (Unit)) {
        registerVariable(name, readHandler, { _, _, _ -> error("Cannot write to read-only variable $name!") })
    }

    fun registerRealStubbedVariable(name: String, value: Double) = registerReadOnlyVariable(name) { _, result, _ -> warnVariableStub(name); result.setReal(value) }

    fun registerInstanceVariable(name: String, readHandler: (Instance, RValue, Int?) -> (Unit), writeHandler: (Instance, RValue, Int?) -> (Unit)) {
        registerVariable(
            name,
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

    fun registerInstanceBooleanVariable(name: String, property: KMutableProperty1<Instance, Boolean>) {
        registerInstanceVariable(
            name,
            { instance, source, _ ->
                source.setBoolean(property.get(instance))
            },
            { instance, result, _ ->
                property.set(instance, result.bool)
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
        registerInstanceRealVariable("depth", Instance::depth)
        registerInstanceRealVariable("direction", Instance::direction)
        registerInstanceRealVariable("image_blend", Instance::imageBlend)
        registerInstanceRealVariable("image_angle", Instance::imageAngle)
        registerInstanceRealVariable("path_speed", Instance::pathSpeed)
        registerInstanceRealVariable("path_position", Instance::pathPosition)
        registerInstanceRealVariable("speed", Instance::speed)
        registerInstanceRealVariable("hspeed", Instance::hspeed)
        registerInstanceRealVariable("vspeed", Instance::vspeed)
        registerInstanceBooleanVariable("solid", Instance::solid)
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

        registerRealStubbedVariable("view_current", 0.0) // Should be 0 outside of draw events
        registerVariable(
            "view_xview",
            { _, result, arrayIndex ->
                result.setReal(runner.currentRoom.views[arrayIndex!!].viewX.toDouble())
            },
            { _, value, arrayIndex ->
                runner.currentRoom.views[arrayIndex!!].viewX = value.real.toInt()
            }
        )
        registerVariable(
            "view_yview",
            { _, result, arrayIndex ->
                result.setReal(runner.currentRoom.views[arrayIndex!!].viewY.toDouble())
            },
            { _, value, arrayIndex ->
                runner.currentRoom.views[arrayIndex!!].viewY = value.real.toInt()
            }
        )
        registerVariable(
            "view_wview",
            { _, result, arrayIndex ->
                result.setReal(runner.currentRoom.views[arrayIndex!!].viewWidth.toDouble())
            },
            { _, value, arrayIndex ->
                runner.currentRoom.views[arrayIndex!!].viewWidth = value.real.toInt()
            }
        )
        registerVariable(
            "view_hview",
            { _, result, arrayIndex ->
                result.setReal(runner.currentRoom.views[arrayIndex!!].viewHeight.toDouble())
            },
            { _, value, arrayIndex ->
                runner.currentRoom.views[arrayIndex!!].viewHeight = value.real.toInt()
            }
        )
        registerRealStubbedVariable("application_surface", -1.0)

        registerReadOnlyVariable("current_time") { _, result, _ -> result.setReal((System.currentTimeMillis() - runner.startedAt).toDouble()) }

        registerRealStubbedVariable("sprite_height", 240.0)
        registerRealStubbedVariable("sprite_width", 240.0)

        // TODO: This should be writeable
        registerReadOnlyVariable("room") { _, result, _ -> result.setInt32(runner.currentRoom.roomId) }
        registerReadOnlyInstanceVariable("bbox_top") { _, result, _ -> result.setReal(32.0) }
        registerReadOnlyInstanceVariable("bbox_left") { _, result, _ -> result.setReal(32.0) }
        registerReadOnlyInstanceVariable("bbox_right") { _, result, _ -> result.setReal(32.0) }
        registerReadOnlyInstanceVariable("bbox_bottom") { _, result, _ -> result.setReal(32.0) }
        registerReadOnlyInstanceVariable("xprevious") { instance, result, _ -> result.setReal(instance.x.toDouble()) }
        registerReadOnlyInstanceVariable("yprevious") { instance, result, _ -> result.setReal(instance.y.toDouble()) }

        // ===[ BUILTIN FUNCTIONS ]===
        registerRawFunction("script_execute", null) { args, result ->
            val callFrame = vm.currentCallFrame
            val scriptId = args[0].int
            val script = vm.gameWAD.scpt.scripts[scriptId]
            val code = vm.gameWAD.code.codes[script.codeIndex]
            val arguments = args.drop(1).map { it.makeIndependent() }.toTypedArray()
            val scriptResult = vm.executeCode(code, vm.currentSelfInstance, callFrame.eventObject, callFrame.eventType, callFrame.eventSubtype, arguments)
            if (scriptResult != null) {
                return@registerRawFunction result.copyFrom(scriptResult)
            } else {
                return@registerRawFunction result.setUndefined()
            }
        }

        registerVoidFunction("show_debug_message", null) { args -> println("Game: ${args[0].toGMLString()}") }
        registerStringFunction("string", 1) { args -> args[0].toGMLString() }
        registerRealFunction("real", 1) { args -> args[0].real }

        registerBooleanStubbedFunction("steam_initialised", 0, true)
        registerBooleanStubbedFunction("steam_file_exists", 1, false)
        registerBooleanStubbedFunction("file_exists", 1, false)
        registerStringStubbedFunction("os_get_language", 0, "pt")
        registerStringStubbedFunction("os_get_region", 0, "BR")

        registerStringFunction("string_upper", 1) { args -> args[0].toGMLString().uppercase() }
        registerStringFunction("string_lower", 1) { args -> args[0].toGMLString().lowercase() }
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
        registerVoidFunction("room_goto_previous", 0) { args ->
            val newRoomIndex = vm.gameWAD.gen8.roomOrder.indexOf(runner.currentRoom.roomId) - 1
            val room = vm.gameWAD.room.rooms[newRoomIndex]
            runner.pendingRoom = PendingRoom(newRoomIndex, room)
        }
        registerBooleanFunction("is_undefined", 1) { args -> args[0].type == RValueDataType.UNDEFINED }
        registerIntAsRealFunction("instance_create", 3) { args ->
            val x = args[0].real
            val y = args[1].real
            val objectId = args[2].int

            val instance = runner.currentRoom.createAndSpawnInstance(objectId) {
                this.x = x.toFloat()
                this.y = y.toFloat()
            }

            return@registerIntAsRealFunction instance.id
        }
        registerVoidFunction("instance_destroy", null) { args ->
            vm.currentSelfInstance.destroyed = true
        }

        registerBooleanFunction("keyboard_check", 1) { args ->
            val keyId = args[0].int
            return@registerBooleanFunction runner.keyboard.keyHeld[keyId]
        }

        registerBooleanFunction("keyboard_check_pressed", 1) { args ->
            val keyId = args[0].int
            return@registerBooleanFunction runner.keyboard.keyPressed[keyId]
        }

        registerBooleanFunction("keyboard_check_released", 1) { args ->
            val keyId = args[0].int
            return@registerBooleanFunction runner.keyboard.keyReleased[keyId]
        }

        registerBooleanFunction("keyboard_check_direct", 1) { args ->
            val keyId = args[0].int
            return@registerBooleanFunction runner.keyboard.keyHeld[keyId]
        }

        registerBooleanStubbedFunction("joystick_exists", 1, false)


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
            val currentInstance = vm.currentSelfInstance
            val callFrame = vm.currentCallFrame

            var gameObject = this.runner.gameWAD.objt.objects[callFrame.eventObject.parentId]

            val events = gameObject.events[callFrame.eventType]
            var subtypeEvents = events
                .filter { it.subtype == callFrame.eventSubtype }
                .flatMap { it.actions }

            // TODO: This SUCKS, we really should precalculate all events ahead of time on boot
            var attempts = 0
            while (subtypeEvents.isEmpty()) {
                if (0 > gameObject.parentId)
                    break

                gameObject = runner.gameWAD.objt.objects[gameObject.parentId]
                subtypeEvents = gameObject.events[callFrame.eventType]
                    .filter { it.subtype == callFrame.eventSubtype }
                    .flatMap { it.actions }
                attempts++
            }

            for (action in subtypeEvents) {
                val code = this.runner.gameWAD.code.codes[action.codeId]
                println("Executing code ${this.runner.gameWAD.strg.getString(code.name)}")
                this.runner.vm.executeCode(code, currentInstance, gameObject, callFrame.eventType, callFrame.eventSubtype, emptyArray())
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

        registerRealStubbedFunction("random", 1, 0.0)

        registerBooleanFunction("instance_exists", 1) { args ->
            // "This function does not take into account those instances that have been deactivated using the instance deactivate functions."
            // TODO: Inheritance
            for (instance in runner.currentRoom.instances) {
                if (instance.destroyed)
                    continue

                if (instance.objectIndex == args[0].int)
                    return@registerBooleanFunction true
            }

            return@registerBooleanFunction false
        }

        registerVoidFunction("draw_text_transformed", 6) { args ->
            val x = args[0].real
            val y = args[1].real
            val string = args[2].toGMLString()

            runner.renderer.drawText(runner.gameWAD.font.fonts[2], x.toFloat(), y.toFloat(), string)
        }

        registerVoidFunction("draw_text", 3) { args ->
            val x = args[0].real
            val y = args[1].real
            val string = args[2].toGMLString()

            runner.renderer.drawText(runner.gameWAD.font.fonts[2], x.toFloat(), y.toFloat(), string)
        }

        registerRealFunction("round", 1) { args ->
            // TODO: delphi round! (bankers rounding)
            round(args[0].real)
        }

        registerRealStubbedFunction("surface_get_width", 1, 320.0)
        registerRealStubbedFunction("surface_get_height", 1, 240.0)
        registerBooleanStubbedFunction("window_get_fullscreen", 0, false)
        registerBooleanStubbedFunction("ini_section_exists", 1, false)

        registerVoidFunction("draw_sprite", 4) { args ->
            runner.renderer.drawSprite(vm.gameWAD.sprt.sprites[args[0].int], args[1].int, args[2].real.toFloat(), args[3].real.toFloat())
        }

        registerVoidFunction("draw_sprite_ext", 9) { args ->
            warnFunctionSemiStub("draw_sprite_ext")
            runner.renderer.drawSprite(vm.gameWAD.sprt.sprites[args[0].int], args[1].int, args[2].real.toFloat(), args[3].real.toFloat())
        }

        registerStringFunction("chr", 1) { args -> args[0].int.toChar().toString() }
        registerIntAsRealFunction("ord", 1) { args -> args[0].refAsString()[0].code }

        registerRealStubbedFunction("collision_point", 5, SpecialObjectReferenceType.NOONE.toDouble())
        registerRealStubbedFunction("collision_rectangle", 7, SpecialObjectReferenceType.NOONE.toDouble())

        registerRealFunction("abs", 1) { args -> args[0].real.absoluteValue }

        registerBooleanStubbedFunction("audio_is_playing", 1, false)

        // TODO: set_colour too? maybe we need to make a way to do aliases?
        registerVoidFunction("draw_set_color", 1) { args ->
            runner.drawSetColor = GMLColor(args[0].int)
            runner.renderer.setDrawColor(runner.drawSetColor)
        }

        registerRealFunction("floor", 1) { args -> floor(args[0].real) }
        registerRealFunction("ceil", 1) { args -> ceil(args[0].real) }
    }
}