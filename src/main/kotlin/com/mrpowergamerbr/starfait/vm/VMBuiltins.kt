package com.mrpowergamerbr.starfait.vm

import com.mrpowergamerbr.starfait.GMLColor
import com.mrpowergamerbr.starfait.MathUtils
import com.mrpowergamerbr.starfait.runner.*
import kotlin.math.*
import kotlin.reflect.KMutableProperty1


class VMBuiltins(val vm: StarfaitVM) {
    val runner: StarfaitRunner
        get() = vm.runner

    val registeredFunctions = mutableMapOf<String, BuiltinFunction>()
    val registeredVariables = mutableMapOf<String, BuiltinVariable>()

    /**
     * Registers a function named [name] that has [parameterCount] parameters (null if variadic)
     */
    fun registerFunction(name: String, parameterCount: Int?, handler: (List<RValue>) -> RValue) {
        require(name !in registeredFunctions) { "Tried registering function $name, but it already exists!" }
        registeredFunctions[name] = BuiltinFunction(parameterCount, handler)
    }

    /**
     * Registers a function named [name] that has [parameterCount] parameters (null if variadic) that always returns [RValue.Undefined]
     */
    fun registerVoidFunction(name: String, parameterCount: Int?, handler: (List<RValue>) -> Unit) = registerFunction(name, parameterCount) { args ->
        handler(args)
        return@registerFunction RValue.Undefined
    }

    /**
     * Registers a function named [name] that has [parameterCount] parameters (null if variadic) that always returns [RValue.String]
     */
    fun registerStringFunction(name: String, parameterCount: Int?, handler: (List<RValue>) -> String) = registerFunction(name, parameterCount) { args -> RValue.String(handler(args)) }

    /**
     * Registers a function named [name] that has [parameterCount] parameters (null if variadic) that converts the result to a [RValue.Boolean]
     */
    fun registerBooleanFunction(name: String, parameterCount: Int?, handler: (List<RValue>) -> Boolean) = registerFunction(name, parameterCount) { args -> RValue.Boolean(handler(args)) }

    /**
     * Registers a function named [name] that has [parameterCount] parameters (null if variadic) that converts the result to a [RValue.getValueAsReal()]
     */
    fun registerRealFunction(name: String, parameterCount: Int?, handler: (List<RValue>) -> Double) = registerFunction(name, parameterCount) { args -> RValue.Real(handler(args)) }

    /**
     * Registers a function named [name] that has [parameterCount] parameters (null if variadic) that converts the result to a [RValue.getValueAsReal()]
     *
     * Even though GameMaker has a [RValue.getValueAsInt32()32], functions ALWAYS returns reals, even if it is an integer
     */
    fun registerIntFunction(name: String, parameterCount: Int?, handler: (List<RValue>) -> Int) = registerFunction(name, parameterCount) { args -> RValue.Real(handler(args).toDouble()) }

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

    fun registerStubbedFunction(name: String, parameterCount: Int?, value: RValue) = registerFunction(name, parameterCount) { _ -> warnFunctionStub(name); value }
    fun registerStubbedFunction(name: String, parameterCount: Int?, value: Boolean) = registerStubbedFunction(name, parameterCount, RValue.Boolean(value))
    fun registerStubbedFunction(name: String, parameterCount: Int?, value: Double) = registerStubbedFunction(name, parameterCount, RValue.Real(value))
    fun registerStubbedFunction(name: String, parameterCount: Int?, value: String) = registerStubbedFunction(name, parameterCount, RValue.String(value))

    fun registerVariable(name: String, readHandler: (Instance?, Int?) -> (RValue), writeHandler: (Instance?, RValue, Int?) -> (Unit)) {
        require(name !in registeredVariables) { "Tried registering variable $name, but it already exists!" }
        registeredVariables[name] = BuiltinVariable(name, readHandler, writeHandler)
    }

    fun registerReadOnlyVariable(name: String, readHandler: (Instance?, Int?) -> (RValue)) {
        registerVariable(name, readHandler, { _, _, _ -> error("Cannot write to read-only variable $name!") })
    }

    fun registerRealStubbedVariable(name: String, value: Double) = registerReadOnlyVariable(name) { _, _ -> warnVariableStub(name); RValue.Real(value) }

    fun registerInstanceVariable(name: String, readHandler: (Instance, Int?) -> (RValue), writeHandler: (Instance, RValue, Int?) -> (Unit)) {
        registerVariable(
            name,
            { instance, arrayIndex -> readHandler(requireNotNull(instance), arrayIndex) },
            { instance, value, arrayIndex -> writeHandler(requireNotNull(instance), value, arrayIndex) }
        )
    }

    fun registerInstanceRealVariable(name: String, property: KMutableProperty1<Instance, Float>) {
        registerInstanceVariable(
            name,
            { instance, _ ->
                RValue.Real(property.get(instance).toDouble())
            },
            { instance, result, _ ->
                property.set(instance, result.convertToReal().toFloat())
            }
        )
    }

    fun registerInstanceBooleanVariable(name: String, property: KMutableProperty1<Instance, Boolean>) {
        registerInstanceVariable(
            name,
            { instance, _ ->
                RValue.Boolean(property.get(instance))
            },
            { instance, result, _ ->
                property.set(instance, result.convertToBoolean())
            }
        )
    }

    fun registerReadOnlyInstanceVariable(name: String, readHandler: (Instance, Int?) -> (RValue)) {
        registerInstanceVariable(
            name,
            readHandler,
            { _, _, _ ->
                error("Cannot write to read-only variable $name!")
            }
        )
    }

    fun registerViewVariable(name: String, readHandler: (Instance?, RunnerRoom.RunnerView, Int?) -> (RValue), writeHandler: (Instance?, RunnerRoom.RunnerView, RValue, Int?) -> (Unit)) {
        registerVariable(
            name,
            { instance, arrayIndex ->
                val view = runner.currentRoom.views[arrayIndex!!]
                readHandler(instance, view, arrayIndex)
            },
            { instance, value, arrayIndex ->
                val view = runner.currentRoom.views[arrayIndex!!]
                writeHandler(instance, view, value, arrayIndex)
            }
        )
    }

