package com.mrpowergamerbr.starfait.vm

class BuiltinVariable(
    val readHandler: (source: RValue, arrayIndex: Int?) -> (Unit),
    val writeHandler: (result: RValue) -> (Unit)
)