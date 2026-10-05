package com.mrpowergamerbr.starfait.vm

import com.mrpowergamerbr.starfait.runner.Instance

class EnvironmentFrame(
    val savedSelf: Instance,
    val instances: List<Instance>,
    val startIp: Int
) {
    var cursor = 0
}