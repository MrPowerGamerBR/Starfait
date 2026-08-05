package com.mrpowergamerbr.starfait.vm

import com.mrpowergamerbr.starfait.runner.Instance
import com.mrpowergamerbr.starfait.wad.Code

class CallFrame(
    val code: Code,
    val currentInstance: Instance,
    val arguments: Array<RValue>,
    val localVariables: MutableMap<Int, RValue>,
    val stackTopWhenTheCallFrameWasCreated: Int,
) {
    var returnValue: RValue? = null
}