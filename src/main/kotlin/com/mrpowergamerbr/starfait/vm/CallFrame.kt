package com.mrpowergamerbr.starfait.vm

import com.mrpowergamerbr.starfait.runner.Instance
import com.mrpowergamerbr.starfait.wad.Code
import com.mrpowergamerbr.starfait.wad.GameObject

class CallFrame(
    val code: Code,
    val initialSelf: Instance,
    val initialOther: Instance?,
    val eventObject: GameObject,
    val eventType: Int,
    val eventSubtype: Int,
    val arguments: Array<RValue>,
    val localVariables: MutableMap<Int, RValue>,
    val stackTopWhenTheCallFrameWasCreated: Int,
) {
    var self = initialSelf
    var other = initialOther
    var returnValue: RValue? = null
    val environmentFrameStack = ArrayDeque<EnvironmentFrame>()
}