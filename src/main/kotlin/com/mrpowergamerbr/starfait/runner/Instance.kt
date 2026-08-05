package com.mrpowergamerbr.starfait.runner

class Instance(var id: Int, var objectIndex: Int) : ObjectBase() {
    var x = 0.0f
    var y = 0.0f
    var imageSpeed = 1.0f
    var imageXScale = 1.0f
    var imageYScale = 1.0f
    var imageAlpha = 1.0f
    var alarms = Array(StarfaitRunner.MAX_ALARMS) { -1 }
    var visible = true
}