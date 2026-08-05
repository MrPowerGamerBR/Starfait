package com.mrpowergamerbr.starfait.vm

import com.mrpowergamerbr.starfait.wad.Code

class CallFrame(
    val code: Code,
    val arguments: Array<RValue>,
    val localVariables: MutableMap<Int, RValue>,
    val stackTopWhenTheCallFrameWasCreated: Int
)