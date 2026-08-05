package com.mrpowergamerbr.starfait.vm

import com.mrpowergamerbr.starfait.runner.Instance
import com.mrpowergamerbr.starfait.runner.StarfaitRunner

class VMBuiltins(val vm: StarfaitVM) {
    val runner: StarfaitRunner
        get() = vm.runner

    val registeredFunctions = mutableMapOf<String, Function2<List<RValue>, RValue, Unit>>()
    val registeredVariables = mutableMapOf<String, BuiltinVariable>()

    fun registerFunction(name: String, handler: (List<RValue>, RValue) -> Unit) {
        registeredFunctions[name] = handler
    }

    fun registerVariable(name: String, readHandler: (RValue) -> (Unit), writeHandler: (RValue) -> (Unit)) {
        registeredVariables[name] = BuiltinVariable(readHandler, writeHandler)
    }

    fun registerReadOnlyVariable(name: String, readHandler: (RValue) -> (Unit)) {
        registerVariable(name, readHandler, { _ -> error("Cannot write to read-only variable $name!") })
    }

    fun registerInstanceVariable(name: String, readHandler: (Instance, RValue) -> (Unit), writeHandler: (Instance, RValue) -> (Unit)) {
        registeredVariables[name] = BuiltinVariable(
            {
                val instance = vm.currentInstance ?: error("No instance bound!")
                readHandler(instance, it)
            },
            {
                val instance = vm.currentInstance ?: error("No instance bound!")
                writeHandler(instance, it)
            }
        )
    }

    fun registerReadOnlyInstanceVariable(name: String, readHandler: (Instance, RValue) -> (Unit)) {
        registerInstanceVariable(
            name,
            readHandler,
            { _, _ ->
                error("Cannot write to read-only variable $name!")
            }
        )
    }

    init {
        // ===[ BUILTIN VARIABLES ]===
        registerReadOnlyVariable("os_type") { result -> result.setReal(runner.operatingSystem.id.toDouble()) }
        registerReadOnlyInstanceVariable("id") { instance, result -> result.setReal(instance.id.toDouble()) }
        registerInstanceVariable(
            "x",
            { instance, result ->
                result.setReal(instance.x.toDouble())
            },
            { instance, source ->
                instance.x = source.real.toFloat()
            }
        )
        registerInstanceVariable(
            "image_speed",
            { instance, result ->
                result.setReal(instance.imageSpeed.toDouble())
            },
            { instance, source ->
                instance.imageSpeed = source.real.toFloat()
            }
        )
        repeat(16) {
            registerVariable(
                "argument$it",
                { result ->
                    result.copyFrom(vm.currentCallFrame.arguments[it])
                },
                { source ->
                    vm.currentCallFrame.arguments[it].copyFrom(source)
                }
            )
        }

        // ===[ BUILTIN FUNCTIONS ]===
        registerFunction("script_execute", ::script_execute)
        registerFunction("show_debug_message", ::show_debug_message)
        registerFunction("string", ::string)
        // TODO: Add some stub warnings?
        registerFunction("steam_initialised") { args, result -> result.setBoolean(true) }
        registerFunction("steam_file_exists") { args, result -> result.setBoolean(false) }
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