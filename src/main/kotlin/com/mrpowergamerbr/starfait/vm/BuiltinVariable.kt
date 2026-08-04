package com.mrpowergamerbr.starfait.vm

class BuiltinVariable(
    val readHandler: (RValue) -> (Unit),
    val writeHandler: (RValue) -> (Unit)
)