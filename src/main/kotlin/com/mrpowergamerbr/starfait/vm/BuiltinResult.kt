package com.mrpowergamerbr.starfait.vm

sealed class BuiltinResult {
    data class String(val value: kotlin.String) : BuiltinResult()
    data class Boolean(val value: kotlin.Boolean) : BuiltinResult()
    data class Real(val value: kotlin.Double) : BuiltinResult()
    data class RValue(val value: com.mrpowergamerbr.starfait.vm.RValue) : BuiltinResult()
    data object Undefined : BuiltinResult()
}