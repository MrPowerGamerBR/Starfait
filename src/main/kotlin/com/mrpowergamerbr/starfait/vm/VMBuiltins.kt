package com.mrpowergamerbr.starfait.vm

import kotlin.reflect.KFunction2

class VMBuiltins(val vm: StarfaitVM) {
    val registeredFunctions = mutableMapOf<String, KFunction2<List<RValue>, RValue, Unit>>()

    fun registerFunction(name: String, handler: KFunction2<List<RValue>, RValue, Unit>) {
        registeredFunctions[name] = handler
    }

    init {
        registerFunction("script_execute", ::script_execute)
        registerFunction("show_debug_message", ::show_debug_message)
    }

    fun script_execute(args: List<RValue>, result: RValue) {
        println("Called script_execute! :3 - Args are ${args.joinToString { it.toStringTyped() }}")
        result.setString("howdy!!")
    }

    fun show_debug_message(args: List<RValue>, result: RValue) {
        println("Game: ${args[0].toStringTyped()}")
    }
}