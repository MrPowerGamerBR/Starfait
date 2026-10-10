package com.mrpowergamerbr.starfait.vm

import com.mrpowergamerbr.starfait.GMLColor
import com.mrpowergamerbr.starfait.runner.*
import com.mrpowergamerbr.starfait.vm.builtins.registerBuiltinFunctions
import kotlin.reflect.KMutableProperty1


class VMBuiltins(val vm: StarfaitVM) {
    companion object {
        fun warnFunctionStub(vm: StarfaitVM, name: String) {
            val dedupKey = "${vm.currentCallFrameCodeName}#${name}"
            if (dedupKey !in vm.loggedStubbedFunctions) {
                println("VM: [${vm.currentCallFrameCodeName}] Stubbed function \"$name\"!")
                vm.loggedStubbedFunctions.add(dedupKey)
            }
        }

        fun warnFunctionSemiStub(vm: StarfaitVM, name: String) {
            val dedupKey = "${vm.currentCallFrameCodeName}#${name}"
            if (dedupKey !in vm.loggedStubbedFunctions) {
                println("VM: [${vm.currentCallFrameCodeName}] Semi-Stubbed function \"$name\"!")
                vm.loggedStubbedFunctions.add(dedupKey)
            }
        }
    }

    val runner: StarfaitRunner
        get() = vm.runner

    val registeredFunctions = mutableMapOf<String, BuiltinFunction>()
    val registeredVariables = mutableMapOf<String, BuiltinVariable>()

    /**
     * Registers a [BuiltinFunction]
     */
    fun registerFunction(builtinFunction: BuiltinFunction) {
        for (name in builtinFunction.names) {
            require(name !in registeredFunctions) { "Tried registering function $name, but it already exists!" }
            registeredFunctions[name] = builtinFunction
        }
    }

    fun warnVariableStub(name: String) {
        val dedupKey = "${vm.currentCallFrameCodeName}#${name}"
        if (dedupKey !in vm.loggedStubbedVariables) {
            println("VM: [${vm.currentCallFrameCodeName}] Stubbed built-in variable \"$name\"!")
            vm.loggedStubbedVariables.add(dedupKey)
        }
    }

    fun warnVariableSemiStub(name: String) {
        val dedupKey = "${vm.currentCallFrameCodeName}#${name}"
        if (dedupKey !in vm.loggedStubbedVariables) {
            println("VM: [${vm.currentCallFrameCodeName}] Semi-Stubbed built-in variable \"$name\"!")
            vm.loggedStubbedVariables.add(dedupKey)
        }
    }

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

    fun registerViewBooleanVariable(name: String, property: KMutableProperty1<RunnerRoom.RunnerView, Boolean>) {
        registerViewVariable(
            name,
            { _, view, _ ->
                RValue.Boolean(property.get(view))
            },
            { _, view, value, _ ->
                property.set(view, value.convertToBoolean())
            }
        )
    }

    fun registerViewRealVariable(name: String, property: KMutableProperty1<RunnerRoom.RunnerView, Float>) {
        registerViewVariable(
            name,
            { _, view, _ ->
                RValue.Real(property.get(view).toDouble())
            },
            { _, view, value, _ ->
                property.set(view, value.convertToReal().toFloat())
            }
        )
    }

    init {
        registerBuiltinFunctions(this)

        // ===[ BUILTIN VARIABLES ]===
        registerReadOnlyVariable("os_type") { _, arrayIndex -> RValue.Real(runner.operatingSystem.id.toDouble()) }
        registerReadOnlyInstanceVariable("id") { instance, arrayIndex -> RValue.Real(instance.id.toDouble()) }
        registerReadOnlyVariable("event_type") { instance, arrayIndex -> RValue.Real(vm.currentCallFrame.eventType.toDouble()) }
        registerReadOnlyVariable("event_number") { instance, arrayIndex -> RValue.Real(vm.currentCallFrame.eventSubtype.toDouble()) }
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
            val sprite = runner.resources.sprites.getOrNull(instance.spriteIndex.toInt())
            RValue.Real((sprite?.width?.toDouble() ?: 0.0) * instance.imageXScale)
        }
        registerReadOnlyInstanceVariable("sprite_height") { instance, _ ->
            val sprite = runner.resources.sprites.getOrNull(instance.spriteIndex.toInt())
            RValue.Real((sprite?.height?.toDouble() ?: 0.0) * instance.imageYScale)
        }
        registerReadOnlyInstanceVariable("image_number") { instance, _ ->
            val sprite = runner.resources.sprites.getOrNull(instance.spriteIndex.toInt())
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
        registerRealStubbedVariable("mouse_x", 0.0)
        registerRealStubbedVariable("mouse_y", 0.0)
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
        // TODO: Are these writable? Is this affected by the stretch field?
        registerReadOnlyVariable(
            "background_width",
            { _, arrayIndex ->
                val background = runner.resources.backgrounds[runner.currentRoom.backgrounds[arrayIndex!!].backgroundIndex]
                val tpag = background.texturePageEntry
                RValue.Real(tpag!!.sourceWidth.toDouble())
            }
        )
        registerReadOnlyVariable(
            "background_height",
            { _, arrayIndex ->
                val background = runner.resources.backgrounds[runner.currentRoom.backgrounds[arrayIndex!!].backgroundIndex]
                val tpag = background.texturePageEntry
                RValue.Real(tpag!!.sourceHeight.toDouble())
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

        // TODO: Actually provide the correct FPS
        registerReadOnlyVariable("fps") { _, arrayIndex ->
            warnVariableSemiStub("fps");
            RValue.Real(runner.currentRoom.speed.toDouble())
        }

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

        // If null, then it means that we are not in a draw event
        registerReadOnlyVariable("view_current", { _, _ -> RValue.Int32(runner.currentViewId ?: 0) })

        registerViewIntVariable("view_xview", RunnerRoom.RunnerView::viewX)
        registerViewIntVariable("view_yview", RunnerRoom.RunnerView::viewY)
        registerViewIntVariable("view_wview", RunnerRoom.RunnerView::viewWidth)
        registerViewIntVariable("view_hview", RunnerRoom.RunnerView::viewHeight)
        registerViewIntVariable("view_object", RunnerRoom.RunnerView::objectIndex)
        registerViewBooleanVariable("view_visible", RunnerRoom.RunnerView::visible)
        registerViewIntVariable("view_hborder", RunnerRoom.RunnerView::borderX)
        registerViewIntVariable("view_vborder", RunnerRoom.RunnerView::borderY)
        registerViewRealVariable("view_angle", RunnerRoom.RunnerView::angle)

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
    }
}