package com.mrpowergamerbr.starfait.runner

class Instance(val runner: StarfaitRunner, var id: Int, var objectIndex: Int) : ObjectBase() {
    var x = 0.0f
    var y = 0.0f
    var imageSpeed = 1.0f
    var imageXScale = 1.0f
    var imageYScale = 1.0f
    var imageAlpha = 1.0f
    var alarms = Array(StarfaitRunner.MAX_ALARMS) { -1 }
    var visible = true

    val objectName: String
        get() = runner.gameWAD.strg.getString(runner.gameWAD.objt.objects[this.objectIndex].name)

    fun setAlarm(alarm: Int, time: Int) {
        require(alarm in 0 until StarfaitRunner.MAX_ALARMS) { "Invalid alarm index!" }

        println("Instance: [${runner.vm.currentCallFrameCodeName}] [$objectName] Alarm $alarm set to $time")

        this.alarms[alarm] = time
    }
}