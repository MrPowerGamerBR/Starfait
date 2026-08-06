package com.mrpowergamerbr.starfait.vm

import com.mrpowergamerbr.starfait.runner.Instance
import com.mrpowergamerbr.starfait.wad.Code

class CallFrame(
    val code: Code,
    val initialSelf: Instance,
    val initialOther: Instance?,
    val arguments: Array<RValue>,
    val localVariables: MutableMap<Int, RValue>,
    val stackTopWhenTheCallFrameWasCreated: Int,
) {
    var self = initialSelf
    var other = initialOther
    var returnValue: RValue? = null
    val environmentFrameStack = ArrayDeque<EnvironmentFrame>()
}