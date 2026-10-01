package com.mrpowergamerbr.starfait.vm

import com.mrpowergamerbr.starfait.runner.Instance

class BuiltinVariable(
    val name: String,
    val readHandler: (instance: Instance?, arrayIndex: Int?) -> (RValue),
    val writeHandler: (instance: Instance?, result: RValue, arrayIndex: Int?) -> (Unit)
)