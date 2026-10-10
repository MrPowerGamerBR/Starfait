package com.mrpowergamerbr.starfait.runner

import com.mrpowergamerbr.starfait.GMLColor
import com.mrpowergamerbr.starfait.runner.resources.RunnerResources

class DrawState(val resources: RunnerResources) {
    var alpha = 1.0f
    var color = GMLColor(0xFFFFFF)
    var font = resources.fonts.firstOrNull()
    var circlePrecision = 24
}