package com.mrpowergamerbr.starfait.vm

import com.mrpowergamerbr.starfait.runner.Instance

class BuiltinVariable(
    val readHandler: (instance: Instance?, source: RValue, arrayIndex: Int?) -> (Unit),
    val writeHandler: (instance: Instance?, result: RValue, arrayIndex: Int?) -> (Unit)
)