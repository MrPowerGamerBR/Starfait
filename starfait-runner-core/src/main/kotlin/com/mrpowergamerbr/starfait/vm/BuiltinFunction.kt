package com.mrpowergamerbr.starfait.vm

class BuiltinFunction(
    val parameterCount: Int?,
    val handler: Function1<List<RValue>, RValue>
)