    fun registerViewIntVariable(name: String, property: KMutableProperty1<RunnerRoom.RunnerView, Int>) {
        registerViewVariable(
            name,
            { _, view, _ ->
                RValue.Real(property.get(view).toDouble())
            },
            { _, view, value, _ ->
                property.set(view, value.convertToReal().toInt())
            }
        )
    }

    init {
        // ===[ BUILTIN VARIABLES ]===
        registerReadOnlyVariable("os_type") { _, arrayIndex -> RValue.Real(runner.operatingSystem.id.toDouble()) }
        registerReadOnlyInstanceVariable("id") { instance, arrayIndex -> RValue.Real(instance.id.toDouble()) }
        registerInstanceRealVariable("x", Instance::x)
        registerInstanceRealVariable("y", Instance::y)
        registerInstanceRealVariable("image_speed", Instance::imageSpeed)
        registerInstanceRealVariable("image_xscale", Instance::imageXScale)
        registerInstanceRealVariable("image_yscale", Instance::imageYScale)
        registerInstanceRealVariable("image_alpha", Instance::imageAlpha)
        registerInstanceRealVariable("image_index", Instance::imageIndex)
        registerInstanceRealVariable("sprite_index", Instance::spriteIndex)
        registerInstanceRealVariable("mask_index", Instance::maskIndex)
        registerInstanceRealVariable("depth", Instance::depth)
        registerInstanceRealVariable("direction", Instance::direction)
        registerInstanceRealVariable("image_blend", Instance::imageBlend)
        registerInstanceRealVariable("image_angle", Instance::imageAngle)
        registerInstanceRealVariable("path_speed", Instance::pathSpeed)
        registerInstanceRealVariable("path_position", Instance::pathPosition)
        registerInstanceRealVariable("speed", Instance::speed)
        registerInstanceRealVariable("hspeed", Instance::hspeed)
        registerInstanceRealVariable("vspeed", Instance::vspeed)
        registerInstanceRealVariable("friction", Instance::friction)
        registerInstanceRealVariable("gravity", Instance::gravity)
        registerInstanceRealVariable("gravity_direction", Instance::gravityDirection)
        registerInstanceRealVariable("xstart", Instance::xStart)
        registerInstanceRealVariable("ystart", Instance::yStart)
        registerReadOnlyInstanceVariable("sprite_width") { instance, _ ->
            val sprite = runner.gameWAD.sprt.sprites.getOrNull(instance.spriteIndex.toInt())
            RValue.Real((sprite?.width?.toDouble() ?: 0.0) * instance.imageXScale)
        }
        registerReadOnlyInstanceVariable("sprite_height") { instance, _ ->
            val sprite = runner.gameWAD.sprt.sprites.getOrNull(instance.spriteIndex.toInt())
            RValue.Real((sprite?.height?.toDouble() ?: 0.0) * instance.imageYScale)
        }
        registerReadOnlyInstanceVariable("image_number") { instance, _ ->
            val sprite = runner.gameWAD.sprt.sprites.getOrNull(instance.spriteIndex.toInt())
            RValue.Real((sprite?.frames?.size?.toDouble() ?: 0.0))
        }
        registerInstanceBooleanVariable("solid", Instance::solid)
        registerVariable("room_persistent", { _, _ -> RValue.Boolean(runner.currentRoom.persistent) }, { _, value, _ -> runner.currentRoom.persistent = value.convertToBoolean() })
        repeat(16) {
            registerVariable(
                "argument$it",
                { _, _ ->
                    vm.currentCallFrame.arguments[it]
                },
                { _, source, _ ->
                    vm.currentCallFrame.arguments[it] = source
                }
            )
        }
        registerVariable("background_color", { _, _ -> RValue.Int32(runner.currentRoom.backgroundColor.value) }, { _, value, _ -> runner.currentRoom.backgroundColor = GMLColor(value.convertToInt32()) })
        registerVariable(
            "background_visible",
            { _, arrayIndex ->
                RValue.Boolean(runner.currentRoom.backgrounds[arrayIndex!!].enabled)
            },
            { _, value, arrayIndex ->
                runner.currentRoom.backgrounds[arrayIndex!!].enabled = value.convertToBoolean()
            }
        )
        registerVariable(
            "background_index",
            { _, arrayIndex ->
                RValue.Real(runner.currentRoom.backgrounds[arrayIndex!!].backgroundIndex.toDouble())
            },
            { _, value, arrayIndex ->
                runner.currentRoom.backgrounds[arrayIndex!!].backgroundIndex = value.convertToInt32()
            }
        )
        registerVariable(
            "background_x",
            { _, arrayIndex ->
                RValue.Real(runner.currentRoom.backgrounds[arrayIndex!!].x.toDouble())
            },
            { _, value, arrayIndex ->
                runner.currentRoom.backgrounds[arrayIndex!!].x = value.convertToInt32()
            }
        )
        registerVariable(
            "background_y",
            { _, arrayIndex ->
                RValue.Real(runner.currentRoom.backgrounds[arrayIndex!!].y.toDouble())
            },
            { _, value, arrayIndex ->
                runner.currentRoom.backgrounds[arrayIndex!!].y = value.convertToInt32()
            }
        )
        registerVariable(
            "background_alpha",
            { _, arrayIndex ->
                RValue.Real(runner.currentRoom.backgrounds[arrayIndex!!].alpha.toDouble())
            },
            { _, value, arrayIndex ->
                runner.currentRoom.backgrounds[arrayIndex!!].alpha = value.convertToReal().toFloat()
            }
        )
        // TODO: Pretty sure this is writeable
        registerReadOnlyVariable("argument") { _, arrayIndex ->
            vm.currentCallFrame.arguments[arrayIndex!!]
        }
        registerReadOnlyVariable("argument_count") { _, _ ->
            RValue.Real(vm.currentCallFrame.arguments.size.toDouble())
        }
        registerInstanceVariable(
            "alarm",
            { instance, arrayIndex ->
                RValue.Real(instance.alarms[arrayIndex!!].toDouble())
            },
            { instance, value, arrayIndex ->
                instance.setAlarm(arrayIndex!!, value.convertToInt32())
            }
        )
        registerInstanceVariable(
            "visible",
            { instance, _ ->
                RValue.Boolean(instance.visible)
            },
            { instance, value, _ ->
                instance.visible = value.convertToBoolean()
            }
        )
        registerReadOnlyInstanceVariable(
            "object_index",
            { instance, _ ->
                RValue.Real(instance.objectIndex.toDouble())
            }
        )

        // TODO: Those should be writeable
        registerReadOnlyVariable("room_width") { _, _ -> RValue.Int32(runner.currentRoom.width) }
        registerReadOnlyVariable("room_height") { _, _ -> RValue.Int32(runner.currentRoom.height) }

        registerRealStubbedVariable("view_current", 0.0) // Should be 0 outside of draw events

        registerViewIntVariable("view_xview", RunnerRoom.RunnerView::viewX)
        registerViewIntVariable("view_yview", RunnerRoom.RunnerView::viewY)
        registerViewIntVariable("view_wview", RunnerRoom.RunnerView::viewWidth)
        registerViewIntVariable("view_hview", RunnerRoom.RunnerView::viewHeight)
        registerViewIntVariable("view_object", RunnerRoom.RunnerView::objectIndex)

        registerRealStubbedVariable("application_surface", -1.0)

        registerReadOnlyVariable("current_time") { _, _ -> RValue.Real((System.currentTimeMillis() - runner.startedAt).toDouble()) }

        // TODO: This should be writeable
        registerReadOnlyVariable("room") { _, _ -> RValue.Int32(runner.currentRoom.roomId) }

        registerReadOnlyInstanceVariable("bbox_left") { instance, _ ->
            val bbox = instance.getBoundingBox()
            RValue.Real(bbox.left.toDouble())
        }
        registerReadOnlyInstanceVariable("bbox_right") { instance, _ ->
            val bbox = instance.getBoundingBox()
            RValue.Real(bbox.right.toDouble())
        }
        registerReadOnlyInstanceVariable("bbox_top") { instance, _ ->
            val bbox = instance.getBoundingBox()
            RValue.Real(bbox.top.toDouble())
        }
        registerReadOnlyInstanceVariable("bbox_bottom") { instance, _ ->
            val bbox = instance.getBoundingBox()
            RValue.Real(bbox.bottom.toDouble())
        }

        registerReadOnlyInstanceVariable("xprevious") { instance, _ -> RValue.Real(instance.xPrevious.toDouble()) }
        registerReadOnlyInstanceVariable("yprevious") { instance, _ -> RValue.Real(instance.yPrevious.toDouble()) }

        // ===[ BUILTIN FUNCTIONS ]===
        registerFunction("script_execute", null) { args ->
            val callFrame = vm.currentCallFrame
            val scriptId = args[0].convertToInt32()
            val script = vm.gameWAD.scpt.scripts[scriptId]
            val code = vm.gameWAD.code.codes[script.codeIndex]
            val arguments = args.drop(1)
            val scriptResult = vm.executeCode(code, vm.currentSelfInstance, callFrame.other, callFrame.eventObject, callFrame.eventType, callFrame.eventSubtype, arguments.toTypedArray())
            if (scriptResult != null) {
                return@registerFunction scriptResult
            } else {
                return@registerFunction RValue.Undefined
            }
        }

        registerVoidFunction("show_debug_message", null) { args -> runner.output.showDebugMessage(args[0].convertToGMLString()) }
        registerFunction("string", 1) { args -> RValue.String(args[0].convertToGMLString()) }
        registerFunction("real", 1) { args -> RValue.Real(args[0].convertToReal()) }

        registerStubbedFunction("steam_initialised", 0, true)
        registerStubbedFunction("steam_file_exists", 1, false)
        registerStubbedFunction("file_exists", 1, false)
        registerStubbedFunction("os_get_language", 0, "pt")
        registerStubbedFunction("os_get_region", 0, "BR")

        registerStringFunction("string_upper", 1) { args -> args[0].convertToGMLString().uppercase() }
        registerStringFunction("string_lower", 1) { args -> args[0].convertToGMLString().lowercase() }
        registerStubbedFunction("string_height", 1, 0.0)
        registerBooleanFunction("variable_global_exists", null) { args ->
            val variableName = args[0].convertToGMLString()
            // TODO: This is kinda nasty, I think it would be better to have a proper variable name -> varId lookup table
            val varId = vm.getVariableSlotFromVariableName(VariableInstanceType.GLOBAL, variableName)
            println("varId: $varId")
            return@registerBooleanFunction vm.global.variables.containsKey(varId)
        }
        registerVoidFunction("room_goto_next", 0) { args ->
            val newRoomIndexInRoomOrder = vm.gameWAD.gen8.roomOrder.indexOf(runner.currentRoom.roomId) + 1
            val newRoomIndex = vm.gameWAD.gen8.roomOrder[newRoomIndexInRoomOrder]
            val room = vm.gameWAD.room.rooms[newRoomIndex]
            runner.setPendingRoom(newRoomIndex)
        }
        registerVoidFunction("room_goto_previous", 0) { args ->
            val newRoomIndexInRoomOrder = vm.gameWAD.gen8.roomOrder.indexOf(runner.currentRoom.roomId) - 1
            val newRoomIndex = vm.gameWAD.gen8.roomOrder[newRoomIndexInRoomOrder]
            runner.setPendingRoom(newRoomIndex)
        }
        registerVoidFunction("room_goto", 1) { args ->
            val newRoomIndex = args[0].convertToInt32()
            runner.setPendingRoom(newRoomIndex)
        }
        registerIntFunction("room_next", 1) { args ->
            val roomIndex = args[0].convertToInt32()
            val positionInRoomOrder = vm.gameWAD.gen8.roomOrder.indexOf(roomIndex)
            val newRoomIndex = vm.gameWAD.gen8.roomOrder[positionInRoomOrder + 1]
            newRoomIndex
        }
        registerIntFunction("room_previous", 1) { args ->
            val roomIndex = args[0].convertToInt32()
            val positionInRoomOrder = vm.gameWAD.gen8.roomOrder.indexOf(roomIndex)
            val newRoomIndex = vm.gameWAD.gen8.roomOrder[positionInRoomOrder - 1]
            newRoomIndex
        }
        registerBooleanFunction("is_undefined", 1) { args -> args[0] is RValue.Undefined }
        registerIntFunction("instance_create", 3) { args ->
            val x = args[0].convertToReal()
            val y = args[1].convertToReal()
            val objectId = args[2].convertToInt32()

            val instance = runner.currentRoom.createAndSpawnInstance(objectId) {
                this.x = x.toFloat()
                this.y = y.toFloat()
            }
            instance.dispatchEvent(EventType.CREATE, 0, null)

            return@registerIntFunction instance.id
        }
        registerVoidFunction("instance_destroy", null) { args ->
            // TODO: instance ID
            // TODO: execute_event_flag
            vm.currentSelfInstance.markAsDestroyed(true)
        }

        registerBooleanFunction("keyboard_check", 1) { args ->
            val keyId = args[0].convertToInt32()
            return@registerBooleanFunction runner.keyboard.keyHeld[keyId]
        }

        registerBooleanFunction("keyboard_check_pressed", 1) { args ->
            val keyId = args[0].convertToInt32()
            return@registerBooleanFunction runner.keyboard.keyPressed[keyId]
        }

        registerBooleanFunction("keyboard_check_released", 1) { args ->
            val keyId = args[0].convertToInt32()
            return@registerBooleanFunction runner.keyboard.keyReleased[keyId]
        }

        registerBooleanFunction("keyboard_check_direct", 1) { args ->
            val keyId = args[0].convertToInt32()
            return@registerBooleanFunction runner.keyboard.keyHeld[keyId]
        }

        registerStubbedFunction("joystick_exists", 1, false)

        registerIntFunction("ds_map_create", 0) { args ->
            val newIndex = vm.dsMapPool.size
            vm.dsMapPool.add(DSMap())
            return@registerIntFunction newIndex
        }

        registerBooleanFunction("ds_map_add", 3) { args ->
            val dsMap = vm.dsMapPool[args[0].convertToInt32()]
            if (dsMap.containsKey(args[1]))
                return@registerBooleanFunction false

            dsMap.set(args[1], args[2])
            return@registerBooleanFunction true
        }

        registerVoidFunction("ds_map_set", 3) { args ->
            val dsMap = vm.dsMapPool[args[0].convertToInt32()]
            dsMap.set(args[1], args[2])
        }

        registerFunction("ds_map_find_value", null) { args ->
            val dsMap = vm.dsMapPool[args[0].convertToInt32()]
            val value = dsMap.get(args[1])
            if (value == null)
                return@registerFunction RValue.Undefined

            return@registerFunction value
        }

        registerIntFunction("string_length", 1) { args ->
            val string = (args[0] as? RValue.String)?.value ?: return@registerIntFunction 0
            return@registerIntFunction string.length
        }

        registerStringFunction("string_copy", null) { args ->
            val string = (args[0] as? RValue.String)?.value ?: return@registerStringFunction ""

            val index = (args[1].convertToInt32() - 1).coerceAtLeast(0) // "The position of the first character in the string to copy from (numbered from 1)"
            val count = (args[2].convertToInt32()).coerceAtLeast(0)

            // When testing in GameMaker: Studio 1.4.9999, it seems that the string_copy copies TRASH if you are copying beyond the string length
            // We don't do that, because it doesn't make sense to do that nor can it be reliably reproduced
            if (index >= string.length || count == 0)
                return@registerStringFunction ""

            // For anything else, we'll just cap it at index + count
            val coercedEndIndex = (index + count).coerceAtMost(string.length)

            if (index == coercedEndIndex)
                return@registerStringFunction ""

            return@registerStringFunction string.substring(index, coercedEndIndex)
        }

        registerStringFunction("string_delete", null) { args ->
            val string = (args[0] as? RValue.String)?.value ?: return@registerStringFunction ""

            // "The position of the first character to remove (from 1). Negative indices are supported and count
            // from the end of the string, e.g. -1 indicates the last character."
            val rawIndex = args[1].convertToInt32()
            val index = if (0 > rawIndex) string.length + rawIndex else rawIndex - 1

            // "A negative number means characters are deleted backwards. For example, -4 indicates the character
            // at index and the 3 characters in front of it will be deleted."
            val count = args[2].convertToInt32()

            val begin: Int
            val end: Int
            if (count >= 0) {
                begin = index
                end = index + count
            } else {
                end = index + 1
                begin = index + 1 + count
            }

            val coercedBegin = begin.coerceIn(0, string.length)
            val coercedEnd = end.coerceIn(0, string.length)

            if (coercedBegin >= coercedEnd)
                return@registerStringFunction string

            return@registerStringFunction string.substring(0, coercedBegin) + string.substring(coercedEnd)
        }

        registerVoidFunction("event_inherited", 0) { args ->
            // event_inherited is like a script_execute, but for parent events
            val callFrame = vm.currentCallFrame
            val currentInstance = callFrame.self

            val parentId = callFrame.eventObject.parentId
            var gameObject = this.runner.gameWAD.objt.objects[parentId]

            currentInstance.dispatchEvent(parentId, gameObject, callFrame.eventType, callFrame.eventSubtype, callFrame.other)
        }

        registerVoidFunction("event_user", 1) { args ->
            val number = args[0].convertToInt32()
            require(number in 0..15) { "User Event is out of bounds!" }
            // event_user is like event_inherited, but for user events
            val currentInstance = vm.currentSelfInstance
            currentInstance.dispatchEvent(EventType.OTHER, EventType.OtherSubType.USER_0 + number, null)
        }

        registerVoidFunction("event_perform", 2) { args ->
            // event_perform is like event_user, but for all events
            val eventType = args[0].convertToInt32()
            val eventSubtype = args[1].convertToInt32()
            val currentInstance = vm.currentSelfInstance
            currentInstance.dispatchEvent(eventType, eventSubtype, null)
        }

        registerStringFunction("string_replace_all", 3) { args ->
            val string = args[0].convertToGMLString()
            val oldValue = (args[1] as RValue.String).value
            val newValue = (args[2] as RValue.String).value

            return@registerStringFunction string.replace(oldValue, newValue)
        }

        registerStringFunction("string_char_at", 2) { args ->
            val string = args[0].convertToGMLString()

            // "with the index starting at 1 for the first character"
            // "however if the given index is equal to or smaller than 0, then the first character of the string is returned."
            val index = args[1].convertToInt32().coerceAtLeast(1) - 1

            // If no character is found or the string is shorter than the given index value, an empty string "" is returned,
            val char = string.getOrNull(index) ?: return@registerStringFunction ""

            return@registerStringFunction char.toString()
        }

        registerIntFunction("string_pos", 2) { args ->
            val string = (args[0] as RValue.String).value
            val substr = (args[1] as RValue.String).value

            val index = string.indexOf(substr)

            // "The function will return 0 if it's not found at all, or the position of the first character of the sub-string if it is found.
            // Keep in mind that for legacy support strings are indexed from 1, so 1 is the first position in the string, not 0 as you may expect."
            return@registerIntFunction index + 1
        }

        registerRealFunction("random", 1) { args ->
            return@registerRealFunction runner.random.nextDouble() * args[0].convertToReal()
        }

        registerIntFunction("irandom", 1) { args ->
            val bound = args[0].convertToInt32()

            // If it is 0 it won't ever have any other random number :3
            if (bound == 0) {
                return@registerIntFunction 0
            } else if (bound > 0) {
                return@registerIntFunction runner.random.nextInt(bound + 1)
            } else {
                return@registerIntFunction -runner.random.nextInt(-bound + 1)
            }
        }

        registerBooleanFunction("instance_exists", 1) { args ->
            val queryId = args[0].convertToInt32()

            // "This function does not take into account those instances that have been deactivated using the instance deactivate functions."
            runner.forEachInstance { instance ->
                val matches = instance.matchesObjectOrInstanceId(
                    queryId,
                    excludeInstance = null,
                    skipDestroyed = true,
                    checkHierarchy = true,
                    checkSpecialSelf = true,
                    checkSpecialOther = true,
                    checkSpecialAll = true,
                    checkSpecialNoone = true
                )

                if (matches)
                    return@registerBooleanFunction true
            }

            return@registerBooleanFunction false
        }

        registerIntFunction("instance_number", 1) { args ->
            val queryId = args[0].convertToInt32()

            var count = 0
            // "This function does not take into account those instances that have been deactivated using the instance deactivate functions."
            runner.forEachInstance { instance ->
                val matches = instance.matchesObjectOrInstanceId(
                    queryId,
                    excludeInstance = null,
                    skipDestroyed = true,
                    checkHierarchy = true,
                    checkSpecialSelf = true,
                    checkSpecialOther = true,
                    checkSpecialAll = true,
                    checkSpecialNoone = true
                )

                if (matches)
                    count++
            }

            return@registerIntFunction count
        }

        registerIntFunction("instance_find", 2) { args ->
            val queryId = args[0].convertToInt32()
            val targetIndex = args[1].convertToInt32()

            var index = 0
            // TODO: Does this need to check for deactivated instances?
            runner.forEachInstance { instance ->
                val matches = instance.matchesObjectOrInstanceId(
                    queryId,
                    excludeInstance = null,
                    skipDestroyed = true,
                    checkHierarchy = true,
                    checkSpecialSelf = true,
                    checkSpecialOther = true,
                    checkSpecialAll = true,
                    checkSpecialNoone = true
                )

                if (matches) {
                    if (index == targetIndex)
                        return@registerIntFunction instance.id

                    index++
                }
            }

            return@registerIntFunction InstanceReferenceType.NOONE
        }

        registerVoidFunction("draw_text_transformed", 6) { args ->
            val x = args[0].convertToReal()
            val y = args[1].convertToReal()
            val string = args[2].convertToGMLString()
            val font = requireNotNull(runner.drawState.font)

            runner.renderer.drawText(font, x.toFloat(), y.toFloat(), string)
        }

        registerVoidFunction("draw_text", 3) { args ->
            val x = args[0].convertToReal()
            val y = args[1].convertToReal()
            val string = args[2].convertToGMLString()
            val font = requireNotNull(runner.drawState.font)

            runner.renderer.drawText(font, x.toFloat(), y.toFloat(), string)
        }

        registerRealFunction("round", 1) { args ->
            MathUtils.delphiRound(args[0].convertToReal()).toDouble()
        }

        registerStubbedFunction("surface_get_width", 1, 320.0)
        registerStubbedFunction("surface_get_height", 1, 240.0)
        registerStubbedFunction("window_get_fullscreen", 0, false)
        registerStubbedFunction("ini_section_exists", 1, false)

        registerVoidFunction("draw_sprite", 4) { args ->
            runner.renderer.drawSprite(vm.gameWAD.sprt.sprites[args[0].convertToInt32()], args[1].convertToInt32(), args[2].convertToReal().toFloat(), args[3].convertToReal().toFloat(), 1f, 1f, 0f, GMLColor(0xFFFFFF), runner.drawState.alpha)
        }

        registerVoidFunction("draw_sprite_ext", 9) { args ->
            runner.renderer.drawSprite(
                vm.gameWAD.sprt.sprites[args[0].convertToInt32()],
                args[1].convertToInt32(),
                args[2].convertToReal().toFloat(),
                args[3].convertToReal().toFloat(),
                args[4].convertToReal().toFloat(),
                args[5].convertToReal().toFloat(),
                args[6].convertToReal().toFloat(),
                GMLColor(args[7].convertToInt32()),
                args[8].convertToReal().toFloat(),
            )
        }

        registerStringFunction("chr", 1) { args -> args[0].convertToInt32().toChar().toString() }
        registerIntFunction("ord", 1) { args -> args[0].convertToGMLString()[0].code }

        registerIntFunction("collision_rectangle", 7) { args ->
            val self = vm.currentSelfInstance
            val x1 = args[0].convertToReal()
            val y1 = args[1].convertToReal()
            val x2 = args[2].convertToReal()
            val y2 = args[3].convertToReal()
            val obj = args[4].convertToInt32()
            val usePreciseCollisionCheck = args[5].convertToBoolean()
            val notme = args[6].convertToBoolean()

            val selfBBox = BoundingBox.fromWorldCoordinates(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat())

            if (runner.enableCollisionRendering)
                runner.debugRectangles.add(selfBBox)

            runner.forEachInstance { instance ->
                val matches = instance.matchesObjectOrInstanceId(
                    obj,
                    excludeInstance = if (notme) self else null,
                    skipDestroyed = true,
                    checkHierarchy = true,
                    checkSpecialSelf = true,
                    checkSpecialOther = true,
                    checkSpecialAll = true,
                    checkSpecialNoone = true
                )

                if (matches) {
                    val bbox = instance.getBoundingBox()

                    if (selfBBox.isCollidingWith(bbox, runner.collisionCompatibilityMode)) {
                        return@registerIntFunction instance.id
                    }
                }
            }

            return@registerIntFunction InstanceReferenceType.NOONE
        }

        registerIntFunction("collision_point", 5) { args ->
            val self = vm.currentSelfInstance
            val x = args[0].convertToReal().toFloat()
            val y = args[1].convertToReal().toFloat()
            val obj = args[2].convertToInt32()
            val usePreciseCollisionCheck = args[3].convertToBoolean()
            val notme = args[4].convertToBoolean()

            runner.forEachInstance { instance ->
                val matches = instance.matchesObjectOrInstanceId(
                    obj,
                    excludeInstance = if (notme) self else null,
                    skipDestroyed = true,
                    checkHierarchy = true,
                    checkSpecialSelf = true,
                    checkSpecialOther = true,
                    checkSpecialAll = true,
                    checkSpecialNoone = true
                )

                if (matches) {
                    val bbox = instance.getBoundingBox()

                    if (bbox.contains(x, y)) {
                        return@registerIntFunction instance.id
                    }
                }
            }

            return@registerIntFunction InstanceReferenceType.NOONE
        }

        registerIntFunction("instance_position", 3) { args ->
            val x = args[0].convertToReal().toFloat()
            val y = args[1].convertToReal().toFloat()
            val obj = args[2].convertToInt32()

            runner.forEachInstance { instance ->
                val matches = instance.matchesObjectOrInstanceId(
                    obj,
                    excludeInstance = null,
                    skipDestroyed = true,
                    checkHierarchy = true,
                    checkSpecialSelf = true,
                    checkSpecialOther = true,
                    checkSpecialAll = true,
                    checkSpecialNoone = true
                )

                if (matches && instance.getBoundingBox().contains(x, y)) {
                    return@registerIntFunction instance.id
                }
            }

            return@registerIntFunction InstanceReferenceType.NOONE
        }

        registerVoidFunction("move_snap", 2) { args ->
            val currentInstance = vm.currentSelfInstance
            val gridWidth = args[0].convertToReal()
            val gridHeight = args[1].convertToReal()

            val gridCellX = MathUtils.delphiRound(currentInstance.x / gridWidth)
            val gridCellY = MathUtils.delphiRound(currentInstance.y / gridHeight)

            currentInstance.x = (gridCellX * gridWidth).toFloat()
            currentInstance.y = (gridCellY * gridHeight).toFloat()
        }

        registerIntFunction("collision_line", 7) { args ->
            val self = vm.currentSelfInstance
            val x1 = args[0].convertToReal().toFloat()
            val y1 = args[1].convertToReal().toFloat()
            val x2 = args[2].convertToReal().toFloat()
            val y2 = args[3].convertToReal().toFloat()
            val obj = args[4].convertToInt32()
            val usePreciseCollisionCheck = args[5].convertToBoolean()
            val notme = args[6].convertToBoolean()

            runner.forEachInstance { instance ->
                val matches = instance.matchesObjectOrInstanceId(
                    obj,
                    excludeInstance = if (notme) self else null,
                    skipDestroyed = true,
                    checkHierarchy = true,
                    checkSpecialSelf = true,
                    checkSpecialOther = true,
                    checkSpecialAll = true,
                    checkSpecialNoone = true
                )

                if (matches) {
                    if (instance.getBoundingBox().intersectsLine(x1, y1, x2, y2)) {
                        return@registerIntFunction instance.id
                    }
                }
            }

            return@registerIntFunction InstanceReferenceType.NOONE
        }

        registerRealFunction("abs", 1) { args -> args[0].convertToReal().absoluteValue }

        // TODO: set_colour too? maybe we need to make a way to do aliases?
        registerVoidFunction("draw_set_color", 1) { args ->
            runner.drawState.color = GMLColor(args[0].convertToInt32())
        }

        registerVoidFunction("draw_set_font", 1) { args ->
            runner.drawState.font = runner.gameWAD.font.fonts[args[0].convertToInt32()]
        }

        registerRealFunction("floor", 1) { args -> floor(args[0].convertToReal()) }
        registerRealFunction("ceil", 1) { args -> ceil(args[0].convertToReal()) }
        registerRealFunction("sin", 1) { args -> sin(args[0].convertToReal()) }
        registerRealFunction("cos", 1) { args -> cos(args[0].convertToReal()) }

        registerFunction("choose", null) { args ->
            return@registerFunction args.random(runner.random)
        }

        registerIntFunction("string_width", 1) { args ->
            val text = args[0].convertToGMLString()
            val font = requireNotNull(runner.drawState.font)

            // This is essentially how the text is rendered... but WITHOUT the rendering
            var width = 0
            for (char in text) {
                // TODO: HashMap lookup for the glyphs would be pretty swell
                val glyph = font.glyphs.firstOrNull { it.character == char }

                if (glyph != null) {
                    width += glyph.shift
                }
            }

            return@registerIntFunction width
        }

        registerVoidFunction("draw_rectangle", 5) { args ->
            val x1 = args[0].convertToReal()
            val y1 = args[1].convertToReal()
            val x2 = args[2].convertToReal()
            val y2 = args[3].convertToReal()
            // TODO: outline support
            val outline = args[4].convertToBoolean()
            if (outline)
                warnFunctionSemiStub("draw_rectangle")

            runner.renderer.fillRectangle(x1.toFloat(), y1.toFloat(), (x2 - x1).toFloat() + 1f, (y2 - y1).toFloat() + 1f, runner.drawState.color, runner.drawState.alpha)
        }

        // See GameMaker-HTML5's Function_Path.js
        registerVoidFunction("path_start", 4) { args ->
            vm.currentSelfInstance.assignPath(args[0].convertToInt32(), args[1].convertToReal().toFloat(), 1.0f, 0.0f, args[3].convertToBoolean(), args[2].convertToInt32())
        }

        registerVoidFunction("path_end", 0) { _ ->
            vm.currentSelfInstance.assignPath(-1, 0.0f, 1.0f, 0.0f, false, 0)
        }

        registerVoidFunction("move_towards_point", 3) { args ->
            val x = args[0].convertToReal()
            val y = args[1].convertToReal()
            val sp = args[2].convertToReal()

            val dx = x - vm.currentSelfInstance.x
            val dy = y - vm.currentSelfInstance.y
            val angle = Math.toDegrees(atan2(-dy, dx))

            val direction = angle.toFloat() % 360
            vm.currentSelfInstance.direction = direction
            vm.currentSelfInstance.speed = sp.toFloat()
        }

        registerVoidFunction("action_move_to", 2) { args ->
            var x = args[0].convertToReal()
            var y = args[1].convertToReal()

            if (vm.dragAndDropRelativeValues) {
                x += vm.currentSelfInstance.x
                y += vm.currentSelfInstance.y
            }

            vm.currentSelfInstance.x = x.toFloat()
            vm.currentSelfInstance.y = y.toFloat()
        }

        registerVoidFunction("action_set_hspeed", 1) { args ->
            val value = args[0].convertToReal().toFloat()
            val self = vm.currentSelfInstance
            self.hspeed = if (vm.dragAndDropRelativeValues) self.hspeed + value else value
        }

        registerVoidFunction("action_set_vspeed", 1) { args ->
            val value = args[0].convertToReal().toFloat()
            val self = vm.currentSelfInstance
            self.vspeed = if (vm.dragAndDropRelativeValues) self.vspeed + value else value
        }

        registerVoidFunction("action_set_friction", 1) { args ->
            val value = args[0].convertToReal().toFloat()
            val self = vm.currentSelfInstance
            self.friction = if (vm.dragAndDropRelativeValues) self.friction + value else value
        }

        registerVoidFunction("action_kill_object", 0) { args ->
            vm.currentSelfInstance.markAsDestroyed(true)
        }

        registerVoidFunction("action_set_relative", 1) { args ->
            val enabled = args[0].convertToBoolean()
            vm.dragAndDropRelativeValues = enabled
        }

        registerVoidFunction("action_set_alarm", 2) { args ->
            var steps = args[0].convertToInt32()
            val alarm = args[1].convertToInt32()

            if (vm.dragAndDropRelativeValues && vm.currentSelfInstance.isAlarmActive(alarm))
                steps += vm.currentSelfInstance.alarms[alarm]

            vm.currentSelfInstance.setAlarm(alarm, steps)
        }

        registerVoidFunction("action_move", 2) { args ->
            val possibleDirections = args[0].convertToGMLString()
            val speed = args[1].convertToReal()

            val enabledIndices = mutableListOf<Int>()

            repeat(9) {
                if (possibleDirections[it] == '1')
                    enabledIndices.add(it)
            }

            val whereAreWeGoing = enabledIndices.random(runner.random)

            val direction = when (whereAreWeGoing) {
                0 -> 225
                1 -> 270
                2 -> 315
                3 -> 180
                4 -> null
                5 -> 0
                6 -> 135
                7 -> 90
                8 -> 45
                else -> error("I don't know how to handle $whereAreWeGoing!")
            }

            if (direction != null) {
                vm.currentSelfInstance.direction = direction.toFloat()
                vm.currentSelfInstance.speed = speed.toFloat()
            } else {
                vm.currentSelfInstance.speed = 0.0f
            }
        }

        registerRealFunction("min", 2) { args ->
            args.minOf { it.convertToReal() }
        }

        registerRealFunction("max", 2) { args ->
            args.maxOf { it.convertToReal() }
        }

        registerRealFunction("distance_to_point", 2) { args ->
            val self = vm.currentCallFrame.self
            val x2 = args[0].convertToReal()
            val y2 = args[1].convertToReal()

            val bbox = self.getBoundingBox()

            // GameMaker-HTML5 does default these values to 0.0 so... ¯\_(ツ)_/¯
            // Get the nearest corner to the target
            val x1 = if (bbox.left > x2) {
                bbox.left
            } else {
                bbox.right
            }

            val y1 = if (bbox.top > y2) {
                bbox.top
            } else {
                bbox.bottom
            }

            return@registerRealFunction sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2))
        }

        registerStubbedFunction("ini_read_real", 3, 0.0)
        registerStubbedFunction("audio_sound_get_gain", 1, 0.0)

        registerFunction("object_get_name", 1) { args ->
            var objectIndex = args[0].convertToInt32()
            // When testing in GameMaker: Studio 1.4.9999 it seems that it DOES NOT throw on a invalid object, it just returns undefined
            val gameObject = vm.gameWAD.objt.objects.getOrNull(objectIndex) ?: return@registerFunction RValue.Undefined
            RValue.String(vm.gameWAD.strg.getString(gameObject.name))
        }

        registerRealFunction("lengthdir_x", 2) { args ->
            val len = args[0].convertToReal()
            val dir = args[1].convertToReal()
            cos(Math.toRadians(dir)) * len
        }

        registerRealFunction("lengthdir_y", 2) { args ->
            val len = args[0].convertToReal()
            val dir = args[1].convertToReal()
            -sin(Math.toRadians(dir)) * len
        }

        registerVoidFunction("draw_sprite_part", 8) { args ->
            val spriteIndex = args[0].convertToInt32()
            val subimg = args[1].convertToInt32()
            val left = args[2].convertToReal().toFloat()
            val top = args[3].convertToReal().toFloat()
            val width = args[4].convertToReal().toFloat()
            val height = args[5].convertToReal().toFloat()
            val x = args[6].convertToReal().toFloat()
            val y = args[7].convertToReal().toFloat()

            val sprite = runner.gameWAD.sprt.sprites[spriteIndex]

            runner.renderer.drawSpritePart(sprite, subimg, left, top, width, height, x, y, 1f, 1f, GMLColor(0xFFFFFF), runner.drawState.alpha)
        }

        registerVoidFunction("draw_sprite_part_ext", 12) { args ->
            val spriteIndex = args[0].convertToInt32()
            val subimg = args[1].convertToInt32()
            val left = args[2].convertToReal().toFloat()
            val top = args[3].convertToReal().toFloat()
            val width = args[4].convertToReal().toFloat()
            val height = args[5].convertToReal().toFloat()
            val x = args[6].convertToReal().toFloat()
            val y = args[7].convertToReal().toFloat()
            val xscale = args[8].convertToReal().toFloat()
            val yscale = args[9].convertToReal().toFloat()
            val color = GMLColor(args[10].convertToInt32())
            val alpha = args[11].convertToReal().toFloat()

            val sprite = runner.gameWAD.sprt.sprites[spriteIndex]

            runner.renderer.drawSpritePart(sprite, subimg, left, top, width, height, x, y, xscale, yscale, color, alpha)
        }

        registerRealFunction("power", 2) { args ->
            args[0].convertToReal().pow(args[1].convertToReal())
        }

        registerVoidFunction("draw_self", 0) { args ->
            runner.renderer.drawInstance(vm.currentSelfInstance)
        }

        registerVoidFunction("draw_set_alpha", 1) { args ->
            runner.drawState.alpha = args[0].convertToReal().toFloat()
        }

        registerIntFunction("audio_play_sound", 3) { args ->
            val index = args[0].convertToInt32()
            val priority = args[1].convertToInt32()
            val loop = args[2].convertToBoolean()

            val sound = runner.gameWAD.sond.sounds[index]
            runner.audioManager.playSound(index, sound, loop)
        }

        registerVoidFunction("audio_stop_sound", 1) { args ->
            val soundIndexOrInstanceId = args[0].convertToInt32()
            runner.audioManager.stopSound(soundIndexOrInstanceId)
        }

        registerVoidFunction("audio_stop_all", 0) { args ->
            runner.audioManager.stopAllSounds()
        }

        registerBooleanFunction("audio_is_playing", 1) { args ->
            val soundIndexOrInstanceId = args[0].convertToInt32()
            runner.audioManager.isAudioPlaying(soundIndexOrInstanceId)
        }

        registerVoidFunction("audio_pause_sound", 1) { args ->
            val soundIndexOrInstanceId = args[0].convertToInt32()
            runner.audioManager.pauseSound(soundIndexOrInstanceId)
        }

        registerVoidFunction("audio_resume_sound", 1) { args ->
            val soundIndexOrInstanceId = args[0].convertToInt32()
            runner.audioManager.resumeSound(soundIndexOrInstanceId)
        }

        registerVoidFunction("audio_sound_pitch", 2) { args ->
            val soundIndexOrInstanceId = args[0].convertToInt32()
            val pitch = args[1].convertToReal()

            runner.audioManager.setAudioPitch(soundIndexOrInstanceId, pitch.toFloat())
        }

        registerVoidFunction("audio_sound_gain", null) { args ->
            val soundIndexOrInstanceId = args[0].convertToInt32()
            val gain = args[1].convertToReal()
            val timeMs = args.getOrNull(2)?.convertToInt32() ?: 0

            runner.audioManager.setAudioGain(soundIndexOrInstanceId, gain.toFloat(), timeMs)
        }

        // TODO: make_colour_rgb alias
        registerIntFunction("make_color_rgb", null) { args ->
            val red = args[0].convertToInt32()
            val green = args[1].convertToInt32()
            val blue = args[2].convertToInt32()
            return@registerIntFunction GMLColor.fromRGB(red, green, blue).value
        }

        registerVoidFunction("game_restart", 0) { args ->
            runner.pendingRestart = true
        }
    }
}