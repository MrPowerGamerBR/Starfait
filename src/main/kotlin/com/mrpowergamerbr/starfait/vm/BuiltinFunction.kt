package com.mrpowergamerbr.starfait.vm

class BuiltinFunction(
    val parameterCount: Int?,
    val handler: Function2<List<RValue>, RValue, Unit>
)